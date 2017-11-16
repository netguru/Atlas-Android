package com.layer.atlas.messagetypes.text;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.MessageSender;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.messaging.PushNotificationPayload;

public class TextSender extends MessageSender {
    private final int mMaxNotificationLength;

    public TextSender() {
        this(200);
    }

    public TextSender(int maxNotificationLength) {
        mMaxNotificationLength = maxNotificationLength;
    }

    public boolean requestSend(String text) {
        if (text == null || text.trim().length() == 0) {
            if (Log.isLoggable(Log.ERROR)) Log.e("No text to send");
            return false;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending text message");

        if (Log.isPerfLoggable()) {
            Log.perf("TextSender is attempting to send a message");
        }

        // Create notification string
        Identity me = getLayerClient().getAuthenticatedUser();
        String myName = me == null ? "" : Util.getDisplayName(me);
        String notificationString = getContext().getString(R.string.atlas_notification_text, myName, (text.length() < mMaxNotificationLength) ? text : (text.substring(0, mMaxNotificationLength) + "…"));

        // Send message
        MessagePart part = getLayerClient().newMessagePart(text);
        PushNotificationPayload payload = new PushNotificationPayload.Builder()
                .text(notificationString)
                .build();
        Message message = getLayerClient().newMessage(new MessageOptions().defaultPushNotificationPayload(payload), part);
        return send(message);
    }
}