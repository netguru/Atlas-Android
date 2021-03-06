package com.layer.atlas.util;

import com.layer.atlas.participant.Participant;
import com.layer.sdk.messaging.Conversation;

import java.util.List;

/**
 * A formatter that enables us to modify  conversation recyclerView items in the UI
 */

public class ConversationFormatter {
    private static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";//
    private static final String METADATA_KEY_CONVERSATION_TITLE_LEGACY = "title";

    public static String getConversationMetadataTitle(Conversation conversation) {
        String metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE);
        if (metadataTitle == null) {
            metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE_LEGACY);
        }
        if (metadataTitle != null && !metadataTitle.trim().isEmpty()) return metadataTitle.trim();
        return null;
    }

    public static void setConversationMetadataTitle(Conversation conversation, String title) {
        if (title == null || title.trim().isEmpty()) {
            conversation.removeMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE_LEGACY);
        } else {
            conversation.putMetadataAtKeyPath(METADATA_KEY_CONVERSATION_TITLE_LEGACY, title.trim());
        }
    }

    public static String getConversationTitle(List<Participant> participants, Conversation conversation) {
        String metadataTitle = getConversationMetadataTitle(conversation);
        if (metadataTitle == null || metadataTitle.isEmpty()) {
            //Try get title for legacy conversation
            metadataTitle = getConversationMetadataTitleLegacy(conversation);
        }
        if (metadataTitle != null) {
            return metadataTitle.trim();
        }

        StringBuilder sb = new StringBuilder();
        for (Participant participant : participants) {
            String initials = conversation.getParticipants().size() > 2 ? getInitialsFromParticipant(participant.getName()) : participant.getName();
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(initials);
        }
        return sb.toString().trim();
    }

    public static String getInitialsFromParticipant(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        if (fullName.contains(" ")) {
            String[] names = fullName.split(" ");
            int count = 0;
            StringBuilder b = new StringBuilder();
            for (String name : names) {
                String t = name.trim();
                if (t.isEmpty()) {
                    continue;
                }
                b.append(("" + t.charAt(0)).toUpperCase());
                if (++count >= 2) {
                    break;
                }
            }
            return b.toString();
        } else {
            return ("" + fullName.trim().charAt(0)).toUpperCase();
        }
    }

    private static String getConversationMetadataTitleLegacy(Conversation conversation) {
        String metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE_LEGACY);
        if (metadataTitle != null && !metadataTitle.trim().isEmpty()) {
            return metadataTitle.trim();
        }
        return null;
    }

    public static String getConversationTitle(Conversation conversation, List<Participant> participants, String authenticatedUserId) {
        StringBuilder sb = new StringBuilder();
        for (Participant participant : participants) {
            if (participant.getId().equals(authenticatedUserId)) {
                continue;
            }
            String initials = conversation.getParticipants().size() > 2 ? ConversationFormatter
                    .getInitialsFromParticipant(participant.getName()) : participant.getName();
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(initials);
        }
        return sb.toString().trim();
    }
}

