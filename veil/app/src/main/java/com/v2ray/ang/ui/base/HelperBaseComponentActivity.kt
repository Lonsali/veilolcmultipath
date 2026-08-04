package com.v2ray.ang.ui.base

import android.net.Uri
import android.os.Bundle
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.helper.FileChooserHelper
import com.v2ray.ang.helper.PermissionHelper
import com.v2ray.ang.helper.QRCodeScannerHelper

/**
 * Compose-capable helper base activity that adds file chooser, permission
 * request, and QR code scanner helpers on top of [BaseComponentActivity].
 */
abstract class HelperBaseComponentActivity : BaseComponentActivity() {

    private lateinit var fileChooser: FileChooserHelper
    private lateinit var permissionRequester: PermissionHelper
    private lateinit var qrCodeScanner: QRCodeScannerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileChooser = FileChooserHelper(this)
        permissionRequester = PermissionHelper(this)
        qrCodeScanner = QRCodeScannerHelper(this)
    }

    /**
     * Check if permission is granted and request it if not.
     *
     * @param permissionType The type of permission to check and request
     * @param onGranted Callback to execute when permission is granted
     */
    protected fun checkAndRequestPermission(
        permissionType: PermissionType,
        onGranted: () -> Unit
    ) {
        permissionRequester.request(permissionType, onGranted)
    }

    /**
     * Launch file chooser with ACTION_GET_CONTENT intent.
     *
     * @param mimeType MIME type filter for files
     * @param onResult Callback invoked with the selected file URI (null if cancelled)
     */
    protected fun launchFileChooser(
        mimeType: String = "*/*",
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.launch(mimeType, onResult)
    }

    /**
     * Launch document creator to create a new file at user-selected location.
     *
     * @param fileName Default file name for the new document
     * @param onResult Callback invoked with the created file URI (null if cancelled)
     */
    protected fun launchCreateDocument(
        fileName: String,
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.createDocument(fileName, onResult)
    }

    /**
     * Launch QR code scanner with camera permission check.
     *
     * @param onResult Callback invoked with the scan result string (null if cancelled or failed)
     */
    protected fun launchQRCodeScanner(onResult: (String?) -> Unit) {
        qrCodeScanner.launch(onResult)
    }
}
