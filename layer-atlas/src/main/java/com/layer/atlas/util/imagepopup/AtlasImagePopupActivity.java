package com.layer.atlas.util.imagepopup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageUtils;
import com.layer.atlas.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;

import java.util.List;
import java.util.UUID;

/**
 * AtlasImagePopupActivity implements a ful resolution image viewer Activity.  This Activity
 * registers with the LayerClient as a LayerProgressListener to monitor progress.
 */
public class AtlasImagePopupActivity extends AppCompatActivity implements LayerProgressListener.BackgroundThread.Weak, SubsamplingScaleImageView.OnImageEventListener {

    private static final int WRITE_PERMISSION_REQUEST = 101;

    private static LayerClient sLayerClient;

    private SubsamplingScaleImageView mImageView;
    private ContentLoadingProgressBar mProgressBar;
    private Toolbar toolbar;
    private Uri mMessagePartId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(R.color.atlas_image_popup_background);
        setContentView(R.layout.atlas_image_popup);
        mImageView = (SubsamplingScaleImageView) findViewById(R.id.image_popup);
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.image_popup_progress);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(false);
        }

        mImageView.setPanEnabled(true);
        mImageView.setZoomEnabled(true);
        mImageView.setDoubleTapZoomDpi(160);
        mImageView.setMinimumDpi(80);
        mImageView.setBitmapDecoderClass(MessagePartDecoder.class);
        mImageView.setRegionDecoderClass(MessagePartRegionDecoder.class);

        Intent intent = getIntent();
        if (intent == null) return;
        mMessagePartId = intent.getParcelableExtra("fullId");
        Uri previewId = intent.getParcelableExtra("previewId");
        ThreePartImageCellFactory.Info info = intent.getParcelableExtra("info");

        mProgressBar.show();
        if (previewId != null && info != null) {
            // ThreePartImage
            switch (info.orientation) {
                case ThreePartImageUtils.ORIENTATION_0:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_0);
                    mImageView.setImage(
                            ImageSource.uri(mMessagePartId).dimensions(info.width, info.height),
                            ImageSource.uri(previewId));
                    break;
                case ThreePartImageUtils.ORIENTATION_90:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_270);
                    mImageView.setImage(
                            ImageSource.uri(mMessagePartId).dimensions(info.height, info.width),
                            ImageSource.uri(previewId));
                    break;
                case ThreePartImageUtils.ORIENTATION_180:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_180);
                    mImageView.setImage(
                            ImageSource.uri(mMessagePartId).dimensions(info.width, info.height),
                            ImageSource.uri(previewId));
                    break;
                case ThreePartImageUtils.ORIENTATION_270:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
                    mImageView.setImage(
                            ImageSource.uri(mMessagePartId).dimensions(info.height, info.width),
                            ImageSource.uri(previewId));
                    break;
            }
        } else {
            // SinglePartImage
            mImageView.setImage(ImageSource.uri(mMessagePartId));
        }
        mImageView.setOnImageEventListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sLayerClient.registerProgressListener(null, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sLayerClient.unregisterProgressListener(null, this);
    }

    public static void init(LayerClient layerClient) {
        sLayerClient = layerClient;
        MessagePartDecoder.init(layerClient);
        MessagePartRegionDecoder.init(layerClient);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.atlas_image_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       if(item.getItemId() == R.id.action_save) {
           if(checkPermission()) {
               saveImageToGallery();
           } else {
               requestPermission();
           }
           return true;
       }
       return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    saveImageToGallery();
            }
            return;
        }
    }

    private void saveImageToGallery() {
        String extractedID = UUID.randomUUID().toString();
        List<String> path = mMessagePartId.getPathSegments();
        if(path !=null && path.size()>2)
            extractedID = path.get(path.size()-1);
        MessagePartDecoder decoderFactory = new MessagePartDecoder();
        Bitmap bitmap = null;
        try {
            bitmap = decoderFactory.decode(this, mMessagePartId);
        } catch (Throwable ex){
            Log.e(ex.getMessage(), ex);
        }
        if(insertImage(bitmap, extractedID))
            Toast.makeText(this, R.string.atlas_save_media_success, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.atlas_save_media_error, Toast.LENGTH_SHORT).show();
    }

    private boolean insertImage(Bitmap bitmap, String title){
        return bitmap != null && MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                title,
                ""
        ) != null;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_PERMISSION_REQUEST);
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    //==============================================================================================
    // SubsamplingScaleImageView.OnImageEventListener: hide progress bar when full part loaded
    //==============================================================================================

    @Override
    public void onReady() {

    }

    @Override
    public void onImageLoaded() {
        mProgressBar.hide();
    }

    @Override
    public void onPreviewLoadError(Exception e) {
        if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        mProgressBar.hide();
    }

    @Override
    public void onImageLoadError(Exception e) {
        if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        mProgressBar.hide();
    }

    @Override
    public void onTileLoadError(Exception e) {
        if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        mProgressBar.hide();
    }


    //==============================================================================================
    // LayerProgressListener: update progress bar while downloading
    //==============================================================================================

    @Override
    public void onProgressStart(MessagePart messagePart, Operation operation) {
        if (!messagePart.getId().equals(mMessagePartId)) return;
        mProgressBar.setProgress(0);
    }

    @Override
    public void onProgressUpdate(MessagePart messagePart, Operation operation, long bytes) {
        if (!messagePart.getId().equals(mMessagePartId)) return;
        double fraction = (double) bytes / (double) messagePart.getSize();
        int progress = (int) Math.round(fraction * mProgressBar.getMax());
        mProgressBar.setProgress(progress);
    }

    @Override
    public void onProgressComplete(MessagePart messagePart, Operation operation) {
        if (!messagePart.getId().equals(mMessagePartId)) return;
        mProgressBar.setProgress(mProgressBar.getMax());
    }

    @Override
    public void onProgressError(MessagePart messagePart, Operation operation, Throwable e) {
        if (!messagePart.getId().equals(mMessagePartId)) return;
        if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
    }

}
