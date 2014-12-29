package com.github.jbarr21.wearscreenshotsaver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class WearScreenshotSaverActivity extends Activity {

    private static final String TAG = WearScreenshotSaverActivity.class.getSimpleName();
    private static final String WEAR_SCREENSHOTS_DIR = "WearScreenshots";
    private static final String FILENAME_FORMAT = "WearScreenshot_%s.png";
    private static final String FILENAME_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveImageToDevice();
        finish();
    }

    private void saveImageToDevice() {
        getImageUriFromExtras(getIntent())
                .flatMap(this::loadBitmapFromSource)
                .flatMap(this::saveBitmapToDevice)
                .doOnNext(this::notifyMediaScanner)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccess, this::onError);
    }

    private Observable<Uri> getImageUriFromExtras(Intent intent) {
        return Observable.just(intent.getParcelableExtra(Intent.EXTRA_STREAM));
    }

    private Observable<Bitmap> loadBitmapFromSource(Uri uri) {
        try {
            return Observable.just(Media.getBitmap(getContentResolver(), uri));
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    private Observable<File> saveBitmapToDevice(Bitmap bitmap) {
        try {
            File file = createBitmapFile();
            FileOutputStream fos = new FileOutputStream(file);
            boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return success ? Observable.just(file) : Observable.error(new IOException("Error saving bitmap to device"));
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    private File createBitmapFile() {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + WEAR_SCREENSHOTS_DIR;
        File parentDir = new File(filePath);
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_DATE_FORMAT);
        String timestampText = sdf.format(System.currentTimeMillis());
        String fileName = String.format(FILENAME_FORMAT, timestampText);
        return new File(filePath, fileName);
    }

    private void notifyMediaScanner(@NonNull File file) {
        MediaScannerConnection.scanFile(this,
                new String[]{file.toString()}, null, (path, uri) -> {
                    Log.i(TAG, "Added new image: " + uri);
                });
    }

    private void onSuccess(@NonNull File file) {
        Toast.makeText(this, getString(R.string.screenshot_saved, file.getParentFile().getAbsolutePath()), Toast.LENGTH_LONG).show();
    }

    private void onError(Throwable throwable) {
        Log.e(TAG, getString(R.string.screenshot_error), throwable);
        Toast.makeText(this, R.string.screenshot_error, Toast.LENGTH_SHORT).show();
    }
}
