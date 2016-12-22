package com.layer.atlas.messagetypes.threepartimage;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.PushNotificationPayload;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * GallerySender creates a ThreePartImage from the a selected image from the user's gallety.
 * Requires `Manifest.permission.READ_EXTERNAL_STORAGE` to read photos from external storage.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class GallerySender extends AttachmentSender {
    private static final String PERMISSION_READ = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) ? Manifest.permission.READ_EXTERNAL_STORAGE : null;
    public static final int ACTIVITY_REQUEST_CODE = 10;
    public static final int PERMISSION_REQUEST_CODE = 11;

    private WeakReference<Activity> mActivity = new WeakReference<Activity>(null);

    public GallerySender(int titleResId, Integer iconResId, Activity activity) {
        this(activity.getString(titleResId), iconResId, activity);
    }

    public GallerySender(String title, Integer iconResId, Activity activity) {
        super(title, iconResId);
        mActivity = new WeakReference<Activity>(activity);
    }

    private void startGalleryIntent(Activity activity) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        activity.startActivityForResult(Intent.createChooser(intent, getContext().getString(R.string.atlas_gallery_sender_chooser)), ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) return;
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Gallery permission denied");
            return;
        }
        Activity activity = mActivity.get();
        if (activity == null) return;
        startGalleryIntent(activity);
    }

    @Override
    public boolean requestSend() {
        Activity activity = mActivity.get();
        if (activity == null) return false;

        if (!hasPermissions(activity, PERMISSION_READ)) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Requesting permissions");
            requestPermissions(activity, PERMISSION_REQUEST_CODE, PERMISSION_READ);
        }else {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending gallery image");
            startGalleryIntent(activity);
        }

        return true;
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != ACTIVITY_REQUEST_CODE) return false;
        if (resultCode != Activity.RESULT_OK) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Result: " + requestCode + ", data: " + data);
            return true;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Received gallery response");
        try {
            Identity me = getLayerClient().getAuthenticatedUser();
            String myName = me == null ? "" : Util.getDisplayName(me);
            Uri uri = data.getData();
            Message message = ThreePartImageUtils.newThreePartImageMessage(activity, getLayerClient(), uri);

            PushNotificationPayload payload = new PushNotificationPayload.Builder()
                    .text(getContext().getString(R.string.atlas_notification_image, myName))
                    .build();
            message.getOptions().defaultPushNotificationPayload(payload);
            send(message);
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        }
        return true;
    }
}
