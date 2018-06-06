package com.layer.atlas.messagetypes.meeting;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.reactivex.subjects.PublishSubject;

public class MeetingCreatedCellFactory extends AtlasCellFactory<TextCellFactory.CellHolder, MeetingCreatedCellFactory.MeetingInfo> {
    private final static String MIME_TYPE_MEETING = "application/vnd.websummit.meeting+json";
    private static final String MEETING_ID_JSON_KEY = "meeting_id";
    private static final String MEETING_BACKGROUND_COLOR_JSON_KEY = "background_color";
    private static final String MEETING_FONT_COLOR_JSON_KEY = "font_color";

    //This is used to bind TextView  to the exact message to ensure the right TextView is updated
    private Map<TextView, Uri> mTextViewUriHashMap = new WeakHashMap<>();
    @Nullable
    private final PublishSubject<String> clickSubject;

    public MeetingCreatedCellFactory(@Nullable PublishSubject<String> clickSubject) {
        super(256 * 1024);
        this.clickSubject = clickSubject;
    }

    @Override
    public boolean isBindable(Message message) {
        return isType(message);
    }

    @Override
    public TextCellFactory.CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        View v = layoutInflater.inflate(R.layout.atlas_message_item_cell_text, cellView, true);
        v.setBackgroundResource(isMe ? R.drawable.atlas_message_item_cell_me : R.drawable.atlas_message_item_cell_them);

        TextView t = v.findViewById(R.id.cell_text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, isMe ? mMessageStyle.getMyTextSize() : mMessageStyle.getOtherTextSize());
        t.setTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setLinkTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setTypeface(isMe ? mMessageStyle.getMyTextTypeface() : mMessageStyle.getOtherTextTypeface(), isMe ? mMessageStyle.getMyTextStyle() : mMessageStyle.getOtherTextStyle());
        return new TextCellFactory.CellHolder(v);
    }

    @Override
    public MeetingInfo parseContent(LayerClient layerClient, Message message) {
        MeetingParts parts = new MeetingParts(message);
        String text = parts.getTextPart().isContentReady() ? new String(parts.getTextPart().getData()) : null;
        String meetingId = null;
        String backgroundColor = null;
        String fontColor = null;
        try {
            JSONObject infoObject = new JSONObject(new String(parts.getMeetingPart().getData()));
            meetingId = infoObject.getString(MEETING_ID_JSON_KEY);
            if (infoObject.has(MEETING_BACKGROUND_COLOR_JSON_KEY)) {
                backgroundColor = infoObject.getString(MEETING_BACKGROUND_COLOR_JSON_KEY);
            }
            if (infoObject.has(MEETING_FONT_COLOR_JSON_KEY)) {
                fontColor = infoObject.getString(MEETING_FONT_COLOR_JSON_KEY);
            }
        } catch (JSONException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
        return new MeetingInfo(text, meetingId, backgroundColor, fontColor);
    }

    @Override
    public void bindCellHolder(TextCellFactory.CellHolder cellHolder, MeetingInfo cached, Message message, CellHolderSpecs specs) {
        //Checking if the TextView is being recycled, replace the value in the map with the new message id
        if (mTextViewUriHashMap.containsKey(cellHolder.mTextView)) {
            mTextViewUriHashMap.put(cellHolder.mTextView, message.getId());
            cellHolder.mProgressBar.hide();
        }

        if (!MeetingInfo.isEmpty(cached.getBackgroundColor())) {
            ((GradientDrawable) cellHolder.root.getBackground())
                    .setColor(Color.parseColor(cached.getBackgroundColor()));
        } else {
            ((GradientDrawable) cellHolder.root.getBackground()).setColor(mMessageStyle.getOtherBubbleColor());
        }

        if(!MeetingInfo.isEmpty(cached.getFontColor()))
            cellHolder.mTextView.setTextColor(Color.parseColor(cached.getFontColor()));

        String textMessage = cached.getText();
        MeetingParts parts = new MeetingParts(message);
        //This string will be null if the message part content is not Ready
        if (textMessage == null) {
            if (parts.getTextPart().isContentReady()) {
                textMessage = new String(parts.getTextPart().getData());
            } else {
                downloadMessage(message, cellHolder);
                cellHolder.mProgressBar.setVisibility(View.VISIBLE);
                cellHolder.mProgressBar.show();
            }
        }
        cellHolder.mTextView.setText(textMessage);
        cellHolder.mTextView.setTag(cached);
        cellHolder.mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeetingInfo parsed = (MeetingInfo) view.getTag();
                if (!MeetingInfo.isEmpty(parsed.getMeetingId()) && clickSubject != null) {
                    clickSubject.onNext(parsed.getMeetingId());
                }
            }
        });
    }

    private void downloadMessage(final Message message, final TextCellFactory.CellHolder cellHolder) {
        final MeetingParts parts = new MeetingParts(message);
        final TextView textView = cellHolder.mTextView;
        mTextViewUriHashMap.put(textView, message.getId());
        LayerProgressListener layerProgressListener = new LayerProgressListener.Weak() {
            @Override
            public void onProgressStart(MessagePart messagePart, Operation operation) {
            }

            @Override
            public void onProgressUpdate(MessagePart messagePart, Operation operation, long l) {
            }

            @Override
            public void onProgressComplete(MessagePart messagePart, Operation operation) {
                //Check the downloaded message to ensure the TextView has not been recycled
                Uri messageId = messagePart.getMessage().getId();
                Uri uriValueInMap = mTextViewUriHashMap.get(textView);
                if (uriValueInMap != null && uriValueInMap.equals(messageId)) {
                    textView.setText(new String(parts.getTextPart().getData()));
                    mTextViewUriHashMap.remove(textView);
                    cellHolder.mProgressBar.hide();
                }
            }

            @Override
            public void onProgressError(MessagePart messagePart, Operation operation, Throwable throwable) {
                mTextViewUriHashMap.remove(textView);
                cellHolder.mProgressBar.hide();
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e("Message part download error: " + messagePart.getId(), throwable);
                }
            }
        };
        parts.getTextPart().download(layerProgressListener);
    }

    @Override
    public boolean isType(Message message) {
        Set<MessagePart> parts = message.getMessageParts();
        if (parts.size() != 2) {
            return false;
        }
        boolean hasText = false;
        boolean hasMeeting = false;
        for (MessagePart part : parts) {
            if (part.getMimeType().equals(TextCellFactory.MIME_TYPE)) {
                hasText = true;
            } else if (part.getMimeType().equals(MIME_TYPE_MEETING)) {
                hasMeeting = true;
            }
        }
        return hasText && hasMeeting;
    }

    @Override
    public String getPreviewText(Context context, Message message) {
        if (isType(message)) {
            MeetingParts parts = new MeetingParts(message);
            // For large text content, the MessagePart may not be downloaded yet.
            return parts.getTextPart().isContentReady() ? new String(parts.getTextPart().getData()) : "";
        } else {
            throw new IllegalArgumentException("Message is not of the correct type - Text");
        }
    }

    //==============================================================================================
    // Inner classes
    //==============================================================================================

    public static class MeetingInfo implements AtlasCellFactory.ParsedContent {
        private final String text;
        private final String meetingId;
        private final String backgroundColor;
        private final String fontColor;

        MeetingInfo(@Nullable String text,
                    @Nullable String meetingId,
                    @Nullable String backgroundColor,
                    @Nullable String fontColor) {
            this.text = text;
            this.meetingId = meetingId;
            this.backgroundColor = backgroundColor;
            this.fontColor = fontColor;
        }

        @Override
        public int sizeOf() {
            int textSize = isEmpty(text) ? 0 : text.getBytes().length;
            int meetingIdSize = isEmpty(meetingId) ? 0 : meetingId.getBytes().length;
            int backgroundColorSize = isEmpty(backgroundColor) ? 0 : backgroundColor.getBytes().length;
            int fontColorSize = isEmpty(fontColor) ? 0 : fontColor.getBytes().length;
            return textSize + meetingIdSize + backgroundColorSize + fontColorSize;
        }

        @Nullable
        public String getText() {
            return text;
        }

        @Nullable
        String getMeetingId() {
            return meetingId;
        }

        @Nullable
        public String getBackgroundColor() {
            return backgroundColor;
        }

        @Nullable
        public String getFontColor(){
            return fontColor;
        }

        private static boolean isEmpty(@Nullable String text) {
            return text == null || text.trim().isEmpty();
        }
    }

    private static class MeetingParts {
        private MessagePart textPart;
        private MessagePart meetingPart;

        MeetingParts(Message message) {
            Set<MessagePart> messageParts = message.getMessageParts();
            for (MessagePart part : messageParts) {
                if (part.getMimeType().equals(TextCellFactory.MIME_TYPE)) {
                    textPart = part;
                } else if (part.getMimeType().equals(MIME_TYPE_MEETING)) {
                    meetingPart = part;
                }
            }
            if (textPart == null || meetingPart == null) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e("Incorrect parts for a two part meeting: " + messageParts);
                }
                throw new IllegalArgumentException("Incorrect parts for a two part meeting: " + messageParts);
            }
        }

        @NonNull
        MessagePart getTextPart() {
            return textPart;
        }

        @NonNull
        MessagePart getMeetingPart() {
            return meetingPart;
        }
    }
}
