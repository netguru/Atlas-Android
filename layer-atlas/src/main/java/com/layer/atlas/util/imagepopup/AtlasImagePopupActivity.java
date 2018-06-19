package com.layer.atlas.util.imagepopup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;

/**
 * AtlasImagePopupActivity implements a ful resolution image viewer Activity.
 */
public class AtlasImagePopupActivity extends AppCompatActivity {

    private static final int WRITE_PERMISSION_REQUEST = 101;

    private static LayerClient layerClient;
    private static Picasso picasso;

    private SubsamplingScaleImageView mImageView;
    private ContentLoadingProgressBar mProgressBar;
    private Toolbar toolbar;
    private Uri mMessagePartId;
    private Disposable disposable = Disposables.disposed();

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

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mImageView.setImage(ImageSource.bitmap(bitmap));
                mProgressBar.hide();
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                mProgressBar.hide();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                mProgressBar.show();
            }
        };

        mImageView.setTag(target);
        picasso.load(mMessagePartId).into(target);

        ThreePartImageCellFactory.Info info = intent.getParcelableExtra("info");
        if (info != null) {
            mImageView.setOrientation(getImageOrientation(info));
        }
    }

    private int getImageOrientation(ThreePartImageCellFactory.Info info) {
        switch (info.orientation) {
            case ThreePartImageUtils.ORIENTATION_90:
                return SubsamplingScaleImageView.ORIENTATION_270;
            case ThreePartImageUtils.ORIENTATION_180:
                return SubsamplingScaleImageView.ORIENTATION_180;
            case ThreePartImageUtils.ORIENTATION_270:
                return SubsamplingScaleImageView.ORIENTATION_90;
            default:
                return SubsamplingScaleImageView.ORIENTATION_0;
        }
    }

    public static void init(LayerClient layerClient, Picasso picasso) {
        AtlasImagePopupActivity.layerClient = layerClient;
        AtlasImagePopupActivity.picasso = picasso;
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
    protected void onDestroy() {
        picasso.cancelTag(mImageView.getTag());
        layerClient = null;
        picasso = null;
        mImageView.setTag(null);
        disposable.dispose();
        super.onDestroy();
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
        MessagePart part = (MessagePart) layerClient.get(mMessagePartId);

        if(disposable.isDisposed()) {
            disposable = Util.saveImageMessageToGallery(part)
                    .subscribe(new Consumer<Util.MediaResponse>() {
                        @Override
                        public void accept(Util.MediaResponse mediaResponse) throws Exception {
                            Toast.makeText(AtlasImagePopupActivity.this,
                                    mediaResponse.isAlreadyExist() ?
                                            R.string.atlas_media_already_saved :
                                            R.string.atlas_save_media_success,
                                    Toast.LENGTH_SHORT).show();

                            updateGallery(mediaResponse);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Toast.makeText(AtlasImagePopupActivity.this,
                                    R.string.atlas_save_media_error,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(throwable.getMessage(), throwable);
                        }
                    });
        }
    }

    private void updateGallery(Util.MediaResponse mediaResponse) {
        getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaResponse.getContentValues());
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_PERMISSION_REQUEST);
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
