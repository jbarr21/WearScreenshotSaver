package com.github.jbarr21.wearscreenshotsaver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Images.Media
import android.util.Log
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

class WearScreenshotSaverActivity : Activity() {

  companion object {
    private val TAG = WearScreenshotSaverActivity::class.java.simpleName
    private val WEAR_SCREENSHOTS_DIR = "WearScreenshots"
    private val FILENAME_FORMAT = "WearScreenshot_%s.png"
    private val FILENAME_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    RxPermissions(this)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .subscribe { granted ->
          if (granted!!) {
            saveImageToDevice()
            finish()
          } else {
            showMessage(getString(R.string.storage_permission_denied))
            finish()
          }
        }
  }

  private fun saveImageToDevice() {
    getImageUriFromExtras(intent)
        .flatMap(this::loadBitmapFromSource)
        .flatMap(this::saveBitmapToDevice)
        .doOnNext(this::notifyMediaScanner)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onSuccess, this::onError)
  }

  private fun getImageUriFromExtras(intent: Intent): Observable<Uri> {
    return Observable.just<Uri>(intent.getParcelableExtra(EXTRA_STREAM))
        .doOnError {
          if (it is NullPointerException) {
            finish()
          }
        }
  }

  private fun loadBitmapFromSource(uri: Uri): Observable<Bitmap> {
    return try {
      Observable.just(Media.getBitmap(contentResolver, uri))
    } catch (e: IOException) {
      Observable.error(e)
    }
  }

  private fun saveBitmapToDevice(bitmap: Bitmap): Observable<File> {
    return try {
      val file = createBitmapFile()
      val fos = FileOutputStream(file)
      val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      if (success) Observable.just(file) else Observable.error(IOException("Error saving bitmap to device"))
    } catch (e: IOException) {
      Observable.error(e)
    }
  }

  private fun createBitmapFile(): File {
    val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + WEAR_SCREENSHOTS_DIR
    val parentDir = File(filePath)
    if (!parentDir.exists()) {
      parentDir.mkdirs()
    }

    val sdf = SimpleDateFormat(FILENAME_DATE_FORMAT)
    val timestampText = sdf.format(System.currentTimeMillis())
    val fileName = String.format(FILENAME_FORMAT, timestampText)
    return File(filePath, fileName)
  }

  private fun notifyMediaScanner(file: File) {
    MediaScannerConnection.scanFile(this,
        arrayOf(file.toString()), null) { _, uri -> Log.i(TAG, "Added new image: " + uri) }
  }

  private fun onSuccess(file: File) = showMessage(getString(R.string.screenshot_saved, file.parentFile.absolutePath))

  private fun onError(throwable: Throwable) {
    Log.e(TAG, getString(R.string.screenshot_error), throwable)
    showMessage(getString(R.string.screenshot_error))
  }

  private fun showMessage(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
