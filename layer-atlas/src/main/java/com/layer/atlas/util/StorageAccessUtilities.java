package com.layer.atlas.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by archit on 12/20/16.
 */

public class StorageAccessUtilities {

    /**
     * Android 7.0 adds the concept of virtual files to the Storage Access Framework. Even though
     * virtual files do not have a binary representation, your client app can open their contents
     * by coercing them into a different file type
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean isVirtualFile(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!DocumentsContract.isDocumentUri(context, uri)) {
                return false;
            }

            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{DocumentsContract.Document.COLUMN_FLAGS}, null, null, null);

            int flags = 0;
            if (cursor.moveToFirst()) {
                flags = cursor.getInt(0);
            }
            cursor.close();
            return (flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
        }

        return false;
    }

    /**
     * You can coerce a virtual file into an alternative MIME type such as an image file.
     * The following code checks whether a virtual file can be represented as the type specified in
     * the filter, and if so, gets an input stream.
     *
     * @param mimeTypeFilter
     * @return
     * @throws IOException
     */
    public static InputStream getInputStreamFromUri(Context context, Uri uri, String mimeTypeFilter) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String[] openableMimeTypes = resolver.getStreamTypes(uri, mimeTypeFilter);

        if (openableMimeTypes == null || openableMimeTypes.length < 1) {
            throw new FileNotFoundException();
        }

        return resolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                .createInputStream();
    }

    public static long getFileSizeForVirtualFile(Context context, Uri uri, String mimeTypeFilter) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String[] openableMimeTypes = resolver.getStreamTypes(uri, mimeTypeFilter);

        if (openableMimeTypes == null || openableMimeTypes.length < 1) {
            throw new FileNotFoundException();
        }

        return resolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                .getLength();
    }

}
