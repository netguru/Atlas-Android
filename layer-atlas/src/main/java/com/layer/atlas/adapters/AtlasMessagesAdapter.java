package com.layer.atlas.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.jakewharton.rxrelay2.PublishRelay;
import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.messagetypes.MessageStyle;
import com.layer.atlas.util.IdentityRecyclerViewEventListener;
import com.layer.atlas.util.Log;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.ListViewController;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AtlasMessagesAdapter drives an AtlasMessagesList.  The AtlasMessagesAdapter itself handles
 * rendering sender names, avatars, dates, left/right alignment, and message clustering, and leaves
 * rendering message content up to registered CellFactories.  Each CellFactory knows which Messages
 * it can render, can create new View hierarchies for its Message types, and can render (bind)
 * Message data with its created View hierarchies.  Typically, CellFactories are segregated by
 * MessagePart MIME types (e.g. "text/plain", "image/jpeg", and "application/vnd.geo+json").
 * <p>
 * Under the hood, the AtlasMessagesAdapter is a RecyclerView.Adapter, which automatically recycles
 * its list items within view-type "buckets".  Each registered CellFactory actually creates two such
 * view-types: one for cells sent by the authenticated user, and another for cells sent by remote
 * actors.  This allows the AtlasMessagesAdapter to efficiently render images sent by the current
 * user aligned on the left, and images sent by others aligned on the right, for example.  In case
 * this sent-by distinction is of value when rendering cells, it provided as the `isMe` argument.
 * <p>
 * When rendering Messages, the AtlasMessagesAdapter first determines which CellFactory to handle
 * the Message with calling CellFactory.isBindable() on each of its registered CellFactories. The
 * first CellFactory to return `true` is used for that Message.  Then, the adapter checks for
 * available CellHolders of that type.  If none are found, a new one is created with a call to
 * CellFactory.createCellHolder().  After creating a new CellHolder (or reusing an available one),
 * the CellHolder is rendered in the UI with Message data via CellFactory.bindCellHolder().
 *
 * @see AtlasCellFactory
 */
public class AtlasMessagesAdapter extends RecyclerView.Adapter<AtlasMessagesAdapter.ViewHolder> implements AtlasBaseAdapter<Message>, RecyclerViewController.Callback {
    private final static int VIEW_TYPE_FOOTER = 0;

    protected final LayerClient mLayerClient;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Message> mQueryController;
    protected final LayoutInflater mLayoutInflater;
    protected final Handler mUiThreadHandler;
    protected OnMessageAppendListener mAppendListener;
    protected final DisplayMetrics mDisplayMetrics;
    private final IdentityRecyclerViewEventListener mIdentityEventListener;

    // Cells
    protected int mViewTypeCount = VIEW_TYPE_FOOTER;
    protected final Set<AtlasCellFactory> mCellFactories = new LinkedHashSet<AtlasCellFactory>();
    protected final Map<Integer, CellType> mCellTypesByViewType = new HashMap<Integer, CellType>();
    protected final Map<AtlasCellFactory, Integer> mMyViewTypesByCell = new HashMap<AtlasCellFactory, Integer>();
    protected final Map<AtlasCellFactory, Integer> mTheirViewTypesByCell = new HashMap<AtlasCellFactory, Integer>();

    // Dates and Clustering
    private final Map<Uri, Cluster> mClusterCache = new HashMap<Uri, Cluster>();
    private final DateFormat mDateFormat;
    private final DateFormat mTimeFormat;

    private View mFooterView;
    private int mFooterPosition = 0;

    private Integer mRecipientStatusPosition;

    //Style
    private MessageStyle mMessageStyle;
    private RecyclerView mRecyclerView;
    private boolean mReadReceiptsEnabled = true;

    protected boolean mShouldShowAvatarInOneOnOneConversations;
    protected boolean mShouldShowAvatarPresence = true;
    @Nullable
    private PublishRelay<Identity> identityPublishRelay;

    public AtlasMessagesAdapter(Context context, LayerClient layerClient, Picasso picasso) {
        mLayerClient = layerClient;
        mPicasso = picasso;
        mLayoutInflater = LayoutInflater.from(context);
        mUiThreadHandler = new Handler(Looper.getMainLooper());
        mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        mDisplayMetrics = context.getResources().getDisplayMetrics();
        mQueryController = layerClient.newRecyclerViewController(null, null, this);
        mQueryController.setPreProcessCallback(new ListViewController.PreProcessCallback<Message>() {
            @Override
            public void onCache(ListViewController listViewController, Message message) {
                for (AtlasCellFactory factory : mCellFactories) {
                    if (factory.isBindable(message)) {
                        factory.getParsedContent(mLayerClient, message);
                        break;
                    }
                }
            }
        });

        setHasStableIds(false);

        mIdentityEventListener = new IdentityRecyclerViewEventListener(this);
        mLayerClient.registerEventListener(mIdentityEventListener);
    }

    /**
     * Sets this AtlasMessagesAdapter's Message Query.
     *
     * @param query Query drive this AtlasMessagesAdapter.
     * @return This AtlasMessagesAdapter.
     */
    public AtlasMessagesAdapter setQuery(Query<Message> query) {
        mQueryController.setQuery(query);
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
        mLayerClient.unregisterEventListener(mIdentityEventListener);
    }

    public AtlasMessagesAdapter setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        return this;
    }

    public void setStyle(MessageStyle messageStyle) {
        this.mMessageStyle = messageStyle;
    }

    public void setFooterView(View footerView) {
        boolean isNull = footerView == null;
        boolean wasNull = mFooterView == null;
        mFooterView = footerView;

        if (wasNull && !isNull) {
            // Insert
            notifyItemInserted(mFooterPosition);
        } else if (!wasNull && isNull) {
            // Delete
            notifyItemRemoved(mFooterPosition);
        } else if (!wasNull && !isNull) {
            // Change
            notifyItemChanged(mFooterPosition);
        }
    }

    public View getFooterView() {
        return mFooterView;
    }

    /**
     * @return If the Avatar for the other participant in a one on one conversation  will be shown
     * or not
     */
    public boolean getShouldShowAvatarInOneOnOneConversations() {
        return mShouldShowAvatarInOneOnOneConversations;
    }

    /**
     * @param shouldShowAvatarInOneOnOneConversations Whether the Avatar for the other participant
     *                                                in a one on one conversation should be shown
     *                                                or not
     */
    public void setShouldShowAvatarInOneOnOneConversations(boolean shouldShowAvatarInOneOnOneConversations) {
        mShouldShowAvatarInOneOnOneConversations = shouldShowAvatarInOneOnOneConversations;
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
    public AtlasMessagesAdapter setShouldShowAvatarPresence(boolean shouldShowPresence) {
        mShouldShowAvatarPresence = shouldShowPresence;
        return this;
    }

    /**
     * Set whether or not the conversation supports read receipts. This determines if the read
     * receipts should be shown in the view holders.
     *
     * @param readReceiptsEnabled true if the conversation is adapter is used for supports read receipts
     */
    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) {
        mReadReceiptsEnabled = readReceiptsEnabled;
    }


    //==============================================================================================
    // Listeners
    //==============================================================================================

    /**
     * Sets the OnAppendListener for this AtlasQueryAdapter.  The listener will be called when items
     * are appended to the end of this adapter.  This is useful for implementing a scroll-to-bottom
     * feature.
     *
     * @param listener The OnAppendListener to notify about appended items.
     * @return This AtlasQueryAdapter.
     */
    public AtlasMessagesAdapter setOnMessageAppendListener(OnMessageAppendListener listener) {
        mAppendListener = listener;
        return this;
    }


    //==============================================================================================
    // Adapter and Cells
    //==============================================================================================

    /**
     * Registers one or more CellFactories for the AtlasMessagesAdapter to manage.  CellFactories
     * know which Messages they can render, and handle View caching, creation, and binding.
     *
     * @param cellFactories Cells to register.
     * @return This AtlasMessagesAdapter.
     */
    public AtlasMessagesAdapter addCellFactories(AtlasCellFactory... cellFactories) {
        for (AtlasCellFactory cellFactory : cellFactories) {
            cellFactory.setStyle(mMessageStyle);
            mCellFactories.add(cellFactory);

            mViewTypeCount++;
            CellType me = new CellType(true, cellFactory);
            mCellTypesByViewType.put(mViewTypeCount, me);
            mMyViewTypesByCell.put(cellFactory, mViewTypeCount);

            mViewTypeCount++;
            CellType notMe = new CellType(false, cellFactory);
            mCellTypesByViewType.put(mViewTypeCount, notMe);
            mTheirViewTypesByCell.put(cellFactory, mViewTypeCount);
        }
        return this;
    }

    public Set<AtlasCellFactory> getCellFactories() {
        return mCellFactories;
    }

    @Override
    public int getItemViewType(int position) {
        if (mFooterView != null && position == mFooterPosition) return VIEW_TYPE_FOOTER;
        Message message = getItem(position);
        Identity authenticatedUser = mLayerClient.getAuthenticatedUser();
        boolean isMe = authenticatedUser != null && authenticatedUser.equals(message.getSender());
        for (AtlasCellFactory factory : mCellFactories) {
            if (!factory.isBindable(message)) continue;
            return isMe ? mMyViewTypesByCell.get(factory) : mTheirViewTypesByCell.get(factory);
        }
        return -1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return new ViewHolder(mLayoutInflater.inflate(ViewHolder.RESOURCE_ID_FOOTER, parent, false));
        }

        CellType cellType = mCellTypesByViewType.get(viewType);
        int rootResId = cellType.mMe ? CellViewHolder.RESOURCE_ID_ME : CellViewHolder.RESOURCE_ID_THEM;
        CellViewHolder rootViewHolder = new CellViewHolder(mLayoutInflater.inflate(rootResId, parent, false), mPicasso, mShouldShowAvatarPresence);
        rootViewHolder.mCellHolder = cellType.mCellFactory.createCellHolder(rootViewHolder.mCell, cellType.mMe, mLayoutInflater);
        rootViewHolder.mCellHolderSpecs = new AtlasCellFactory.CellHolderSpecs();
        return rootViewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        mQueryController.updateBoundPosition(position);
        if (mFooterView != null && position == mFooterPosition) {
            // Footer
            bindFooter(viewHolder);
        } else {
            // Cell
            bindCellViewHolder((CellViewHolder) viewHolder, position);
        }
    }

    public void bindFooter(ViewHolder viewHolder) {
        viewHolder.mRoot.removeAllViews();
        if (mFooterView.getParent() != null) {
            ((ViewGroup) mFooterView.getParent()).removeView(mFooterView);
        }
        viewHolder.mRoot.addView(mFooterView);
    }

    public void bindCellViewHolder(final CellViewHolder viewHolder, int position) {
        final Message message = getItem(position);
        viewHolder.mMessage = message;
        CellType cellType = mCellTypesByViewType.get(viewHolder.getItemViewType());
        boolean oneOnOne = message.getConversation().getParticipants().size() == 2;

        // Clustering and dates
        Cluster cluster = getClustering(message, position);
        if (cluster.mClusterWithPrevious == null) {
            // No previous message, so no gap
            viewHolder.mClusterSpaceGap.setVisibility(View.GONE);
            bindDateTimeForMessage(viewHolder, message);
        } else if (cluster.mDateBoundaryWithPrevious || cluster.mClusterWithPrevious == ClusterType.MORE_THAN_HOUR) {
            // Crossed into a new day, or > 1hr lull in conversation
            bindDateTimeForMessage(viewHolder, message);
            viewHolder.mClusterSpaceGap.setVisibility(View.GONE);
        } else if (cluster.mClusterWithPrevious == ClusterType.LESS_THAN_MINUTE) {
            // Same sender with < 1m gap
            viewHolder.mClusterSpaceGap.setVisibility(View.GONE);
            viewHolder.mTimeGroup.setVisibility(View.GONE);
        } else if (cluster.mClusterWithPrevious == ClusterType.NEW_SENDER || cluster.mClusterWithPrevious == ClusterType.LESS_THAN_HOUR) {
            // New sender or > 1m gap
            viewHolder.mClusterSpaceGap.setVisibility(View.VISIBLE);
            viewHolder.mTimeGroup.setVisibility(View.GONE);
        }

        // Sender-dependent elements
        if (cellType.mMe) {
            updateViewHolderForRecipientStatus(viewHolder, position, message);

            // Unsent and sent
            if (!message.isSent()) {
                viewHolder.mCell.setAlpha(0.5f);
            } else {
                viewHolder.mCell.setAlpha(1.0f);
            }
        } else {
            if (mReadReceiptsEnabled) {
                message.markAsRead();
            }
            // Sender name, only for first message in cluster
            if (!oneOnOne && (cluster.mClusterWithPrevious == null || cluster.mClusterWithPrevious == ClusterType.NEW_SENDER)) {
                Identity sender = message.getSender();
                if (sender != null) {
                    viewHolder.mUserName.setText(Util.getDisplayName(sender));
                } else {
                    viewHolder.mUserName.setText(R.string.atlas_message_item_unknown_user);
                }
                viewHolder.mUserName.setVisibility(View.VISIBLE);

                // Add the position to the positions map for Identity updates
                mIdentityEventListener.addIdentityPosition(position, Collections.singleton(sender));
            } else {
                viewHolder.mUserName.setVisibility(View.GONE);
            }

            // Avatars
            if (oneOnOne) {
                if (mShouldShowAvatarInOneOnOneConversations) {
                    viewHolder.mAvatar.setVisibility(View.VISIBLE);
                    viewHolder.mAvatar.setParticipants(message.getSender());

                } else {
                    viewHolder.mAvatar.setVisibility(View.GONE);
                }
            } else if (cluster.mClusterWithNext == null || cluster.mClusterWithNext != ClusterType.LESS_THAN_MINUTE) {
                // Last message in cluster
                viewHolder.mAvatar.setVisibility(View.VISIBLE);
                viewHolder.mAvatar.setParticipants(message.getSender());
                viewHolder.mAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (identityPublishRelay != null && message.getSender() != null) {
                            identityPublishRelay.accept(message.getSender());
                        }
                    }
                });
                // Add the position to the positions map for Identity updates
                mIdentityEventListener.addIdentityPosition(position, Collections.singleton(message.getSender()));
            } else {
                // Invisible for clustered messages to preserve proper spacing
                viewHolder.mAvatar.setVisibility(View.INVISIBLE);
            }
        }

        // CellHolder
        AtlasCellFactory.CellHolder cellHolder = viewHolder.mCellHolder;
        cellHolder.setMessage(message);

        // Cell dimensions
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.mCell.getLayoutParams();
        int maxWidth = mRecyclerView.getWidth() - viewHolder.mRoot.getPaddingLeft() - viewHolder.mRoot.getPaddingRight() - params.leftMargin - params.rightMargin;
        if (!oneOnOne && !cellType.mMe) {
            // Subtract off avatar width if needed
            ViewGroup.MarginLayoutParams avatarParams = (ViewGroup.MarginLayoutParams) viewHolder.mAvatar.getLayoutParams();
            maxWidth -= avatarParams.width + avatarParams.rightMargin + avatarParams.leftMargin;
        }
        // TODO: subtract spacing rather than multiply by 0.8 to handle screen sizes more cleanly
        int maxHeight = (int) viewHolder.mRoot.getContext().getResources().getDimension(R.dimen.atlas_messages_max_cell_height);

        viewHolder.mCellHolderSpecs.isMe = cellType.mMe;
        viewHolder.mCellHolderSpecs.position = position;
        viewHolder.mCellHolderSpecs.maxWidth = maxWidth;
        viewHolder.mCellHolderSpecs.maxHeight = maxHeight;
        cellType.mCellFactory.bindCellHolder(cellHolder, cellType.mCellFactory.getParsedContent(mLayerClient, message), message, viewHolder.mCellHolderSpecs);
    }

    public void setIdentityPublishRelay(PublishRelay<Identity> identityPublishRelay) {
        this.identityPublishRelay = identityPublishRelay;
    }

    private void updateViewHolderForRecipientStatus(CellViewHolder viewHolder, int position, Message message) {
        if (mReadReceiptsEnabled && mRecipientStatusPosition != null && mRecipientStatusPosition == position) {
            int readCount = 0;
            boolean delivered = false;
            Map<Identity, Message.RecipientStatus> statuses = message.getRecipientStatus();
            for (Map.Entry<Identity, Message.RecipientStatus> entry : statuses.entrySet()) {
                // Only show receipts for other members
                if (entry.getKey().equals(mLayerClient.getAuthenticatedUser())) continue;
                // Skip receipts for members no longer in the conversation
                if (entry.getValue() == null) continue;

                switch (entry.getValue()) {
                    case READ:
                        readCount++;
                        break;
                    case DELIVERED:
                        delivered = true;
                        break;
                }
            }
            if (readCount > 0) {
                viewHolder.mReceipt.setVisibility(View.VISIBLE);
                // Use 2 to include one other participant plus the current user
                if (statuses.size() > 2) {
                    String quantityString = viewHolder.mReceipt.getResources()
                            .getQuantityString(R.plurals.atlas_message_item_read_muliple_participants, readCount, readCount);
                    viewHolder.mReceipt.setText(quantityString);
                } else {
                    viewHolder.mReceipt.setText(R.string.atlas_message_item_read);
                }
            } else if (delivered) {
                viewHolder.mReceipt.setVisibility(View.VISIBLE);
                viewHolder.mReceipt.setText(R.string.atlas_message_item_delivered);
            } else {
                viewHolder.mReceipt.setVisibility(View.GONE);
            }
        } else {
            viewHolder.mReceipt.setVisibility(View.GONE);
        }
    }

    private void bindDateTimeForMessage(CellViewHolder viewHolder, Message message) {
        Date receivedAt = message.getReceivedAt();
        if (receivedAt == null) receivedAt = new Date();
        String timeBarDayText = Util.formatTimeDay(viewHolder.mCell.getContext(), receivedAt);
        viewHolder.mTimeGroupDay.setText(timeBarDayText);
        String timeBarTimeText = mTimeFormat.format(receivedAt.getTime());
        viewHolder.mTimeGroupTime.setText(" " + timeBarTimeText);
        viewHolder.mTimeGroup.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount() + ((mFooterView == null) ? 0 : 1);
    }

    @Override
    public Integer getPosition(Message message) {
        return mQueryController.getPosition(message);
    }

    @Override
    public Integer getPosition(Message message, int lastPosition) {
        return mQueryController.getPosition(message, lastPosition);
    }

    @Override
    public Message getItem(int position) {
        if (mFooterView != null && position == mFooterPosition) return null;
        return mQueryController.getItem(position);
    }

    @Override
    public Message getItem(RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof CellViewHolder)) return null;
        return ((CellViewHolder) viewHolder).mMessage;
    }

    //==============================================================================================
    // Clustering
    //==============================================================================================

    // TODO: optimize by limiting search to positions in- and around- visible range
    private Cluster getClustering(Message message, int position) {
        Cluster result = mClusterCache.get(message.getId());
        if (result == null) {
            result = new Cluster();
            mClusterCache.put(message.getId(), result);
        }

        int previousPosition = position - 1;
        Message previousMessage = (previousPosition >= 0) ? getItem(previousPosition) : null;
        if (previousMessage != null) {
            result.mDateBoundaryWithPrevious = isDateBoundary(previousMessage.getReceivedAt(), message.getReceivedAt());
            result.mClusterWithPrevious = ClusterType.fromMessages(previousMessage, message);

            Cluster previousCluster = mClusterCache.get(previousMessage.getId());
            if (previousCluster == null) {
                previousCluster = new Cluster();
                mClusterCache.put(previousMessage.getId(), previousCluster);
            } else {
                // does the previous need to change its clustering?
                if ((previousCluster.mClusterWithNext != result.mClusterWithPrevious) ||
                        (previousCluster.mDateBoundaryWithNext != result.mDateBoundaryWithPrevious)) {
                    requestUpdate(previousMessage, previousPosition);
                }
            }
            previousCluster.mClusterWithNext = result.mClusterWithPrevious;
            previousCluster.mDateBoundaryWithNext = result.mDateBoundaryWithPrevious;
        }

        int nextPosition = position + 1;
        Message nextMessage = (nextPosition < getItemCount()) ? getItem(nextPosition) : null;
        if (nextMessage != null) {
            result.mDateBoundaryWithNext = isDateBoundary(message.getReceivedAt(), nextMessage.getReceivedAt());
            result.mClusterWithNext = ClusterType.fromMessages(message, nextMessage);

            Cluster nextCluster = mClusterCache.get(nextMessage.getId());
            if (nextCluster == null) {
                nextCluster = new Cluster();
                mClusterCache.put(nextMessage.getId(), nextCluster);
            } else {
                // does the next need to change its clustering?
                if ((nextCluster.mClusterWithPrevious != result.mClusterWithNext) ||
                        (nextCluster.mDateBoundaryWithPrevious != result.mDateBoundaryWithNext)) {
                    requestUpdate(nextMessage, nextPosition);
                }
            }
            nextCluster.mClusterWithPrevious = result.mClusterWithNext;
            nextCluster.mDateBoundaryWithPrevious = result.mDateBoundaryWithNext;
        }

        return result;
    }

    private static boolean isDateBoundary(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        return (d1.getYear() != d2.getYear()) || (d1.getMonth() != d2.getMonth()) || (d1.getDay() != d2.getDay());
    }

    private void requestUpdate(final Message message, final int lastPosition) {
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(getPosition(message, lastPosition));
            }
        });
    }


    //==============================================================================================
    // Read and delivery receipts
    //==============================================================================================

    private void updateRecipientStatusPosition() {
        if (mReadReceiptsEnabled) {
            Integer oldPosition = mRecipientStatusPosition;
            // Set new position to last in the list
            mRecipientStatusPosition = mQueryController.getItemCount() - 1;
            if (oldPosition != null) {
                notifyItemChanged(oldPosition);
            }
        }
    }


    //==============================================================================================
    // UI update callbacks
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        mFooterPosition = mQueryController.getItemCount();
        updateRecipientStatusPosition();
        notifyDataSetChanged();

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryDataSetChanged");
        }
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        notifyItemChanged(position);

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemChanged. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemRangeChanged. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        mFooterPosition++;
        updateRecipientStatusPosition();
        notifyItemInserted(position);
        if (mAppendListener != null && (position + 1) == getItemCount()) {
            mAppendListener.onMessageAppend(this, getItem(position));
        }

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemInserted. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        mFooterPosition += itemCount;
        updateRecipientStatusPosition();
        notifyItemRangeInserted(positionStart, itemCount);
        int positionEnd = positionStart + itemCount;
        if (mAppendListener != null && (positionEnd + 1) == getItemCount()) {
            mAppendListener.onMessageAppend(this, getItem(positionEnd));
        }

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemRangeInserted. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        mFooterPosition--;
        updateRecipientStatusPosition();
        notifyItemRemoved(position);

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemRemoved. Position: " + position);
        }
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        mFooterPosition -= itemCount;
        updateRecipientStatusPosition();
        notifyItemRangeRemoved(positionStart, itemCount);

        if (Log.isPerfLoggable()) {
            Log.perf("Messages adapter - onQueryItemRangeRemoved. Position start: " + positionStart + " Count: " + itemCount);
        }
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        updateRecipientStatusPosition();
        notifyItemMoved(fromPosition, toPosition);

        if (Log.isPerfLoggable()) {
            Log.perf("Conversations adapter - onQueryItemMoved. From: " + fromPosition + " To: " + toPosition);
        }
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final static int RESOURCE_ID_FOOTER = R.layout.atlas_message_item_footer;

        // View cache
        protected ViewGroup mRoot;

        public ViewHolder(View itemView) {
            super(itemView);
            mRoot = (ViewGroup) itemView.findViewById(R.id.swipeable);
        }
    }

    static class CellViewHolder extends ViewHolder {
        public final static int RESOURCE_ID_ME = R.layout.atlas_message_item_me;
        public final static int RESOURCE_ID_THEM = R.layout.atlas_message_item_them;

        protected Message mMessage;

        // View cache
        protected TextView mUserName;
        protected View mTimeGroup;
        protected TextView mTimeGroupDay;
        protected TextView mTimeGroupTime;
        protected Space mClusterSpaceGap;
        protected AtlasAvatar mAvatar;
        protected ViewGroup mCell;
        protected TextView mReceipt;

        // Cell
        protected AtlasCellFactory.CellHolder mCellHolder;
        protected AtlasCellFactory.CellHolderSpecs mCellHolderSpecs;

        public CellViewHolder(View itemView, Picasso picasso, boolean shouldShowAvatarPresence) {
            super(itemView);
            mUserName = (TextView) itemView.findViewById(R.id.sender);
            mTimeGroup = itemView.findViewById(R.id.time_group);
            mTimeGroupDay = (TextView) itemView.findViewById(R.id.time_group_day);
            mTimeGroupTime = (TextView) itemView.findViewById(R.id.time_group_time);
            mClusterSpaceGap = (Space) itemView.findViewById(R.id.cluster_space);
            mCell = (ViewGroup) itemView.findViewById(R.id.cell);
            mReceipt = (TextView) itemView.findViewById(R.id.receipt);

            mAvatar = ((AtlasAvatar) itemView.findViewById(R.id.avatar));
            if (mAvatar != null) {
                mAvatar.init(picasso);
                mAvatar.setShouldShowPresence(shouldShowAvatarPresence);
            }
        }
    }

    private enum ClusterType {
        NEW_SENDER,
        LESS_THAN_MINUTE,
        LESS_THAN_HOUR,
        MORE_THAN_HOUR;

        private static final long MILLIS_MINUTE = 60 * 1000;
        private static final long MILLIS_HOUR = 60 * MILLIS_MINUTE;

        public static ClusterType fromMessages(Message older, Message newer) {
            // Different users?
            if (!older.getSender().equals(newer.getSender())) return NEW_SENDER;

            // Time clustering for same user?
            Date oldReceivedAt = older.getReceivedAt();
            Date newReceivedAt = newer.getReceivedAt();
            if (oldReceivedAt == null || newReceivedAt == null) return LESS_THAN_MINUTE;
            long delta = Math.abs(newReceivedAt.getTime() - oldReceivedAt.getTime());
            if (delta <= MILLIS_MINUTE) return LESS_THAN_MINUTE;
            if (delta <= MILLIS_HOUR) return LESS_THAN_HOUR;
            return MORE_THAN_HOUR;
        }
    }

    private static class Cluster {
        public boolean mDateBoundaryWithPrevious;
        public ClusterType mClusterWithPrevious;

        public boolean mDateBoundaryWithNext;
        public ClusterType mClusterWithNext;
    }

    private static class MessagePosition {
        public Message mMessage;
        public int mPosition;

        public MessagePosition(Message message, int position) {
            mMessage = message;
            mPosition = position;
        }
    }

    private static class CellType {
        protected final boolean mMe;
        protected final AtlasCellFactory mCellFactory;

        public CellType(boolean me, AtlasCellFactory CellFactory) {
            mMe = me;
            mCellFactory = CellFactory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CellType cellType = (CellType) o;

            if (mMe != cellType.mMe) return false;
            return mCellFactory.equals(cellType.mCellFactory);

        }

        @Override
        public int hashCode() {
            int result = (mMe ? 1 : 0);
            result = 31 * result + mCellFactory.hashCode();
            return result;
        }
    }

    /**
     * Listens for inserts to the end of an AtlasQueryAdapter.
     */
    public interface OnMessageAppendListener {
        /**
         * Alerts the listener to inserts at the end of an AtlasQueryAdapter.  If a batch of items
         * were appended, only the last one will be alerted here.
         *
         * @param adapter The AtlasQueryAdapter which had an item appended.
         * @param message The item appended to the AtlasQueryAdapter.
         */
        void onMessageAppend(AtlasMessagesAdapter adapter, Message message);
    }
}