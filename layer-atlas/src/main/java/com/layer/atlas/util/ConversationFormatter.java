package com.layer.atlas.util;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;

import java.util.Set;

/**
 * A formatter that enables us to modify  conversation recyclerView items in the UI
 */

public class ConversationFormatter {
    private static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";

    public  String getConversationTitle(LayerClient client, Conversation conversation) {
        return getConversationTitle(client, conversation, conversation.getParticipants());
    }


    public  String getConversationTitle(LayerClient client, Conversation conversation, Set<Identity> participants) {
        String metadataTitle = getConversationMetadataTitle(conversation);
        if (metadataTitle != null) return metadataTitle.trim();

        StringBuilder sb = new StringBuilder();
        Identity authenticatedUser = client.getAuthenticatedUser();
        for (Identity participant : participants) {
            if (participant.equals(authenticatedUser)) continue;
            String initials = participants.size() > 2 ? Util.getInitials(participant) : Util.getDisplayName(participant);
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials);
        }
        return sb.toString();
    }

    public  String getConversationMetadataTitle(Conversation conversation) {
        String metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE);
        if (metadataTitle != null && !metadataTitle.trim().isEmpty()) return metadataTitle.trim();
        return null;
    }

    public static void setConversationMetadataTitle(Conversation conversation, String title) {
        if (title == null || title.trim().isEmpty()) {
            conversation.removeMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE);
        } else {
            conversation.putMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE, title.trim());
        }
    }
}

