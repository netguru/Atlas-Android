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
package com.layer.atlas;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

import com.layer.atlas.adapters.AtlasMessagesAdapter;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.messagetypes.MessageStyle;
import com.layer.atlas.util.itemanimators.NoChangeAnimator;
import com.layer.atlas.util.views.SwipeableItem;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

public class AtlasMessagesRecyclerView extends RecyclerView {
    private AtlasMessagesAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ItemTouchHelper mSwipeItemTouchHelper;
    private boolean mShouldShowAvatarsInOneOnOneConversations;

    private MessageStyle mMessageStyle;

    public AtlasMessagesRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(getContext(), attrs, defStyle);
    }

    public AtlasMessagesRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasMessagesRecyclerView(Context context) {
        super(context);
    }

    public AtlasMessagesRecyclerView init(LayerClient layerClient, Picasso picasso) {
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mLayoutManager.setStackFromEnd(true);
        setLayoutManager(mLayoutManager);

        // Create an adapter that auto-scrolls if we're already at the bottom
        mAdapter = new AtlasMessagesAdapter(getContext(), layerClient, picasso)
                .setRecyclerView(this)
                .setOnMessageAppendListener(new AtlasMessagesAdapter.OnMessageAppendListener() {
                    @Override
                    public void onMessageAppend(AtlasMessagesAdapter adapter, Message message) {
                        autoScroll();
                    }
                });
        mAdapter.setStyle(mMessageStyle);
        super.setAdapter(mAdapter);

        // Don't flash items when changing content
        setItemAnimator(new NoChangeAnimator());

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                for (AtlasCellFactory factory : mAdapter.getCellFactories()) {
                    factory.onScrollStateChanged(newState);
                }
            }
        });

        setShouldShowAvatarInOneOnOneConversations(mShouldShowAvatarsInOneOnOneConversations);
        return this;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        throw new RuntimeException("AtlasMessagesRecyclerView sets its own Adapter");
    }

    /**
     * Performs cleanup when the Activity/Fragment using the adapter is destroyed.
     */
    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
    }

    /**
     * Automatically refresh on resume
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        refresh();
    }

    public AtlasMessagesRecyclerView refresh() {
        if (mAdapter != null) mAdapter.refresh();
        return this;
    }

    /**
     * Updates the underlying AtlasMessagesAdapter with a Query for Messages in the given
     * Conversation.
     *
     * @param conversation Conversation to display Messages for.
     * @return This AtlasMessagesRecyclerView.
     */
    public AtlasMessagesRecyclerView setConversation(Conversation conversation) {
        if (conversation != null) {
            mAdapter.setReadReceiptsEnabled(conversation.isReadReceiptsEnabled());
        }
        mAdapter.setQuery(Query.builder(Message.class)
                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                .build()).refresh();
        return this;
    }

    public AtlasMessagesRecyclerView setOnMessageSwipeListener(SwipeableItem.OnSwipeListener<Message> listener) {
        if (mSwipeItemTouchHelper != null) {
            mSwipeItemTouchHelper.attachToRecyclerView(null);
        }
        if (listener == null) {
            mSwipeItemTouchHelper = null;
        } else {
            listener.setBaseAdapter((AtlasMessagesAdapter) getAdapter());
            mSwipeItemTouchHelper = new ItemTouchHelper(listener);
            mSwipeItemTouchHelper.attachToRecyclerView(this);
        }
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#addCellFactories(AtlasCellFactory...)
     */
    public AtlasMessagesRecyclerView addCellFactories(AtlasCellFactory... cellFactories) {
        mAdapter.addCellFactories(cellFactories);
        return this;
    }

    public AtlasMessagesRecyclerView setTextTypeface(Typeface myTypeface, Typeface otherTypeface) {
        mMessageStyle.setMyTextTypeface(myTypeface);
        mMessageStyle.setOtherTextTypeface(otherTypeface);
        return this;
    }

    /**
     * Convenience pass-through to this list's LinearLayoutManager.
     *
     * @see LinearLayoutManager#findLastVisibleItemPosition()
     */
    private int findLastVisibleItemPosition() {
        return mLayoutManager.findLastVisibleItemPosition();
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#setFooterView(View)
     */
    public AtlasMessagesRecyclerView setFooterView(View footerView) {
        mAdapter.setFooterView(footerView);
        autoScroll();
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#getFooterView()
     */
    public View getFooterView() {
        return mAdapter.getFooterView();
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#getShouldShowAvatarInOneOnOneConversations()
     */
    public boolean getShouldShowAvatarInOneOnOneConversations() {
        return mAdapter.getShouldShowAvatarInOneOnOneConversations();
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#setShouldShowAvatarInOneOnOneConversations(boolean)
     */
    public AtlasMessagesRecyclerView setShouldShowAvatarInOneOnOneConversations(boolean shouldShowAvatarInOneOnOneConversations) {
        mAdapter.setShouldShowAvatarInOneOnOneConversations(shouldShowAvatarInOneOnOneConversations);
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#getShouldShowAvatarPresence()
     */
    public boolean getShouldShowAvatarPresence() {
        return mAdapter.getShouldShowAvatarPresence();
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#setShouldShowAvatarPresence(boolean)
     */
    public AtlasMessagesRecyclerView setShouldShowAvatarPresence(boolean shouldShowAvatarPresence) {
        mAdapter.setShouldShowAvatarPresence(shouldShowAvatarPresence);
        return this;
    }

    /**
     * Scrolls if the user is at the end
     */
    private void autoScroll() {
        int end = mAdapter.getItemCount() - 1;
        if (end <= 0) return;
        int visible = findLastVisibleItemPosition();
        // -3 because -1 seems too finicky
        if (visible >= (end - 3)) scrollToPosition(end);
    }

    public void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasMessagesRecyclerView, R.attr.AtlasMessagesRecyclerView, defStyle);
        MessageStyle.Builder messageStyleBuilder = new MessageStyle.Builder();
        messageStyleBuilder.myTextColor(ta.getColor(R.styleable.AtlasMessagesRecyclerView_myTextColor, context.getResources().getColor(R.color.atlas_text_black)));
        int myTextStyle = ta.getInt(R.styleable.AtlasMessagesRecyclerView_myTextStyle, Typeface.NORMAL);
        messageStyleBuilder.myTextStyle(myTextStyle);
        String myTextTypefaceName = ta.getString(R.styleable.AtlasMessagesRecyclerView_myTextTypeface);
        messageStyleBuilder.myTextTypeface(myTextTypefaceName != null ? Typeface.create(myTextTypefaceName, myTextStyle) : null);
        messageStyleBuilder.myTextSize(ta.getDimensionPixelSize(R.styleable.AtlasMessagesRecyclerView_myTextSize, context.getResources().getDimensionPixelSize(R.dimen.atlas_text_size_message_item)));

        messageStyleBuilder.otherTextColor(ta.getColor(R.styleable.AtlasMessagesRecyclerView_theirTextColor, context.getResources().getColor(R.color.atlas_color_primary_blue)));
        int otherTextStyle = ta.getInt(R.styleable.AtlasMessagesRecyclerView_theirTextStyle, Typeface.NORMAL);
        messageStyleBuilder.otherTextStyle(otherTextStyle);
        String otherTextTypefaceName = ta.getString(R.styleable.AtlasMessagesRecyclerView_theirTextTypeface);
        messageStyleBuilder.otherTextTypeface(otherTextTypefaceName != null ? Typeface.create(otherTextTypefaceName, otherTextStyle) : null);
        messageStyleBuilder.otherTextSize(ta.getDimensionPixelSize(R.styleable.AtlasMessagesRecyclerView_theirTextSize, context.getResources().getDimensionPixelSize(R.dimen.atlas_text_size_message_item)));

        messageStyleBuilder.myBubbleColor(ta.getColor(R.styleable.AtlasMessagesRecyclerView_myBubbleColor, context.getResources().getColor(R.color.atlas_color_primary_blue)));
        messageStyleBuilder.otherBubbleColor(ta.getColor(R.styleable.AtlasMessagesRecyclerView_theirBubbleColor, context.getResources().getColor(R.color.atlas_color_primary_gray)));

        mShouldShowAvatarsInOneOnOneConversations = ta.getBoolean(R.styleable.AtlasMessagesRecyclerView_shouldShowAvatarsInOneOnOneConversations, false);
        ta.recycle();
        this.mMessageStyle = messageStyleBuilder.build();
    }
}