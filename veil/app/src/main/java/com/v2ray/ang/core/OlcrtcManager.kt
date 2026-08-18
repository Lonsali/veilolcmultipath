package com.v2ray.ang.core

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import mobile.Mobile
import mobile.Runtime
import mobile.SocketProtector
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object OlcrtcManager {

    private const val WAIT_READY_MS = 15000L
    private const val STOP_TIMEOUT_MS = 5000L
    private const val DEFAULT_VP8_FPS = 30L
    private const val DEFAULT_VP8_BATCH_SIZE = 64L

    @Volatile
    var socketProtector: ((Int) -> Boolean)? = null

    /**
     * One runtime per started profile, keyed by profile guid.
     *
     * A policy group of olcRTC profiles needs every member live at the same
     * time: each is a separate session at the provider, and the provider caps
     * throughput per session, so the balancer only gains anything when several
     * sessions are actually running. [mobile.Runtime] owns one independent
     * client lifecycle, so one instance per profile is the supported way to do
     * this.
     */
    private val runtimes = ConcurrentHashMap<String, Runtime>()

    /**
     * Ports handed out during the current start pass. [findAvailablePort] probes
     * by binding, which cannot see a port that an already-started runtime is
     * about to take, so remember them here to avoid handing the same port to
     * two profiles.
     */
    private val reservedPorts = ConcurrentHashMap.newKeySet<Int>()

    /** Kept apart from tunnel runtimes so probing never disturbs a live session. */
    private val probeRuntime: Runtime by lazy { Mobile.new_() }

    private val protector = object : SocketProtector {
        override fun protect(fd: Long): Boolean {
            return socketProtector?.invoke(fd.toInt()) ?: true
        }
    }

    val isRunning: Boolean
        get() = runtimes.values.any { rt ->
            try {
                rt.isRunning
            } catch (e: Exception) {
                false
            }
        }

    /** Number of olcRTC sessions currently live. */
    val runningCount: Int
        get() = runtimes.values.count { rt ->
            try {
                rt.isRunning
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Start one olcRTC session for [config] and remember it under [guid].
     *
     * The allocated SOCKS port is written back into [config]; the caller is
     * responsible for persisting it, because the generated Xray outbound points
     * at that port.
     */
    fun start(context: Context, config: ProfileItem, guid: String): Boolean {
        val existing = runtimes[guid]
        if (existing != null && runtimeIsRunning(existing)) {
            LogUtil.i(AppConfig.TAG, "OlcrtcManager: $guid already running, returning success")
            return true
        }

        val carrier = config.olcrtcCarrier?.takeIf { it.isNotBlank() } ?: "jitsi"
        val transport = config.olcrtcTransport?.takeIf { it.isNotBlank() } ?: "datachannel"
        val roomId = normalizeRoomURL(carrier, config.olcrtcRoomId, config.olcrtcServerUrl)
        val hadClientId = config.olcrtcClientId?.isNotBlank() == true
        val clientId = config.olcrtcClientId?.takeIf { it.isNotBlank() } ?: persistentDeviceId()
        val keyHex = config.olcrtcKeyHex ?: ""
        val socksHost = AppConfig.LOOPBACK
        val (fps, batchSize) = parseEngine(config.olcrtcEngine)

        if (roomId.isEmpty()) {
            LogUtil.e(AppConfig.TAG, "OlcrtcManager: roomId is empty")
            return false
        }
        if (keyHex.isEmpty()) {
            LogUtil.e(AppConfig.TAG, "OlcrtcManager: keyHex is empty")
            return false
        }

        // Allocated only once the profile is known to be usable, so a rejected
        // profile does not burn a port number for the rest of the start pass.
        val preferredPort = (config.serverPort ?: AppConfig.PORT_OLCRTC_SOCKS).toIntOrNull() ?: AppConfig.PORT_OLCRTC_SOCKS.toInt()
        val socksPort = findAvailablePort(preferredPort)

        if (!hadClientId) {
            config.olcrtcClientId = clientId
        }
        config.serverPort = socksPort.toString()

        val runtime = runtimes.getOrPut(guid) { Mobile.new_() }
        try {
            runtime.setProtector(protector)
            runtime.setProvider(carrier)
            runtime.setTransport(transport)
            runtime.setRoom(roomId)
            runtime.setKey(keyHex)
            runtime.setDNS(resolveOlcrtcDns(context))
            runtime.setSocksListenHost(socksHost)
            runtime.setSocksPort(socksPort.toLong())
            runtime.setDeviceID(clientId)
            if (fps > 0 || batchSize > 0) {
                runtime.setVP8Options(
                    (if (fps > 0) fps.toLong() else DEFAULT_VP8_FPS),
                    (if (batchSize > 0) batchSize.toLong() else DEFAULT_VP8_BATCH_SIZE)
                )
            }

            LogUtil.d(AppConfig.TAG, "OlcrtcManager: start guid=$guid carrier=$carrier transport=$transport room=$roomId client=$clientId key=${keyHex.take(8)}... socks=${socksHost}:${socksPort}")
            runtime.start()

            LogUtil.d(AppConfig.TAG, "OlcrtcManager: waitReady (15s) guid=$guid")
            runtime.waitReady(WAIT_READY_MS)
            LogUtil.i(AppConfig.TAG, "OlcrtcManager: started $guid on ${socksHost}:${socksPort}")
            return true
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "OlcrtcManager: start failed for $guid: ${e.javaClass.simpleName}: ${e.message}", e)
            runtimes.remove(guid)
            releasePort(socksPort)
            return false
        }
    }

    /** Stops every live session and forgets the reserved ports. */
    fun stop() {
        runtimes.forEach { (guid, runtime) ->
            try {
                runtime.stop(STOP_TIMEOUT_MS)
                LogUtil.i(AppConfig.TAG, "OlcrtcManager: stopped $guid")
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "OlcrtcManager: stop failed for $guid", e)
            }
        }
        runtimes.clear()
        reservedPorts.clear()
    }

    private fun runtimeIsRunning(runtime: Runtime): Boolean = try {
        runtime.isRunning
    } catch (e: Exception) {
        false
    }

    private fun releasePort(port: Int) {
        reservedPorts.remove(port)
    }

    private fun resolveOlcrtcDns(context: Context): String {
        // 1. User-defined olcRTC DNS preference
        MmkvManager.decodeSettingsString(AppConfig.PREF_OLCRTC_DNS)?.trim()?.let { dns ->
            if (dns.isNotBlank()) {
                ipWithPort(dns)?.let { return it }
            }
        }

        // 2. Provider/system DNS
        Utils.getProviderDns(context)?.let { return it }

        // 3. Domestic DNS
        SettingsManager.getDomesticDnsServers().firstNotNullOfOrNull { ipWithPort(it) }?.let { return it }

        // 4. Remote DNS
        SettingsManager.getRemoteDnsServers().firstNotNullOfOrNull { ipWithPort(it) }?.let { return it }

        // 5. Fallback
        return AppConfig.DNS_OLCRTC_FALLBACK
    }

    private fun ipWithPort(dns: String): String? {
        val trimmed = dns.trim()
        return when {
            trimmed.contains(":") -> {
                val ip = trimmed.substringBefore(":")
                val port = trimmed.substringAfter(":", "53").toIntOrNull() ?: 53
                if (Utils.isPureIpAddress(ip)) "$ip:$port" else null
            }
            Utils.isPureIpAddress(trimmed) -> "$trimmed:53"
            else -> null
        }
    }

    fun ping(config: ProfileItem, timeoutSec: Int = 5): Boolean {
        val carrier = config.olcrtcCarrier?.takeIf { it.isNotBlank() } ?: "jitsi"
        val transport = config.olcrtcTransport?.takeIf { it.isNotBlank() } ?: "datachannel"
        val roomId = normalizeRoomURL(carrier, config.olcrtcRoomId, config.olcrtcServerUrl)
        val clientId = config.olcrtcClientId?.takeIf { it.isNotBlank() } ?: persistentDeviceId()
        val keyHex = config.olcrtcKeyHex ?: ""
        val socksPort = (config.serverPort ?: AppConfig.PORT_OLCRTC_SOCKS).toLongOrNull() ?: AppConfig.PORT_OLCRTC_SOCKS.toLong()

        return try {
            val result = probeRuntime.ping(
                carrier, transport, roomId, clientId, keyHex,
                socksPort, timeoutSec.toLong() * 1000, "", 0L, 0L
            )
            result > 0
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "OlcrtcManager: ping failed: ${e.message}", e)
            false
        }
    }

    fun check(config: ProfileItem): String {
        val carrier = config.olcrtcCarrier?.takeIf { it.isNotBlank() } ?: "jitsi"
        val transport = config.olcrtcTransport?.takeIf { it.isNotBlank() } ?: "datachannel"
        val roomId = normalizeRoomURL(carrier, config.olcrtcRoomId, config.olcrtcServerUrl)
        val clientId = config.olcrtcClientId?.takeIf { it.isNotBlank() } ?: persistentDeviceId()
        val keyHex = config.olcrtcKeyHex ?: ""
        val socksPort = (config.serverPort ?: AppConfig.PORT_OLCRTC_SOCKS).toLongOrNull() ?: AppConfig.PORT_OLCRTC_SOCKS.toLong()

        return try {
            val result = probeRuntime.check(
                carrier, transport, roomId, clientId, keyHex,
                socksPort, WAIT_READY_MS, 0L, 0L
            )
            if (result >= 0) "ok" else "error: $result"
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "OlcrtcManager: check failed: ${e.message}", e)
            e.message ?: "check failed"
        }
    }

    private fun persistentDeviceId(): String {
        val key = "olcrtc_device_id"
        val stored = MmkvManager.decodeSettingsString(key)
        if (!stored.isNullOrBlank()) return stored
        val generated = generateInstallId()
        MmkvManager.encodeSettings(key, generated)
        return generated
    }

    private fun generateInstallId(): String {
        return "install-" + Random.nextBytes(16).joinToString("") { b ->
            (b.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun findAvailablePort(preferred: Int): Int {
        for (port in preferred..preferred + 100) {
            // A port handed to an earlier profile in this pass may not be bound
            // yet, so probing alone would offer it twice.
            if (!reservedPorts.add(port)) continue
            try {
                ServerSocket(port).use {
                    return port
                }
            } catch (_: Exception) {
                reservedPorts.remove(port)
            }
        }
        LogUtil.e(AppConfig.TAG, "OlcrtcManager: no available port in range $preferred..${preferred + 100}")
        return preferred
    }

    private fun normalizeRoomURL(carrier: String, roomId: String?, serverUrl: String?): String {
        val room = roomId ?: ""
        if (room.isEmpty()) return ""
        if (room.contains("://") || room.contains("/")) return room
        val server = serverUrl?.takeIf { it.isNotBlank() } ?: when (carrier) {
            "jitsi" -> "meet.egovm.ru"
            "telemost" -> "telemost.yandex.ru/j"
            else -> return room
        }
        return "https://$server/$room"
    }

    /**
     * Returns the provider domain derived from the OLCRTC room identifier.
     *
     * @param carrier The transport carrier.
     * @param roomId The room identifier or full room URL.
     * @param serverUrl Optional custom server URL.
     * @return The provider domain, or an empty string if unavailable.
     */
    fun providerUrl(carrier: String, roomId: String?, serverUrl: String?): String {
        val fullUrl = normalizeRoomURL(carrier, roomId, serverUrl)
        if (fullUrl.isEmpty()) return ""
        return try {
            val url = java.net.URL(fullUrl)
            url.authority
        } catch (_: Exception) {
            fullUrl
        }
    }

    private fun parseEngine(engine: String?): Pair<Int, Int> {
        if (engine.isNullOrBlank()) return 0 to 0
        var fps = 0
        var batchSize = 0
        engine.split("&").forEach { pair ->
            val kv = pair.split("=", limit = 2)
            when (kv.getOrNull(0)?.trim()) {
                "fps" -> fps = kv.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                "batchSize" -> batchSize = kv.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            }
        }
        return fps to batchSize
    }
}
