/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.layer.atlas.BuildConfig;
import com.layer.atlas.R;
import com.layer.atlas.support.Participant;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Queryable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Util {
    private static final int TIME_HOURS_24 = 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat DAY_OF_WEEK = new SimpleDateFormat("EEE, LLL dd,", Locale.getDefault());

    /**
     * Returns the app version name.
     *
     * @return The app version name.
     */
    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static void copyToClipboard(Context context, int stringResId, String content) {
        copyToClipboard(context, context.getString(stringResId), content);
    }

    public static void copyToClipboard(Context context, String description, String content) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = new ClipData(description, new String[]{"text/plain"}, new ClipData.Item(content));
        manager.setPrimaryClip(clipData);
    }


    public static String getInitials(Identity user) {
        String first = user.getFirstName();
        String last = user.getLastName();
        if (!TextUtils.isEmpty(first)) {
            if (!TextUtils.isEmpty(last)) {
                return getInitials(first) + getInitials(last);
            }
            return getInitials(first);
        } else if (!TextUtils.isEmpty(last)) {
            return getInitials(last);
        } else {
            return getInitials(user.getDisplayName());
        }
    }

    public static Map<String, Participant> createParticipantsMap(List<Participant> participants) {
        Map<String, Participant> participantMap = new HashMap<>(participants.size());
        for (Participant participant : participants) {
            participantMap.put(participant.getId(), participant);
        }
        return participantMap;
    }

    public static List<String> getIdsFromIdentities(Collection<Identity> identityHashSet) {
        List<String> isd = new ArrayList<>(identityHashSet.size());
        for (Identity identity : identityHashSet) {
            isd.add(identity.getUserId());
        }
        return isd;
    }

    private static String getInitials(String name) {
        if (TextUtils.isEmpty(name)) return "";
        if (name.contains(" ")) {
            String[] nameParts = name.split(" ");
            int count = 0;
            StringBuilder b = new StringBuilder();
            for (String part : nameParts) {
                String t = part.trim();
                if (t.isEmpty()) continue;
                b.append(("" + t.charAt(0)).toUpperCase());
                if (++count >= 2) break;
            }
            return b.toString();
        } else {
            return ("" + name.trim().charAt(0)).toUpperCase();
        }
    }

    @NonNull
    public static String getDisplayName(Identity identity) {
        if (TextUtils.isEmpty(identity.getDisplayName())) {
            String first = identity.getFirstName();
            String last = identity.getLastName();
            if (!TextUtils.isEmpty(first)) {
                if (!TextUtils.isEmpty(last)) {
                    return String.format("%s %s", first, last);
                }
                return first;
            } else if (!TextUtils.isEmpty(last)) {
                return last;
            } else {
                return identity.getUserId();
            }
        }
        return identity.getDisplayName();
    }

    public static String formatTime(Context context, Date date, DateFormat timeFormat, DateFormat dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeText;
        if (date.getTime() > todayMidnight) {
            timeText = timeFormat.format(date.getTime());
        } else if (date.getTime() > yesterMidnight) {
            timeText = context.getString(R.string.atlas_time_yesterday);
        } else if (date.getTime() > weekAgoMidnight) {
            cal.setTime(date);
            timeText = context.getResources().getStringArray(R.array.atlas_time_days_of_week)[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeText = dateFormat.format(date);
        }
        return timeText;
    }

    /**
     * Returns Today, Yesterday, the day of the week within one week, or a date if greater.
     *
     * @param context
     * @param date
     * @return
     */
    public static String formatTimeDay(Context context, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterdayMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeBarDayText;
        if (date.getTime() > todayMidnight) {
            timeBarDayText = context.getString(R.string.atlas_time_today);
        } else if (date.getTime() > yesterdayMidnight) {
            timeBarDayText = context.getString(R.string.atlas_time_yesterday);
        } else if (date.getTime() > weekAgoMidnight) {
            cal.setTime(date);
            timeBarDayText = context.getResources().getStringArray(R.array.atlas_time_days_of_week)[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeBarDayText = DAY_OF_WEEK.format(date);
        }
        return timeBarDayText;
    }

    /**
     * Returns int[] {scaledWidth, scaledHeight} for dimensions that fit within the given maxWidth,
     * maxHeight at the given inWidth, inHeight aspect ratio.  If the in dimensions fit fully inside
     * the max dimensions, no scaling is applied.  Otherwise, at least one scaled dimension is set
     * to a max dimension, and the other scaled dimension is scaled to fit.
     *
     * @param inWidth
     * @param inHeight
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static int[] scaleDownInside(int inWidth, int inHeight, int maxWidth, int maxHeight) {
        int scaledWidth;
        int scaledHeight;
        if (inWidth <= maxWidth && inHeight <= maxHeight) {
            scaledWidth = inWidth;
            scaledHeight = inHeight;
        } else {
            double widthRatio = (double) inWidth / (double) maxWidth;
            double heightRatio = (double) inHeight / (double) maxHeight;
            if (widthRatio > heightRatio) {
                scaledWidth = maxWidth;
                scaledHeight = (int) Math.round((double) inHeight / widthRatio);
            } else {
                scaledHeight = maxHeight;
                scaledWidth = (int) Math.round((double) inWidth / heightRatio);
            }
        }
        return new int[]{scaledWidth, scaledHeight};
    }

    /**
     * Synchronously starts downloading the given MessagePart and waits for downloading to complete.
     * Returns `true` if the MessagePart downloaded successfully within the given period of time, or
     * `false` otherwise.
     *
     * @param layerClient LayerClient to download the MessagePart with.
     * @param part        MessagePart to download.
     * @param timeLength  Length of time to wait for downloading.
     * @param timeUnit    Unit of time to wait for downloading.
     * @return `true` if the MessagePart content is available, or `false` otherwise.
     */
    public static boolean downloadMessagePart(LayerClient layerClient, MessagePart part, int timeLength, TimeUnit timeUnit) {
        if (part.isContentReady()) return true;

        final CountDownLatch latch = new CountDownLatch(1);
        final LayerProgressListener listener = new LayerProgressListener.BackgroundThread.Weak() {
            @Override
            public void onProgressStart(MessagePart messagePart, Operation operation) {

            }

            @Override
            public void onProgressUpdate(MessagePart messagePart, Operation operation, long l) {

            }

            @Override
            public void onProgressComplete(MessagePart messagePart, Operation operation) {
                latch.countDown();
            }

            @Override
            public void onProgressError(MessagePart messagePart, Operation operation, Throwable throwable) {
                latch.countDown();
            }
        };
        part.download(listener);
        if (!part.isContentReady()) {
            try {
                latch.await(timeLength, timeUnit);
            } catch (InterruptedException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
            }
        }
        return part.isContentReady();
    }

    /**
     * Asynchronously deauthenticates with Layer.
     *
     * @param layerClient LayerClient to deauthenticate.
     * @param callback    Callback to report deauthentication success and failure.
     */
    public static void deauthenticate(LayerClient layerClient, final DeauthenticationCallback callback) {
        final AtomicBoolean alerted = new AtomicBoolean(false);
        final LayerAuthenticationListener listener = new LayerAuthenticationListener.BackgroundThread() {
            @Override
            public void onAuthenticated(LayerClient layerClient, String s) {

            }

            @Override
            public void onDeauthenticated(LayerClient layerClient) {
                if (alerted.compareAndSet(false, true)) {
                    callback.onDeauthenticationSuccess(layerClient);
                }
            }

            @Override
            public void onAuthenticationChallenge(LayerClient layerClient, String s) {

            }

            @Override
            public void onAuthenticationError(LayerClient layerClient, LayerException e) {
                if (alerted.compareAndSet(false, true)) {
                    callback.onDeauthenticationFailed(layerClient, e.getMessage());
                }
            }
        };
        layerClient.registerAuthenticationListener(listener);
        if (!layerClient.isAuthenticated()) {
            layerClient.unregisterAuthenticationListener(listener);
            if (alerted.compareAndSet(false, true)) {
                callback.onDeauthenticationSuccess(layerClient);
            }
            return;
        }
        layerClient.deauthenticate();
    }

    public interface ContentAvailableCallback {
        void onContentAvailable(LayerClient client, Queryable object);

        void onContentFailed(LayerClient client, Uri objectId, String reason);
    }


    public interface DeauthenticationCallback {
        void onDeauthenticationSuccess(LayerClient client);

        void onDeauthenticationFailed(LayerClient client, String reason);
    }
}