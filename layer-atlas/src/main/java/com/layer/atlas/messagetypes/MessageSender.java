package com.layer.atlas.messagetypes;

import android.content.Context;

import com.layer.atlas.util.AtlasBugfenderLogger;
import com.layer.atlas.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

/**
 * MessageSenders handle the construction- and sending- of Messages.
 */
public abstract class MessageSender {
    private Conversation mConversation;
    private Callback mCallback;

    private Context mContext;
    private LayerClient mLayerClient;
    protected String mUserName;

    public void init(Context context, LayerClient layerClient) {
        mContext = context;
        mLayerClient = layerClient;
    }

    /**
     * Sets the Conversation used for sending generated Messages.
     *
     * @param conversation The Conversation to send generated Messages.
     */
    public MessageSender setConversation(Conversation conversation, String userName) {
        mConversation = conversation;
        mUserName = userName;
        return this;
    }

    /**
     * Sets an optional Callback for this MessageSender.
     *
     * @param callback Callback to receive MessageSender events.
     * @see #send(Message)
     */
    public MessageSender setCallback(Callback callback) {
        mCallback = callback;
        return this;
    }

    protected Context getContext() {
        return mContext;
    }

    protected LayerClient getLayerClient() {
        return mLayerClient;
    }

    /**
     * Sends the given Message to this MessageSender's Conversation.  If a Callback is registered,
     * the Callback may add options or abort sending.
     *
     * @param message Message to send.
     * @return `true` if the Message was queued for sending, or `false` if aborted.
     */
    protected boolean send(Message message) {
        if ((mCallback == null) || mCallback.beforeSend(this, mLayerClient, mConversation, message)) {
            mConversation.send(message);
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Message sent by " + getClass().getSimpleName());
            return true;
        }
        AtlasBugfenderLogger.log("Message failed to sent");
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Message sending aborted by " + getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Callback alerts external classes of MessageSender events.
     */
    public interface Callback {
        /**
         * Called when this MessageSender creates a new Message, before queuing the Message for
         * sending.  Options can be set on the Message at this time.  Return `true` to continue with
         * sending the Message, or `false` to prevent sending.  If sending continues, track updates
         * to the Message with a LayerChangeEventListener.
         *
         * @param sender              The MessageSender reporting Message creation.
         * @param layerClient         The LayerClient doing the sending.
         * @param conversation        The Conversation the new Message will be sent to.
         * @param message             The new Message created.
         * @return `true` to continue sending, or `false` to prevent sending
         * @see com.layer.sdk.messaging.Message
         * @see com.layer.sdk.listeners.LayerChangeEventListener
         */
        boolean beforeSend(MessageSender sender, LayerClient layerClient, Conversation conversation, Message message);
    }
}