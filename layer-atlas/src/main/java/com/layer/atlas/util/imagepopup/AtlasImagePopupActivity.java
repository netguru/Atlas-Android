package com.layer.atlas.util.imagepopup;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
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

import java.lang.ref.WeakReference;

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
    private ThreePartImageCellFactory.Info info;
    private Disposable disposable = Disposables.disposed();
    private WeakReference<AlertDialog> dialogWeakReference;

    private boolean isImageLoaded = false;

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
        info = intent.getParcelableExtra("info");

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                AtlasImagePopupActivity.this.onBitmapLoaded(bitmap);
            }

            @Override
            public void onBitmapFailed(Exception ex, Drawable errorDrawable) {
                mProgressBar.hide();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                mProgressBar.show();
            }
        };

        mImageView.setTag(target);
        if (picasso != null)
            picasso.load(mMessagePartId).into(target);

        if (info != null) {
            mImageView.setOrientation(getImageOrientation(info));
        }
    }

    private void onBitmapLoaded(Bitmap bitmap) {
        isImageLoaded = true;
        mImageView.setImage(ImageSource.bitmap(bitmap));
        invalidateOptionsMenu();
        mProgressBar.hide();
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
        menu.findItem(R.id.action_save).setVisible(isImageLoaded);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            if (checkPermission()) {
                showImageSizeDialog(AtlasImagePopupActivity.this);
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
        mImageView.setTag(null);
        disposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showImageSizeDialog(AtlasImagePopupActivity.this);
        }
    }

    private void showImageSizeDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        showImageSizeDialog(context, builder);
    }

    private void showImageSizeDialog(final Context context, AlertDialog.Builder builder) {
        if (dialogWeakReference != null && dialogWeakReference.get() != null) {
            return;
        }
        builder.setTitle(R.string.atlas_three_part_image_size_dialog_title)
                .setMessage(context.getResources().getString(
                        R.string.atlas_three_part_image_size_dialog_message,
                        Formatter.formatShortFileSize(context,
                                info.fullPartSizeInBytes)))
                .setCancelable(true)
                .setPositiveButton(R.string.atlas_three_part_image_size_dialog_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                saveImageToGallery();
                            }
                        })
                .setNegativeButton(R.string.atlas_three_part_image_size_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Empty
                            }
                        })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        dialogWeakReference.clear();
                    }
                });

        dialogWeakReference = new WeakReference<>(builder.show());
    }

    private void saveImageToGallery() {
        MessagePart part = (MessagePart) layerClient.get(mMessagePartId);

        if (disposable.isDisposed()) {
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
