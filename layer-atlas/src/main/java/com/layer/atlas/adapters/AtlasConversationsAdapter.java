package com.layer.atlas.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
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
import com.layer.atlas.messagetypes.meeting.MeetingCreatedCellFactory;
import com.layer.atlas.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.atlas.participant.ChatParticipantProvider;
import com.layer.atlas.participant.Participant;
import com.layer.atlas.util.ConversationFormatter;
import com.layer.atlas.util.ConversationStyle;
import com.layer.atlas.util.IdentityRecyclerViewEventListener;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.CompoundPredicate;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class AtlasConversationsAdapter extends RecyclerView.Adapter<AtlasConversationsAdapter.ViewHolder> implements AtlasBaseAdapter<Conversation>, RecyclerViewController.Callback {
    protected final LayerClient mLayerClient;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Conversation> mQueryController;
    private final LayoutInflater mInflater;
    private long mInitialHistory = 0;

    private OnConversationClickListener mConversationClickListener;
    private ViewHolder.OnClickListener mViewHolderClickListener;

    private final DateFormat mDateFormat;
    private final DateFormat mTimeFormat;
    private ConversationStyle conversationStyle;
    private final IdentityRecyclerViewEventListener mIdentityEventListener;

    protected Set<AtlasCellFactory> mCellFactories;
    private Set<AtlasCellFactory> mDefaultCellFactories;

    protected ConversationFormatter mConversationFormatter;
    protected boolean mShouldShowAvatarPresence = true;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final ChatParticipantProvider chatParticipantProvider;

    public AtlasConversationsAdapter(Context context, LayerClient client, Picasso picasso, ConversationFormatter conversationFormatter, ChatParticipantProvider chatParticipantProvider) {
        this(context, client, picasso, null, conversationFormatter, chatParticipantProvider);
    }

    public AtlasConversationsAdapter(Context context, LayerClient client, Picasso picasso, Collection<String> updateAttributes, ConversationFormatter conversationFormatter, ChatParticipantProvider chatParticipantProvider) {
        this.chatParticipantProvider = chatParticipantProvider;
        mConversationFormatter = conversationFormatter;
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
            mCellFactories = new LinkedHashSet<>();
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
        compositeDisposable.clear();
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

    public void setConversationFormatter(ConversationFormatter mConversationFormatter) {
        this.mConversationFormatter = mConversationFormatter;
    }

    /**
     * @return If the Avatar for the other participant in a one on one conversation will be shown
     * or not. Defaults to `true`.
     */
    public boolean getShouldShowAvatarPresence() {
        return mShouldShowAvatarPresence;
    }

    /**
     * @param shouldShowPresence Whether the Avatar for the other participant in a one on one
     *                           conversation should be shown or not. Default is `true`.
     */
    public AtlasConversationsAdapter setShouldShowAvatarPresence(boolean shouldShowPresence) {
        mShouldShowAvatarPresence = shouldShowPresence;
        return this;
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
        ViewHolder viewHolder = new ViewHolder(mInflater.inflate(ViewHolder.RESOURCE_ID, parent, false), conversationStyle, mShouldShowAvatarPresence);
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
        Message lastMessage = conversation.getLastMessage();
        Context context = viewHolder.itemView.getContext();

        viewHolder.setConversation(conversation);
        final Set<Identity> identities = conversation.getParticipants();
        identities.remove(mLayerClient.getAuthenticatedUser());

        // Add the position to the positions map for Identity updates
        mIdentityEventListener.addIdentityPosition(position, identities);


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

        compositeDisposable.add(chatParticipantProvider
                .getParticipants(Util.getIdsFromIdentities(identities))
                .subscribe(new Consumer<List<Participant>>() {
                    @Override
                    public void accept(List<Participant> participants) throws Exception {
                        if (!participants.isEmpty()) {
                            setPresenceStatus(participants, identities);
                            viewHolder.mTitleView.setText(ConversationFormatter.getConversationTitle(participants, conversation));
                            if (participants.size() > 1) {
                                sortAvatars(participants, conversation, viewHolder);
                            } else {
                                viewHolder.mAvatarCluster.setParticipants(new LinkedHashSet<>(participants));
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("Error during participants downloading", throwable);
                    }
                }));
    }

    private void setPresenceStatus(List<Participant> participants, Set<Identity> identities) {
        for (Identity identity : identities) {
            Participant participant = findParticipant(identity.getUserId(), participants);
            if (participant != null && identity.getPresenceStatus() != null) {
                participant.setPresenceStatus(identity.getPresenceStatus());
            }
        }
    }

    @Nullable
    private Participant findParticipant(String userId, List<Participant> participants) {
        for (Participant participant : participants) {
            if (userId.equals(participant.getId())) {
                return participant;
            }
        }
        return null;
    }

    private void sortAvatars(final List<Participant> participants,
                             final Conversation conversation,
                             final ViewHolder viewHolder) {
        compositeDisposable.add(Observable.fromIterable(participants)
                .map(new Function<Participant, String>() {
                    @Override
                    public String apply(Participant identity) throws Exception {
                        return identity.getId();
                    }
                }).flatMap(new Function<String, ObservableSource<List<Message>>>() {
                    @Override
                    public ObservableSource<List<Message>> apply(String identityId) throws Exception {
                        return Observable.just(queryLastMessageForIdentity(conversation, identityId));
                    }
                }).filter(new io.reactivex.functions.Predicate<List<Message>>() {
                    @Override
                    public boolean test(List<Message> messages) throws Exception {
                        return messages.size() > 0;
                    }
                }).map(new Function<List<Message>, Message>() {
                    @Override
                    public Message apply(List<Message> messages) throws Exception {
                        return messages.get(0);
                    }
                }).sorted(new Comparator<Message>() {
                    @SuppressLint("NewApi")
                    @Override
                    public int compare(Message message1, Message message2) {
                        return Long.compare(message1.getPosition(), message2.getPosition());
                    }
                }).toList().map(new Function<List<Message>, Set<Participant>>() {
                    @Override
                    public Set<Participant> apply(List<Message> messages) throws Exception {
                        return sortIdentities(participants, messages);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Set<Participant>>() {
                    @Override
                    public void accept(Set<Participant> identities) throws Exception {
                        viewHolder.mAvatarCluster.setParticipants(identities);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private List<Message> queryLastMessageForIdentity(Conversation conversation, String identityId) {
        Query.Builder<Message> query = Query.builder(Message.class)
                .predicate(new CompoundPredicate(CompoundPredicate.Type.AND,
                        new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation),
                        new Predicate(Message.Property.SENDER_USER_ID, Predicate.Operator.EQUAL_TO, identityId)))
                .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.DESCENDING))
                .limit(1);

        return mLayerClient.executeQuery(query.build(), Query.ResultType.OBJECTS);
    }

    /**
     * Sort identities in order first identities without any messages(random order) and then sorted by last message sent.
     */
    private Set<Participant> sortIdentities(List<Participant> participants, List<Message> messages) {
        Map<String, Participant> participantsMap = Observable.fromIterable(participants)
                .toMap(new Function<Participant, String>() {
                    @Override
                    public String apply(Participant identity) throws Exception {
                        return identity.getId();
                    }
                }).blockingGet();
        List<Participant> sortedParticipants = new ArrayList<>(participants.size());
        for (Message sortedMessage : messages) {
            Identity sender = sortedMessage.getSender();
            String identityId = sender != null ? sender.getUserId() : null;
            if (identityId != null && participantsMap.containsKey(identityId)) {
                sortedParticipants.add(participantsMap.get(identityId));
                participantsMap.remove(identityId);
            }
        }
        Set<Participant> allParticipants = new LinkedHashSet<>(participants.size());
        allParticipants.addAll(participantsMap.values());
        allParticipants.addAll(sortedParticipants);

        return allParticipants;
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
                    new SinglePartImageCellFactory(mLayerClient, mPicasso),
                    new MeetingCreatedCellFactory(null)));
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

        public ViewHolder(View itemView, ConversationStyle conversationStyle, boolean shouldShowAvatarPresence) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            this.conversationStyle = conversationStyle;

            mAvatarCluster = itemView.findViewById(R.id.avatar);
            mTitleView = itemView.findViewById(R.id.title);
            mMessageView = itemView.findViewById(R.id.last_message);
            mTimeView = itemView.findViewById(R.id.time);
            itemView.setBackgroundColor(conversationStyle.getCellBackgroundColor());
            mAvatarCluster.setShouldShowPresence(shouldShowAvatarPresence);
            itemView.setBackgroundColor(conversationStyle.getCellBackgroundColor());
        }

        public void applyStyle(boolean unread) {
            mTitleView.setTextColor(unread ? conversationStyle.getTitleUnreadTextColor() : conversationStyle.getTitleTextColor());
            mTitleView.setTypeface(unread ? conversationStyle.getTitleUnreadTextTypeface() : conversationStyle.getTitleTextTypeface(), unread ? conversationStyle.getTitleUnreadTextStyle() : conversationStyle.getTitleTextStyle());
            mMessageView.setTextColor(unread ? conversationStyle.getSubtitleUnreadTextColor() : conversationStyle.getSubtitleTextColor());
            mMessageView.setTypeface(unread ? conversationStyle.getSubtitleUnreadTextTypeface() : conversationStyle.getSubtitleTextTypeface(), unread ? conversationStyle.getSubtitleUnreadTextStyle() : conversationStyle.getSubtitleTextStyle());
            mTimeView.setTextColor(unread ? conversationStyle.getDateUnreadTextColor() : conversationStyle.getDateTextColor());
            mTimeView.setTypeface(unread ? conversationStyle.getDateUnreadTextTypeface() : conversationStyle.getDateTextTypeface());
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