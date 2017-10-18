package com.layer.atlas.messagetypes.text;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.util.Map;
import java.util.WeakHashMap;

public class TextCellFactory extends AtlasCellFactory<TextCellFactory.CellHolder, TextCellFactory.TextInfo> implements View.OnLongClickListener {
    public final static String MIME_TYPE = "text/plain";
    //This is used to bind TextView  to the exact message to ensure the right TextView is updated
    private Map<TextView, Uri> mTextViewUriHashMap =  new WeakHashMap<>();

    public TextCellFactory() {
        super(256 * 1024);
    }

    @Override
    public boolean isBindable(Message message) {
        return isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        View v = layoutInflater.inflate(R.layout.atlas_message_item_cell_text, cellView, true);
        v.setBackgroundResource(isMe ? R.drawable.atlas_message_item_cell_me : R.drawable.atlas_message_item_cell_them);
        ((GradientDrawable) v.getBackground()).setColor(isMe ? mMessageStyle.getMyBubbleColor() : mMessageStyle.getOtherBubbleColor());

        TextView t = (TextView) v.findViewById(R.id.cell_text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, isMe ? mMessageStyle.getMyTextSize() : mMessageStyle.getOtherTextSize());
        t.setTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setLinkTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setTypeface(isMe ? mMessageStyle.getMyTextTypeface() : mMessageStyle.getOtherTextTypeface(), isMe ? mMessageStyle.getMyTextStyle() : mMessageStyle.getOtherTextStyle());
        return new CellHolder(v);
    }

    @Override
    public TextInfo parseContent(LayerClient layerClient, Message message) {
        MessagePart part = message.getMessageParts().get(0);
        String text = part.isContentReady() ? new String(part.getData()) : null;
        String name;
        Identity sender = message.getSender();
        if (sender != null) {
            name = Util.getDisplayName(sender) + ": ";
        } else {
            name = "";
        }
        return new TextInfo(text, name);
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, final TextInfo parsed, Message message, CellHolderSpecs specs) {

        //Checking if the TextView is being recycled, replace the value in the map with the new message id
        if (mTextViewUriHashMap.containsKey(cellHolder.mTextView)) {
            mTextViewUriHashMap.put(cellHolder.mTextView, message.getId());
            cellHolder.mProgressBar.hide();
        }

        String textMessage = parsed.getString();
        //This string will be null if the message part content is not Ready
        if (textMessage == null) {
            if (message.getMessageParts().get(0).isContentReady()) {
                textMessage = new String(message.getMessageParts().get(0).getData());
            } else {
                downloadMessage(message, cellHolder);
                cellHolder.mProgressBar.setVisibility(View.VISIBLE);
                cellHolder.mProgressBar.show();
            }
        }
        cellHolder.mTextView.setText(textMessage);
        cellHolder.mTextView.setTag(parsed);
        cellHolder.mTextView.setOnLongClickListener(this);
    }

    private void downloadMessage(final Message message, final CellHolder cellHolder) {
        final MessagePart part = message.getMessageParts().get(0);
        final TextView textView = cellHolder.mTextView;
        mTextViewUriHashMap.put(textView, message.getId());
        LayerProgressListener layerProgressListener = new LayerProgressListener.Weak() {
            @Override
            public void onProgressStart(MessagePart messagePart, Operation operation) {}

            @Override
            public void onProgressUpdate(MessagePart messagePart, Operation operation, long l) {}

            @Override
            public void onProgressComplete(MessagePart messagePart, Operation operation) {
                //Check the downloaded message to ensure the TextView has not been recycled
                Uri messageId = messagePart.getMessage().getId();
                Uri uriValueInMap = mTextViewUriHashMap.get(textView);
                if (uriValueInMap != null && uriValueInMap.equals(messageId) ) {
                    textView.setText(new String(part.getData()));
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
        part.download(layerProgressListener);
    }

    public boolean isType(Message message) {
        return message.getMessageParts().size() == 1 &&  message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    @Override
    public String getPreviewText(Context context, Message message) {
        if (isType(message)) {
            MessagePart part = message.getMessageParts().get(0);
            // For large text content, the MessagePart may not be downloaded yet.
            return part.isContentReady() ? new String(part.getData()) : "";
        }
        else {
            throw new IllegalArgumentException("Message is not of the correct type - Text");
        }
    }

    /**
     * Long click copies message text and sender name to clipboard
     */
    @Override
    public boolean onLongClick(View v) {
        TextInfo parsed = (TextInfo) v.getTag();
        String text = parsed.getClipboardPrefix() + parsed.getString();
        Util.copyToClipboard(v.getContext(), R.string.atlas_text_cell_factory_clipboard_description, text);
        Toast.makeText(v.getContext(), R.string.atlas_text_cell_factory_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        return true;
    }

    public static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;
        ContentLoadingProgressBar mProgressBar;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
            mProgressBar = (ContentLoadingProgressBar) view.findViewById(R.id.text_cell_progress);
        }
    }

    public static class TextInfo implements AtlasCellFactory.ParsedContent {
        private final String mString;
        private final String mClipboardPrefix;
        private final int mSize;

        public TextInfo(String string, String clipboardPrefix) {
            mString = string;
            mClipboardPrefix = clipboardPrefix;
            int clipboardLength = mClipboardPrefix.getBytes().length;
            mSize = (mString != null) ? mString.getBytes().length + clipboardLength : clipboardLength;
        }

        public String getString() {
            return mString;
        }

        public String getClipboardPrefix() {
            return mClipboardPrefix;
        }

        @Override
        public int sizeOf() {
            return mSize;
        }
    }
}
