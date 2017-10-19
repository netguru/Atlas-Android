package com.layer.atlas.messagetypes.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.RequiresPermission;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.messaging.PushNotificationPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * LocationSender creates JSON MessagePart with latitude, longitude, and label.  Google's fused
 * location API is used for gathering location at send time and may trigger a dialog for updating
 * Google Play Services.  Requires `Manifest.permission.ACCESS_FINE_LOCATION` for getting device
 * location.
 */
public class LocationSender extends AttachmentSender {
    private static final String PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int ACTIVITY_REQUEST_CODE = 30;
    public static final int PERMISSION_REQUEST_CODE = 31;

    private static GoogleApiClient sGoogleApiClient;

    private WeakReference<Activity> mActivity = new WeakReference<Activity>(null);

    public LocationSender(int titleResId, Integer iconResId, Activity activity) {
        this(activity.getString(titleResId), iconResId, activity);
    }

    public LocationSender(String title, Integer iconResId, Activity activity) {
        super(title, iconResId);
        mActivity = new WeakReference<Activity>(activity);
        init(activity);
    }

    private void init(final Activity activity) {
        // If the client has already been created, ensure connected and return.
        if (sGoogleApiClient != null) {
            if (!sGoogleApiClient.isConnected()) sGoogleApiClient.connect();
            return;
        }

        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);

        // If the correct Google Play Services are available, connect and return.
        if (errorCode == ConnectionResult.SUCCESS) {
            GoogleApiCallbacks googleApiCallbacks = new GoogleApiCallbacks();
            sGoogleApiClient = new GoogleApiClient.Builder(activity.getApplicationContext())
                    .addConnectionCallbacks(googleApiCallbacks)
                    .addOnConnectionFailedListener(googleApiCallbacks)
                    .addApi(LocationServices.API)
                    .build();
            sGoogleApiClient.connect();
            return;
        }

        // If the correct Google Play Services are not available, redirect to proper solution.
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(activity, errorCode, ACTIVITY_REQUEST_CODE)
                    .show();
            return;
        }

        if (Log.isLoggable(Log.ERROR)) Log.e("Cannot update Google Play Services: " + errorCode);
    }

    @RequiresPermission(PERMISSION)
    private static boolean getFreshLocation(LocationListener listener) {
        if (sGoogleApiClient == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("GoogleApiClient not initialized");
            return false;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Getting fresh location");
        LocationRequest r = new LocationRequest()
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(10000)
                .setMaxWaitTime(10000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(sGoogleApiClient, r, listener);
            return true;
        } catch (IllegalStateException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
        return false;
    }

    @RequiresPermission(PERMISSION)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) return;
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Location permission denied");
            return;
        }
        getFreshLocation(new SenderLocationListener(this));
    }

    /**
     * Asynchronously requests a fresh location and sends a location Message.
     */
    @RequiresPermission(PERMISSION)
    @Override
    public boolean requestSend() {
        Activity activity = mActivity.get();
        if (activity == null) return false;
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending location");
        if (checkSelfPermission(activity, PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(activity, PERMISSION_REQUEST_CODE, PERMISSION);
            return true;
        }
        return getFreshLocation(new SenderLocationListener(this));
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != ACTIVITY_REQUEST_CODE) return false;
        init(activity);
        return true;
    }

    private static class SenderLocationListener implements LocationListener {
        private final WeakReference<LocationSender> mLocationSenderReference;

        public SenderLocationListener(LocationSender locationsender) {
            mLocationSenderReference = new WeakReference<LocationSender>(locationsender);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Got fresh location");

            if (Log.isPerfLoggable()) {
                Log.perf("LocationSender is attempting to send a message");
            }
            LocationSender sender = mLocationSenderReference.get();
            if (sender == null) return;
            Context context = sender.getContext();
            LayerClient client = sender.getLayerClient();
            try {
                Identity me = client.getAuthenticatedUser();
                String myName = me == null ? "" : Util.getDisplayName(me);
                JSONObject o = new JSONObject()
                        .put(LocationCellFactory.KEY_LATITUDE, location.getLatitude())
                        .put(LocationCellFactory.KEY_LONGITUDE, location.getLongitude())
                        .put(LocationCellFactory.KEY_LABEL, myName);
                String notification = context.getString(R.string.atlas_notification_location, myName);
                MessagePart part = client.newMessagePart(LocationCellFactory.MIME_TYPE, o.toString().getBytes());
                PushNotificationPayload payload = new PushNotificationPayload.Builder()
                        .text(notification)
                        .build();
                Message message = client.newMessage(new MessageOptions().defaultPushNotificationPayload(payload), part);
                sender.send(message);
            } catch (JSONException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
            }
        }
    }

    private static class GoogleApiCallbacks implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient connected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient failed: " + connectionResult);
        }
    }
}