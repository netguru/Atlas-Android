package com.layer.atlas.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.messagetypes.generic.GenericCellFactory;
import com.layer.atlas.messagetypes.location.LocationCellFactory;
import com.layer.atlas.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.atlas.support.ChatAttendeesProvider;
import com.layer.atlas.support.Participant;
import com.layer.atlas.util.ConversationStyle;
import com.layer.atlas.util.IdentityRecyclerViewEventListener;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class AtlasConversationsAdapter extends RecyclerView.Adapter<AtlasConversationsAdapter.ViewHolder> implements AtlasBaseAdapter<Conversation>, RecyclerViewController.Callback {
    protected final LayerClient mLayerClient;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Conversation> mQueryController;
    private final LayoutInflater mInflater;
    //Added to work with participant object
    private final ChatAttendeesProvider chatAttendeesProvider;
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private long mInitialHistory = 0;

    private OnConversationClickListener mConversationClickListener;
    private ViewHolder.OnClickListener mViewHolderClickListener;

    private final DateFormat mDateFormat;
    private final DateFormat mTimeFormat;
    private ConversationStyle conversationStyle;
    private final IdentityRecyclerViewEventListener mIdentityEventListener;

    protected Set<AtlasCellFactory> mCellFactories;
    private Set<AtlasCellFactory> mDefaultCellFactories;

    public AtlasConversationsAdapter(Context context, LayerClient client, Picasso picasso, ChatAttendeesProvider chatAttendeesProvider) {
        this(context, client, picasso, chatAttendeesProvider, null);
    }

    public AtlasConversationsAdapter(Context context, LayerClient client, Picasso picasso, ChatAttendeesProvider chatAttendeesProvider, Collection<String> updateAttributes) {
        this.chatAttendeesProvider = chatAttendeesProvider;
        Query<Conversation> query = Query.builder(Conversation.class)
                /* Only show conversations we're still a member of */
                .predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 1))

                /* Sort by the last Message's receivedAt time */
                .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING))
                .build();
        mQueryController = client.newRecyclerViewController(query, updateAttributes, this);
        mLayerClient = client;
        mPicasso = picasso;
        mInflater = LayoutInflater.from(context);
        mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        mViewHolderClickListener = new ViewHolder.OnClickListener() {
            @Override
            public void onClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return;

                if (Log.isPerfLoggable()) {
                    Log.perf("Conversation ViewHolder onClick");
                }

                mConversationClickListener.onConversationClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }

            @Override
            public boolean onLongClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return false;
                return mConversationClickListener.onConversationLongClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }
        };
        setHasStableIds(false);

        mIdentityEventListener = new IdentityRecyclerViewEventListener(this);
        mLayerClient.registerEventListener(mIdentityEventListener);
    }

    public AtlasConversationsAdapter addCellFactories(AtlasCellFactory... cellFactories) {
        if (mCellFactories == null) {
            mCellFactories = new LinkedHashSet<AtlasCellFactory>();
        }
        Collections.addAll(mCellFactories, cellFactories);
        return this;
    }

    /**
     * Refreshes this adapter by re-running the underlying Query.
     */
    public void refresh() {
        mQueryController.execute();
    }

    /**
     * Performs cleanup when the Activity/Fragment using the adapter is destroyed.
     */
    public void onDestroy() {
        subscriptions.clear();
        mLayerClient.unregisterEventListener(mIdentityEventListener);
    }

    //==============================================================================================
    // Initial message history
    //==============================================================================================

    public AtlasConversationsAdapter setInitialHistoricMessagesToFetch(long initialHistory) {
        mInitialHistory = initialHistory;
        return this;
    }

    public void setStyle(ConversationStyle conversationStyle) {
        this.conversationStyle = conversationStyle;
    }

    private void syncInitialMessages(final int start, final int length) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long desiredHistory = mInitialHistory;
                if (desiredHistory <= 0) return;
                for (int i = start; i < start + length; i++) {
                    try {
                        final Conversation conversation = getItem(i);
                        if (conversation == null || conversation.getHistoricSyncStatus() != Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
                            continue;
                        }
                        Query<Message> localCountQuery = Query.builder(Message.class)
                                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                                .build();
                        long delta = desiredHistory - mLayerClient.executeQueryForCount(localCountQuery);
                        if (delta > 0) conversation.syncMoreHistoricMessages((int) delta);
                    } catch (IndexOutOfBoundsException e) {
                        // Concurrent modification
                    }
                }
            }
        }).start();
    }


    //==============================================================================================
    // Listeners
    //==============================================================================================

    public AtlasConversationsAdapter setOnConversationClickListener(OnConversationClickListener conversationClickListener) {
        mConversationClickListener = conversationClickListener;
        return this;
    }


    //==============================================================================================
    // Adapter
    //==============================================================================================

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new ViewHolder(mInflater.inflate(ViewHolder.RESOURCE_ID, parent, false), conversationStyle);
        viewHolder.setClickListener(mViewHolderClickListener);
        viewHolder.mAvatarCluster
                .init(mPicasso)
                .setStyle(conversationStyle.getAvatarStyle());
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        mQueryController.updateBoundPosition(position);
        final Conversation conversation = mQueryController.getItem(position);
        final Message lastMessage = conversation.getLastMessage();
        Context context = viewHolder.itemView.getContext();

        viewHolder.setConversation(conversation);
        Set<Identity> participants = conversation.getParticipants();
        participants.remove(mLayerClient.getAuthenticatedUser());

        // Add the position to the positions map for Identity updates
        mIdentityEventListener.addIdentityPosition(position, participants);

        viewHolder.mTitleView.setText("");
        viewHolder.applyStyle(conversation.getTotalUnreadMessageCount() > 0);

        if (lastMessage == null) {
            viewHolder.mMessageView.setText(null);
            viewHolder.mTimeView.setText(null);
        } else {
            viewHolder.mMessageView.setText(this.getLastMessageString(context, lastMessage));
            if (lastMessage.getReceivedAt() == null) {
                viewHolder.mTimeView.setText(null);
            } else {
                viewHolder.mTimeView.setText(Util.formatTime(context, lastMessage.getReceivedAt(), mTimeFormat, mDateFormat));
            }
        }

        List<String> ids = Util.getIdsFromIdentities(participants);
        Subscription participantsSingleSubscription = chatAttendeesProvider.getParticipants(ids).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Action1<List<Participant>>() {
                    @Override
                    public void call(List<Participant> participants) {
                        if (!participants.isEmpty()) {
                            viewHolder.mTitleView.setText(Util.getConversationTitle(participants, conversation));
                            viewHolder.mAvatarCluster.setParticipants(Util.createParticipantsMap(participants));
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d("Error during participants downloading", throwable);
                    }
                });
        subscriptions.add(participantsSingleSubscription);
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount();
    }

    @Override
    public Integer getPosition(Conversation conversation) {
        return mQueryController.getPosition(conversation);
    }

    @Override
    public Integer getPosition(Conversation conversation, int lastPosition) {
        return mQueryController.getPosition(conversation, lastPosition);
    }

    @Override
    public Conversation getItem(int position) {
        return mQueryController.getItem(position);
    }

    @Override
    public Conversation getItem(RecyclerView.ViewHolder viewHolder) {
        return ((ViewHolder) viewHolder).getConversation();
    }

    //==============================================================================================
    // Util methods
    //==============================================================================================

    private String getLastMessageString(Context context, Message message) {
        Set<AtlasCellFactory> cellFactories = (mCellFactories == null || mCellFactories.isEmpty()) ? getDefaultCellFactories() : mCellFactories;

        for (AtlasCellFactory cellFactory : cellFactories) {
            if (cellFactory.isType(message)) {
                return cellFactory.getPreviewText(context, message);
            }
        }

        return GenericCellFactory.getPreview(context, message);
    }

    private Set<AtlasCellFactory> getDefaultCellFactories() {
        if (mDefaultCellFactories == null) {
            mDefaultCellFactories = new LinkedHashSet<>();
        }
        if (mDefaultCellFactories.isEmpty()) {
            mDefaultCellFactories.addAll(Arrays.asList(new TextCellFactory(),
                    new ThreePartImageCellFactory(mLayerClient, mPicasso),
                    new LocationCellFactory(mPicasso),
                    new SinglePartImageCellFactory(mLayerClient, mPicasso)));
        }

        return mDefaultCellFactories;
    }

    //==============================================================================================
    // UI update callbacks
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        syncInitialMessages(0, getItemCount());
        notifyDataSetChanged();

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryDataSetChanged");
        }
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        notifyItemChanged(position);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemChanged. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemRangeChanged. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        syncInitialMessages(position, 1);
        notifyItemInserted(position);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemInserted. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        syncInitialMessages(positionStart, itemCount);
        notifyItemRangeInserted(positionStart, itemCount);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemRangeInserted. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        notifyItemRemoved(position);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemRemoved. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart, itemCount);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemRangeRemoved. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemMoved. From: " + fromPosition + " To: " + toPosition);
        }
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // Layout to inflate
        public final static int RESOURCE_ID = R.layout.atlas_conversation_item;

        // View cache
        protected TextView mTitleView;
        protected AtlasAvatar mAvatarCluster;
        protected TextView mMessageView;
        protected TextView mTimeView;

        protected ConversationStyle conversationStyle;
        protected Conversation mConversation;
        protected OnClickListener mClickListener;

        public ViewHolder(View itemView, ConversationStyle conversationStyle) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            this.conversationStyle = conversationStyle;

            mAvatarCluster = (AtlasAvatar) itemView.findViewById(R.id.avatar);
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mMessageView = (TextView) itemView.findViewById(R.id.last_message);
            mTimeView = (TextView) itemView.findViewById(R.id.time);
            itemView.setBackgroundColor(conversationStyle.getCellBackgroundColor());
        }

        public void applyStyle(boolean unread) {
            mTitleView.setTextColor(unread ? conversationStyle.getTitleUnreadTextColor() : conversationStyle.getTitleTextColor());
            mTitleView.setTypeface(unread ? conversationStyle.getTitleUnreadTextTypeface() : conversationStyle.getTitleTextTypeface(), unread ? conversationStyle.getTitleUnreadTextStyle() : conversationStyle.getTitleTextStyle());
            mMessageView.setTextColor(unread ? conversationStyle.getSubtitleTextColor() : conversationStyle.getSubtitleTextColor());
            mMessageView.setTypeface(unread ? conversationStyle.getSubtitleUnreadTextTypeface() : conversationStyle.getSubtitleTextTypeface(), unread ? conversationStyle.getSubtitleUnreadTextStyle() : conversationStyle.getSubtitleTextStyle());
            mTimeView.setTextColor(conversationStyle.getDateTextColor());
            mTimeView.setTypeface(conversationStyle.getDateTextTypeface());
        }

        protected ViewHolder setClickListener(OnClickListener clickListener) {
            mClickListener = clickListener;
            return this;
        }

        public Conversation getConversation() {
            return mConversation;
        }

        public void setConversation(Conversation conversation) {
            mConversation = conversation;
        }

        @Override
        public void onClick(View v) {
            if (mClickListener == null) return;
            mClickListener.onClick(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mClickListener == null) return false;
            return mClickListener.onLongClick(this);
        }

        interface OnClickListener {
            void onClick(ViewHolder viewHolder);

            boolean onLongClick(ViewHolder viewHolder);
        }
    }

    /**
     * Listens for item clicks on an IntegrationConversationsAdapter.
     */
    public interface OnConversationClickListener {
        /**
         * Alerts the listener to item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item clicked.
         * @param conversation The item clicked.
         */
        void onConversationClick(AtlasConversationsAdapter adapter, Conversation conversation);

        /**
         * Alerts the listener to long item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item long-clicked.
         * @param conversation The item long-clicked.
         * @return true if the long-click was handled, false otherwise.
         */
        boolean onConversationLongClick(AtlasConversationsAdapter adapter, Conversation conversation);
    }
}