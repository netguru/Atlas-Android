package com.layer.atlas.messagetypes.singlepartimage;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.imagepopup.AtlasImagePopupActivity;
import com.layer.atlas.util.picasso.transformations.RoundedTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import java.util.Set;

/**
 * BasicImage handles non-ThreePartImage images.  It relies on the ThreePartImage RequestHandler and does not handle image rotation.
 */
public class SinglePartImageCellFactory extends AtlasCellFactory<SinglePartImageCellFactory.CellHolder, SinglePartImageCellFactory.PartId> implements View.OnClickListener {
    private static final String PICASSO_TAG = SinglePartImageCellFactory.class.getSimpleName();
    private static final int PLACEHOLDER = com.layer.atlas.R.drawable.atlas_message_item_cell_placeholder;
    private static final int CACHE_SIZE_BYTES = 256 * 1024;

    private final LayerClient mLayerClient;
    private final Picasso mPicasso;
    private Transformation mTransform;

    public SinglePartImageCellFactory(LayerClient mLayerClient, Picasso mPicasso) {
        super(CACHE_SIZE_BYTES);
        this.mLayerClient = mLayerClient;
        this.mPicasso = mPicasso;
    }

    public SinglePartImageCellFactory(Activity activity, LayerClient layerClient, Picasso picasso) {
        this(layerClient, picasso);
    }

    @Override
    public boolean isBindable(Message message) {
        return isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, PartId index, Message message, CellHolderSpecs specs) {
        cellHolder.mImageView.setTag(index);
        cellHolder.mImageView.setOnClickListener(this);
        cellHolder.mProgressBar.show();
        RequestCreator requestCreator = mPicasso.load(index.mId).tag(PICASSO_TAG).placeholder(PLACEHOLDER)
                .centerInside();
        if (specs.maxWidth > 0 && specs.maxHeight > 0) {
            requestCreator.resize(specs.maxWidth, specs.maxHeight);
        } else if (Log.isLoggable(Log.ERROR)) {
            Log.e("Width or Height of image passed into SinglePartImageCellFactory.bindCellHolder should be > 0");
        }
        requestCreator.onlyScaleDown()
                .transform(getTransform(cellHolder.mImageView.getContext()));

        requestCreator.into(cellHolder.mImageView, new Callback() {
            @Override
            public void onSuccess() {
                cellHolder.mProgressBar.hide();
            }

            @Override
            public void onError() {
                cellHolder.mProgressBar.hide();
            }
        });
    }

    @Override
    public void onClick(View v) {
        AtlasImagePopupActivity.init(mLayerClient);
        Context context = v.getContext();
        if (context == null) return;
        Intent intent = new Intent(context, AtlasImagePopupActivity.class);
        intent.putExtra("fullId", ((PartId) v.getTag()).mId);

        if (Build.VERSION.SDK_INT >= 21 && context instanceof Activity) {
            context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation((Activity) context, v, "image").toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public void onScrollStateChanged(int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                mPicasso.pauseTag(PICASSO_TAG);
                break;
            case RecyclerView.SCROLL_STATE_IDLE:
            case RecyclerView.SCROLL_STATE_SETTLING:
                mPicasso.resumeTag(PICASSO_TAG);
                break;
        }
    }

    @Override
    public PartId parseContent(LayerClient layerClient, Message message) {
        for (MessagePart part : message.getMessageParts()) {
            if (part.getMimeType().startsWith("image/")) return new PartId(part.getId());
        }
        return null;
    }

    @Override
    public boolean isType(Message message) {
        Set<MessagePart> parts = message.getMessageParts();
        return parts.size() == 1
                && parts.iterator().next().getMimeType().startsWith("image/");
    }

    @Override
    public String getPreviewText(Context context, Message message) {
        if (isType(message)) {
            return context.getString(R.string.atlas_message_preview_image);
        }
        else {
            throw new IllegalArgumentException("Message is not of the correct type - SinglePartImage");
        }
    }

    //==============================================================================================
    // private methods
    //==============================================================================================

    private Transformation getTransform(Context context) {
        if (mTransform == null) {
            float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
            mTransform = new RoundedTransform(radius);
        }

        return mTransform;
    }

    //==============================================================================================
    // Inner classes
    //==============================================================================================

    public static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;
        ContentLoadingProgressBar mProgressBar;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
            mProgressBar = (ContentLoadingProgressBar) view.findViewById(R.id.cell_progress);
        }
    }

    public static class PartId implements AtlasCellFactory.ParsedContent {
        public final Uri mId;

        public PartId(Uri id) {
            mId = id;
        }

        @Override
        public int sizeOf() {
            return mId.toString().getBytes().length;
        }
    }
}
