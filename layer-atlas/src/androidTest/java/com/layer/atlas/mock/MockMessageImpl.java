package com.layer.atlas.mock;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MockMessageImpl implements Message {

    private List<MessagePart> mMessageParts = new ArrayList<>();

    public MockMessageImpl(List<MessagePart> messageParts) {
        mMessageParts = messageParts;
    }

    @Override
    public void delete(LayerClient.DeletionMode deletionMode) {

    }

    @Override
    public void markAsRead() {

    }

    @Override
    public Uri getId() {
        return null;
    }

    @Override
    public long getPosition() {
        return 0;
    }

    @Override
    public Conversation getConversation() {
        return null;
    }

    @Override
    public List<MessagePart> getMessageParts() {
        return mMessageParts;
    }

    @Override
    public boolean isSent() {
        return true;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public Date getSentAt() {
        return null;
    }

    @Override
    public Date getReceivedAt() {
        return null;
    }

    @Nullable
    @Override
    public Identity getSender() {
        return null;
    }

    @Override
    public Map<Identity, RecipientStatus> getRecipientStatus() {
        return null;
    }

    @Override
    public RecipientStatus getRecipientStatus(Identity identity) {
        return null;
    }

    @Override
    public MessageOptions getOptions() {
        return null;
    }
}
