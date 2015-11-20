package com.github.jbarr21.wearscreenshotsaver

import android.Manifest;
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Images.Media
import android.util.Log
import android.widget.Toast

import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

import rx.Observable
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class WearScreenshotSaverActivity : Activity() {

    private val TAG = "WearScreenshotSaverActivity"; // TODO: figure out how to name based on class file name
    private val WEAR_SCREENSHOTS_DIR = "WearScreenshots"
    private val FILENAME_PREFIX = "WearScreenshot"
    private val FILENAME_EXT = "png"
    private val FILENAME_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ granted -> onPermissionGranted(granted) }, { e -> onError(e) })
    }

    private fun onPermissionGranted(granted: Boolean) {
        if (granted) {
            saveImageToDevice()
            finish()
        } else {
            showMessage(getString(R.string.storage_permission_denied))
            finish()
        }
    }

    private fun saveImageToDevice() {
        showMessage("saving...")
        getImageUriFromExtras(getIntent())
                .flatMap({ uri -> loadBitmapFromSource(uri) })
                .flatMap({ bitmap -> saveBitmapToDevice(bitmap) })
                .doOnNext({ file -> notifyMediaScanner(file) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ file -> onSuccess(file) }, { e -> onError(e) })
    }

    private fun getImageUriFromExtras(intent: Intent): Observable<Uri> {
        return Observable.just(intent.getParcelableExtra(Intent.EXTRA_STREAM));
    }

    private fun loadBitmapFromSource(uri: Uri): Observable<Bitmap> {
        try {
            return Observable.just(Media.getBitmap(getContentResolver(), uri));
        } catch (e: IOException) {
            return Observable.error(e);
        }
    }

    private fun saveBitmapToDevice(bitmap: Bitmap): Observable<File> {
        try {
            val file = createBitmapFile()
            val fos = FileOutputStream(file)
            val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            if (success) {
                return Observable.just(file)
            } else {
                val exception = IOException("Error saving bitmap to device")
                return Observable.error(exception)
            }
        } catch (e: IOException) {
            return Observable.error(e);
        }
    }

    private fun createBitmapFile(): File {
        val filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}${File.separator}${WEAR_SCREENSHOTS_DIR}"
        val parentDir = File(filePath)
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }

        val sdf = SimpleDateFormat(FILENAME_DATE_FORMAT)
        val timestampText = sdf.format(System.currentTimeMillis())
        val fileName = "${FILENAME_PREFIX}_${timestampText}.${FILENAME_EXT}"
        return File(filePath, fileName)
    }

    private fun notifyMediaScanner(file: File) {
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null,
                { path, uri -> Log.i(TAG, "Added new image: ${uri}") })
    }

    private fun onSuccess(file: File) {
        showMessage(getString(R.string.screenshot_saved, file.getParentFile().getAbsolutePath()))
    }

    private fun onError(throwable: Throwable) {
        Log.e(TAG, getString(R.string.screenshot_error), throwable)
        showMessage(getString(R.string.screenshot_error))
    }

    private fun showMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
