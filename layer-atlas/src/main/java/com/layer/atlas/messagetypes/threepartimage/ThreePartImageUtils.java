package com.layer.atlas.messagetypes.threepartimage;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class ThreePartImageUtils {
    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_180 = 1;
    public static final int ORIENTATION_90 = 2;
    public static final int ORIENTATION_270 = 3;

    public static final String MIME_TYPE_PREVIEW = "image/jpeg+preview";
    public static final String MIME_TYPE_INFO = "application/json+imageSize";

    public static final int PART_INDEX_FULL = 0;
    public static final int PART_INDEX_PREVIEW = 1;
    public static final int PART_INDEX_INFO = 2;

    public static final int PREVIEW_COMPRESSION_QUALITY = 75;
    public static final int PREVIEW_MAX_WIDTH = 512;
    public static final int PREVIEW_MAX_HEIGHT = 512;
    public static final String MIME_TYPE_FILTER_IMAGE = "image/*";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";

    public static MessagePart getInfoPart(Message message) {
        return message.getMessageParts().get(PART_INDEX_INFO);
    }

    public static MessagePart getPreviewPart(Message message) {
        return message.getMessageParts().get(PART_INDEX_PREVIEW);
    }

    public static MessagePart getFullPart(Message message) {
        return message.getMessageParts().get(PART_INDEX_FULL);
    }

    public static Message newThreePartImageMessage(Context context, LayerClient layerClient, Uri imageUri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return newThreePartImageMessageFromUri(context, layerClient, imageUri);
        } else {
            Cursor cursor = context.getContentResolver().query(imageUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
            try {
                if (cursor == null || !cursor.moveToFirst()) return null;
                File imageFile = new File(cursor.getString(0));
                return newThreePartImageMessage(context, layerClient, imageFile);
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    /**
     * Creates a new ThreePartImage Message.  The full image is attached untouched, while the
     * preview is created from the full image by loading, resizing, and compressing.
     *
     * @param client
     * @param file   Image file
     * @return
     */
    public static Message newThreePartImageMessage(Context context, LayerClient client, File file) throws IOException {
        if (client == null) throw new IllegalArgumentException("Null LayerClient");
        if (file == null) throw new IllegalArgumentException("Null image file");
        if (!file.exists()) throw new IllegalArgumentException("No image file");
        if (!file.canRead()) throw new IllegalArgumentException("Cannot read image file");

        BitmapFactory.Options bounds = getBounds(new FileInputStream(file.getAbsolutePath()));
        ExifInterface exifData = getExifData(file);

        // Create info message part
        MessagePart info = buildInfoMessagePart(client, bounds, exifData);

        // Create Preview message part
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Creating Preview from '" + file.getAbsolutePath() + "'");
        }
        MessagePart preview = buildPreviewMessagePart(context, client, new FileInputStream(file.getAbsolutePath()), bounds, exifData);

        // Create Full message part
        MessagePart full = client.newMessagePart(MIME_TYPE_IMAGE_JPEG, new FileInputStream(file), file.length());
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v(String.format(Locale.US, "Full image bytes: %d, preview bytes: %d, info bytes: %d", full.getSize(), preview.getSize(), info.getSize()));
        }

        MessagePart[] parts = new MessagePart[3];
        parts[PART_INDEX_FULL] = full;
        parts[PART_INDEX_PREVIEW] = preview;
        parts[PART_INDEX_INFO] = info;
        return client.newMessage(parts);
    }


    private static ExifInterface getExifData(File imageFile) throws IOException {
        if (imageFile == null) throw new IllegalArgumentException("Null image file");
        if (!imageFile.exists()) throw new IllegalArgumentException("Image file does not exist");
        if (!imageFile.canRead()) throw new IllegalArgumentException("Cannot read image file");
        if (imageFile.length() <= 0) throw new IllegalArgumentException("Image file is empty");

        try {
            ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
            return exifInterface;
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
            throw e;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static ExifInterface getExifData(@NonNull InputStream inputStream) throws IOException {
        try {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            return exifInterface;
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
            throw e;
        }
    }

    private static BitmapFactory.Options getBounds(InputStream inputStream) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, bounds);

        return bounds;
    }

    private static MessagePart buildInfoMessagePart(LayerClient client, BitmapFactory.Options bounds,
                                                    ExifInterface exifData) throws IOException {
        int[] orientationData = getOrientationData(exifData);
        int orientation = orientationData[0];

        boolean isSwap = orientation == ORIENTATION_270 || orientation == ORIENTATION_90;

        String intoString = "{\"orientation\":" + orientation + ", \"width\":"
                + (!isSwap ? bounds.outWidth : bounds.outHeight) + ", \"height\":"
                + (!isSwap ? bounds.outHeight : bounds.outWidth) + "}";

        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Creating image info: " + intoString);
        }

        return client.newMessagePart(MIME_TYPE_INFO, intoString.getBytes());
    }

    private static Bitmap getPreviewBitmap (BitmapFactory.Options bounds, InputStream inputStream) {
        // Determine preview size
        int[] previewDimensions = Util.scaleDownInside(bounds.outWidth, bounds.outHeight, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT);
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Preview size: " + previewDimensions[0] + "x" + previewDimensions[1]);
        }

        // Determine sample size for preview
        int sampleSize = 1;
        int sampleWidth = bounds.outWidth;
        int sampleHeight = bounds.outHeight;
        while (sampleWidth > previewDimensions[0] && sampleHeight > previewDimensions[1]) {
            sampleWidth >>= 1;
            sampleHeight >>= 1;
            sampleSize <<= 1;
        }
        if (sampleSize != 1) sampleSize >>= 1; // Back off 1 for scale-down instead of scale-up

        BitmapFactory.Options previewOptions = new BitmapFactory.Options();
        previewOptions.inSampleSize = sampleSize;

        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Preview sampled size: " + (sampleWidth << 1) + "x" + (sampleHeight << 1));
        }

        // Create preview
        Bitmap sampledBitmap = BitmapFactory.decodeStream(inputStream, null, previewOptions);
        Bitmap previewBitmap = Bitmap.createScaledBitmap(sampledBitmap, previewDimensions[0], previewDimensions[1], true);

        sampledBitmap.recycle();

        return previewBitmap;
    }

    private static MessagePart buildPreviewMessagePart(Context context, LayerClient client, InputStream inputStream,
                                                       BitmapFactory.Options bounds, ExifInterface exifData) throws IOException {

        Bitmap previewBitmap = getPreviewBitmap(bounds, inputStream);
        File temp = new File(context.getCacheDir(), ThreePartImageUtils.class.getSimpleName() + "." + System.nanoTime() + ".jpg");
        FileOutputStream previewStream = new FileOutputStream(temp);

        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Compressing preview to '" + temp.getAbsolutePath() + "'");
        }

        previewBitmap.compress(Bitmap.CompressFormat.JPEG, PREVIEW_COMPRESSION_QUALITY, previewStream);
        previewBitmap.recycle();
        previewStream.close();

        // Preserve exif orientation
        ExifInterface preserver = new ExifInterface(temp.getAbsolutePath());
        int[] orientationData = getOrientationData(exifData);
        int exifOrientation = orientationData[1];
        preserver.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(exifOrientation));
        preserver.saveAttributes();
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Exif orientation preserved in preview");
        }

        return client.newMessagePart(MIME_TYPE_PREVIEW, new FileInputStream(temp), temp.length());
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private static Message newThreePartImageMessageFromUri(Context context, LayerClient client, @NonNull Uri uri) throws IOException {
        if (client == null) throw new IllegalArgumentException("Null LayerClient");

        //InputStream inputStream = StorageAccessUtilities.getInputStreamFromUri(context, uri, MIME_TYPE_FILTER_IMAGE);
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        ExifInterface exifData = getExifData(inputStream);

        inputStream = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options bounds = getBounds(inputStream);

        // Create info message part
        MessagePart info = buildInfoMessagePart(client, bounds, exifData);

        // Create Preview message part
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Creating Preview from " + uri.toString());
        }
        inputStream = context.getContentResolver().openInputStream(uri);
        MessagePart preview = buildPreviewMessagePart(context, client, inputStream, bounds, exifData);

        // Create Full message part
        inputStream = context.getContentResolver().openInputStream(uri);
        long fileSize = getFileSizeFromUri(context, uri);
        MessagePart full = client.newMessagePart(MIME_TYPE_IMAGE_JPEG, inputStream, fileSize);
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v(String.format(Locale.US, "Full image bytes: %d, preview bytes: %d, info bytes: %d", full.getSize(), preview.getSize(), info.getSize()));
        }

        MessagePart[] parts = new MessagePart[3];
        parts[PART_INDEX_FULL] = full;
        parts[PART_INDEX_PREVIEW] = preview;
        parts[PART_INDEX_INFO] = info;
        return client.newMessage(parts);
    }

    private static int[] getOrientationData(ExifInterface exifInterface) {
        // Try parsing Exif data.
        int orientation = ORIENTATION_0;
        int exifOrientation = ExifInterface.ORIENTATION_UNDEFINED;

        exifOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Found Exif orientation: " + exifOrientation);
        }
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_UNDEFINED:
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                orientation = ORIENTATION_0;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                orientation = ORIENTATION_180;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = ORIENTATION_270;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = ORIENTATION_90;
                break;
        }
        int[] orientationData = new int[]{orientation, exifOrientation};
        return orientationData;
    }

    private static long getFileSizeFromUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri,
                null, null, null, null);
        cursor.moveToFirst();
        long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
        cursor.close();

        return size;
    }
}
