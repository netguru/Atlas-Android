package com.layer.atlas;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.layer.atlas.participant.ChatParticipantProvider;
import com.layer.atlas.participant.Participant;
import com.layer.atlas.util.AvatarStyle;
import com.layer.atlas.util.EditTextUtil;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.views.EmptyDelEditText;
import com.layer.atlas.util.views.FlowLayout;
import com.layer.atlas.util.views.MaxHeightScrollView;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.query.CompoundPredicate;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AtlasAddressBar extends LinearLayout {
    public static final int MAX_PARTICIPANTS = 25;

    private LayerClient mLayerClient;
    private Picasso mPicasso;

    private OnConversationClickListener mOnConversationClickListener;
    private OnParticipantSelectionChangeListener mOnParticipantSelectionChangeListener;
    private OnParticipantSelectionFailedListener mOnParticipantSelectionFailedListener;

    private FlowLayout mSelectedParticipantLayout;
    private EmptyDelEditText mFilter;
    private RecyclerView mParticipantList;
    private AvailableConversationAdapter mAvailableConversationAdapter;
    private final Set<Participant> mSelectedParticipants = new LinkedHashSet<>();

    private TextWatcher mTextWatcher;

    private boolean mShowConversations;

    // styles
    private int mInputTextSize;
    private int mInputTextColor;
    private Typeface mInputTextTypeface;
    private int mInputTextStyle;
    private int mInputUnderlineColor;
    private int mInputCursorColor;
    private int mListTextSize;
    private int mListTextColor;
    private Typeface mListTextTypeface;
    private int mListTextStyle;
    private Typeface mChipTypeface;
    private int mChipStyle;
    private AvatarStyle mAvatarStyle;
    private int backgroundColor;

    private final static String TAG = AtlasAddressBar.class.getSimpleName();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable textChangeDisposable = Disposables.disposed();
    private ChatParticipantProvider chatParticipantProvider;

    public AtlasAddressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasAddressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseStyle(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.atlas_address_bar, this, true);
        mSelectedParticipantLayout = (FlowLayout) findViewById(R.id.selected_participant_group);
        mFilter = (EmptyDelEditText) findViewById(R.id.filter);
        mSelectedParticipantLayout.setStretchChild(mFilter);
        mParticipantList = (RecyclerView) findViewById(R.id.participant_list);
        ((MaxHeightScrollView) findViewById(R.id.selected_participant_scroll)).setMaximumHeight(getResources().getDimensionPixelSize(R.dimen.atlas_selected_participant_group_max_height));
        setOrientation(VERTICAL);
    }

    public AtlasAddressBar init(LayerClient layerClient, Picasso picasso, ChatParticipantProvider chatParticipantProvider) {
        mLayerClient = layerClient;
        mPicasso = picasso;
        this.chatParticipantProvider = chatParticipantProvider;

        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mParticipantList.setLayoutManager(manager);
        mAvailableConversationAdapter = new AvailableConversationAdapter(mLayerClient, mPicasso, chatParticipantProvider);

        applyStyle();

        mParticipantList.setAdapter(mAvailableConversationAdapter);

        // Hitting backspace with an empty search string deletes the last selected participant
        mFilter.setOnEmptyDelListener(new EmptyDelEditText.OnEmptyDelListener() {
            @Override
            public boolean onEmptyDel(EmptyDelEditText editText) {
                removeLastSelectedParticipant();
                return true;
            }
        });

        // Refresh available participants and conversations with every search string change
        if (textChangeDisposable.isDisposed()) {
            textChangeDisposable = RxTextView.textChanges(mFilter)
                    .debounce(500, TimeUnit.MILLISECONDS)
                    .subscribe(new Consumer<CharSequence>() {
                        @Override
                        public void accept(CharSequence charSequence) throws Exception {
                            refresh();
                        }
                    });
        }

        return this;
    }

    /**
     * Call this method to prevent memory leaks.
     */
    public void onDestroy() {
        compositeDisposable.clear();
        textChangeDisposable.dispose();
        if (mAvailableConversationAdapter != null) {
            mAvailableConversationAdapter.destroy();
        }
    }

    public AtlasAddressBar addTextChangedListener(TextWatcher textWatcher){
        return addTextChangedListener(textWatcher, true);
    }

    public AtlasAddressBar addTextChangedListener(TextWatcher textWatcher, boolean forceOne) {
        if(mTextWatcher!=null && forceOne)
            removeTextChangedListener(mTextWatcher);
        mFilter.addTextChangedListener(textWatcher);
        this.mTextWatcher = textWatcher;
        return this;
    }

    public AtlasAddressBar removeTextChangedListener(TextWatcher textWatcher) {
        mFilter.removeTextChangedListener(textWatcher);
        return this;
    }

    public AtlasAddressBar setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        mFilter.setOnEditorActionListener(listener);
        return this;
    }

    public AtlasAddressBar setOnConversationClickListener(OnConversationClickListener onConversationClickListener) {
        mOnConversationClickListener = onConversationClickListener;
        return this;
    }

    public AtlasAddressBar setOnParticipantSelectionChangeListener(OnParticipantSelectionChangeListener onParticipantSelectionChangeListener) {
        mOnParticipantSelectionChangeListener = onParticipantSelectionChangeListener;
        return this;
    }

    public AtlasAddressBar setOnParticipantSelectionFailedListener(OnParticipantSelectionFailedListener onParticipantSelectionFailedListener) {
        this.mOnParticipantSelectionFailedListener = onParticipantSelectionFailedListener;
        return this;
    }

    public AtlasAddressBar setSuggestionsVisibility(int visibility) {
        mParticipantList.setVisibility(visibility);
        return this;
    }

    public AtlasAddressBar setTypeface(Typeface inputTextTypeface, Typeface listTextTypeface,
                                       Typeface avatarTextTypeface, Typeface chipTypeface) {
        this.mInputTextTypeface = inputTextTypeface;
        this.mListTextTypeface = listTextTypeface;
        this.mChipTypeface = chipTypeface;
        this.mAvatarStyle.setAvatarTextTypeface(avatarTextTypeface);
        applyTypeface();
        return this;
    }

    public Set<String> getSelectedParticipantIds() {
        return generateIds(mSelectedParticipants);
    }

    public Set<Participant> getSelectedParticipants() {
        return new LinkedHashSet<>(mSelectedParticipants);
    }

    public int getListTextSize() {
        return mListTextSize;
    }

    public int getListTextColor() {
        return mListTextColor;
    }

    public Typeface getListTextTypeface() {
        return mListTextTypeface;
    }

    public int getListTextStyle() {
        return mListTextStyle;
    }

    public AvatarStyle getAvatarStyle() {
        return mAvatarStyle;
    }

    public EmptyDelEditText getFilter() {
        return mFilter;
    }

    public AtlasAddressBar refresh() {
        return refresh(null);
    }

    private AtlasAddressBar refresh(@Nullable String filter) {
        if (mAvailableConversationAdapter == null) {
            return this;
        }
        if (filter == null) {
            filter = getSearchFilter();
        }
        mAvailableConversationAdapter.refresh(filter, mSelectedParticipants);
        return this;
    }

    public AtlasAddressBar setShowConversations(boolean showConversations) {
        this.mShowConversations = showConversations;
        return this;
    }

    public void requestFilterFocus() {
        mFilter.requestFocus();
    }

    private void selectParticipant(Participant participant) {
        selectParticipant(participant, false);
    }

    public AtlasAddressBar setSelectedParticipants(Set<String> selectedParticipants) {
        compositeDisposable.add(chatParticipantProvider.getParticipants(new ArrayList<>(selectedParticipants)).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Consumer<List<Participant>>() {
                    @Override
                    public void accept(List<Participant> participants) throws Exception {
                        mSelectedParticipants.addAll(participants);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, "Error during getting participants", throwable);

                    }
                }));
        return this;
    }

    public void selectParticipant(Participant participant, boolean skipRefresh) {
        if (mSelectedParticipants.contains(participant)) {
            return;
        }
        if (mSelectedParticipants.size() >= MAX_PARTICIPANTS) {
            if (this.mOnParticipantSelectionFailedListener != null) {
                this.mOnParticipantSelectionFailedListener.onMaxParticipantLimitExceeded();
            } else {
                Toast.makeText(this.getContext(),
                        String.format("Exceeded maximum permissible participants(%d)", MAX_PARTICIPANTS),
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        mSelectedParticipants.add(participant);
        ParticipantChip chip = new ParticipantChip(getContext(), participant, mPicasso);
        mSelectedParticipantLayout.addView(chip, mSelectedParticipantLayout.getChildCount() - 1);
        mFilter.setText(null);
        if (!skipRefresh) {
            refresh();
        }
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<>(generateIds(mSelectedParticipants)));
        }
    }

    private void unselectParticipant(ParticipantChip chip) {
        if (!mSelectedParticipants.contains(chip.mParticipant)) {
            return;
        }
        mSelectedParticipants.remove(chip.mParticipant);
        mSelectedParticipantLayout.removeView(chip);
        refresh();
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<>(generateIds(mSelectedParticipants)));
        }
    }

    private String getSearchFilter() {
        String s = mFilter.getText().toString();
        return s.trim().isEmpty() ? null : s;
    }

    private String removeLastSelectedParticipant() {
        ParticipantChip lastChip = null;
        for (int i = 0; i < mSelectedParticipantLayout.getChildCount(); i++) {
            View v = mSelectedParticipantLayout.getChildAt(i);
            if (v instanceof ParticipantChip) lastChip = (ParticipantChip) v;
        }
        if (lastChip == null) return null;
        unselectParticipant(lastChip);
        return lastChip.mParticipant.getId();
    }

    private void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasAddressBar, R.attr.AtlasAddressBar, defStyle);
        Resources resources = context.getResources();
        this.mInputTextSize = ta.getDimensionPixelSize(R.styleable.AtlasAddressBar_inputTextSize, resources.getDimensionPixelSize(R.dimen.atlas_text_size_input));
        this.mInputTextColor = ta.getColor(R.styleable.AtlasAddressBar_inputTextColor, resources.getColor(R.color.atlas_text_black));
        this.mInputTextStyle = ta.getInt(R.styleable.AtlasAddressBar_inputTextStyle, Typeface.NORMAL);
        String inputTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_inputTextTypeface);
        this.mInputTextTypeface = inputTextTypefaceName != null ? Typeface.create(inputTextTypefaceName, mInputTextStyle) : null;
        this.mInputCursorColor = ta.getColor(R.styleable.AtlasAddressBar_inputCursorColor, resources.getColor(R.color.atlas_color_primary_blue));
        this.mInputUnderlineColor = ta.getColor(R.styleable.AtlasAddressBar_inputUnderlineColor, resources.getColor(R.color.atlas_color_primary_blue));

        this.mListTextSize = ta.getDimensionPixelSize(R.styleable.AtlasAddressBar_listTextSize, resources.getDimensionPixelSize(R.dimen.atlas_text_size_secondary_item));
        this.mListTextColor = ta.getColor(R.styleable.AtlasAddressBar_listTextColor, resources.getColor(R.color.atlas_text_black));
        this.mListTextStyle = ta.getInt(R.styleable.AtlasAddressBar_listTextStyle, Typeface.NORMAL);
        String listTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_listTextTypeface);
        this.mListTextTypeface = listTextTypefaceName != null ? Typeface.create(listTextTypefaceName, mInputTextStyle) : null;

        this.mChipStyle = ta.getInt(R.styleable.AtlasAddressBar_chipStyle, Typeface.NORMAL);
        String chipTypefaceName = ta.getString(R.styleable.AtlasAddressBar_chipTypeface);
        this.mChipTypeface = chipTypefaceName != null ? Typeface.create(chipTypefaceName, mChipStyle) : null;

        this.backgroundColor = ta.getColor(R.styleable.AtlasAddressBar_backgroundColor, Color.TRANSPARENT);

        AvatarStyle.Builder avatarStyleBuilder = new AvatarStyle.Builder();
        avatarStyleBuilder.avatarBackgroundColor(ta.getColor(R.styleable.AtlasAddressBar_avatarBackgroundColor, resources.getColor(R.color.atlas_avatar_background)));
        avatarStyleBuilder.avatarTextColor(ta.getColor(R.styleable.AtlasAddressBar_avatarTextColor, resources.getColor(R.color.atlas_avatar_text)));
        avatarStyleBuilder.avatarBorderColor(ta.getColor(R.styleable.AtlasAddressBar_avatarBorderColor, resources.getColor(R.color.atlas_avatar_border)));
        int avatarTextStyle = ta.getInt(R.styleable.AtlasAddressBar_avatarTextStyle, Typeface.NORMAL);
        String avatarTextTypefaceName = ta.getString(R.styleable.AtlasAddressBar_avatarTextTypeface);
        avatarStyleBuilder.avatarTextTypeface(inputTextTypefaceName != null ? Typeface.create(avatarTextTypefaceName, avatarTextStyle) : null);
        this.mAvatarStyle = avatarStyleBuilder.build();

        ta.recycle();
    }

    private void applyStyle() {
        mSelectedParticipantLayout.setBackgroundColor(backgroundColor);
        mFilter.setTextColor(mInputTextColor);
        mFilter.setTextSize(TypedValue.COMPLEX_UNIT_PX, mInputTextSize);
        EditTextUtil.setCursorDrawableColor(mFilter, mInputCursorColor);
        EditTextUtil.setUnderlineColor(mFilter, mInputUnderlineColor);
        applyTypeface();
    }

    private void applyTypeface() {
        mFilter.setTypeface(mInputTextTypeface, mInputTextStyle);
        mAvailableConversationAdapter.notifyDataSetChanged();
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

    /**
     * Save selected participants
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mSelectedParticipants.isEmpty()) {
            return superState;
        }
        SavedState savedState = new SavedState(superState);
        savedState.mSelectedParticipants = new ArrayList<>(mSelectedParticipants);
        return savedState;
    }

    /**
     * Restore selected participants
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        if (savedState.mSelectedParticipants != null) {
            mSelectedParticipants.clear();
            for (Participant participant : savedState.mSelectedParticipants) {
                selectParticipant(participant);
            }
        }
    }

    private static class SavedState extends BaseSavedState {
        List<Participant> mSelectedParticipants;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            if (in.readByte() == 0x01) {
                mSelectedParticipants = new ArrayList<>();
                in.readList(mSelectedParticipants, Participant.class.getClassLoader());
            } else {
                mSelectedParticipants = null;
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            if (mSelectedParticipants == null) {
                dest.writeByte((byte) (0x00));
            } else {
                dest.writeByte((byte) (0x01));
                dest.writeList(mSelectedParticipants);
            }
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * ParticipantChip implements the View used to populate the selected participant FlowLayout.
     */
    private class ParticipantChip extends LinearLayout {
        private Participant mParticipant;

        private AtlasAvatar mAvatar;
        private TextView mName;
        private ImageView mRemove;

        public ParticipantChip(Context context, Participant participant, Picasso picasso) {
            super(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            Resources r = getContext().getResources();
            mParticipant = participant;

            // Inflate and cache views
            inflater.inflate(R.layout.atlas_participant_chip, this, true);
            mAvatar = (AtlasAvatar) findViewById(R.id.avatar);
            mName = (TextView) findViewById(R.id.name);
            mRemove = (ImageView) findViewById(R.id.remove);

            // Set Style
            mName.setTypeface(mChipTypeface);

            // Set layout
            int height = r.getDimensionPixelSize(R.dimen.atlas_chip_height);
            int margin = r.getDimensionPixelSize(R.dimen.atlas_chip_margin);
            FlowLayout.LayoutParams p = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            p.setMargins(margin, margin, margin, margin);
            setLayoutParams(p);
            setOrientation(HORIZONTAL);
            setBackgroundDrawable(r.getDrawable(R.drawable.atlas_participant_chip_background));

            // Initialize participant data
            mName.setText(mParticipant.getName());
            mAvatar.init(picasso)
                    .setStyle(mAvatarStyle)
                    .setParticipants(participant);

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    unselectParticipant(ParticipantChip.this);
                }
            });
        }
    }

    private enum Type {
        PARTICIPANT,
        CONVERSATION
    }

    private Set<String> generateIds(Set<Participant> participants) {
        Set<String> participantIds = new LinkedHashSet<>();
        for (Participant participant : participants) {
            participantIds.add(participant.getId());
        }
        return participantIds;
    }

    /**
     * AvailableConversationAdapter provides items for individual Participants and existing
     * Conversations.  Items are filtered by a participant filter string and by a set of selected
     * Participants.
     */
    private class AvailableConversationAdapter extends RecyclerView.Adapter<AvailableConversationAdapter.ViewHolder> implements RecyclerViewController.Callback {
        private final String TAG = AvailableConversationAdapter.class.getSimpleName();
        private final CompositeDisposable compositeDisposable = new CompositeDisposable();
        private final LayerClient mLayerClient;
        private final Picasso mPicasso;
        private final ChatParticipantProvider chatParticipantProvider;
        private final RecyclerViewController<Conversation> mQueryController;

        private final ArrayList<String> mParticipantIds = new ArrayList<>();
        private final ArrayList<Participant> mParticipants = new ArrayList<>();
        private final Map<String, Participant> mParticipantMap = new HashMap<>();

        AvailableConversationAdapter(LayerClient client, Picasso picasso, ChatParticipantProvider chatParticipantProvider) {
            this(client, picasso, chatParticipantProvider, null);
        }

        AvailableConversationAdapter(LayerClient client, Picasso picasso, ChatParticipantProvider chatParticipantProvider, Collection<String> updateAttributes) {
            mQueryController = client.newRecyclerViewController(null, updateAttributes, this);
            mLayerClient = client;
            mPicasso = picasso;
            this.chatParticipantProvider = chatParticipantProvider;
            setHasStableIds(false);
        }

        /**
         * Refreshes this adapter by filtering Conversations to return only those Conversations with
         * the given set of selected Participants.
         */
        void refresh(String filter, final Set<Participant> selectedParticipants) {
            // Apply text search filter to available participants
            synchronized (mParticipantIds) {
                compositeDisposable.add(chatParticipantProvider.getMatchingParticipants(filter == null ? "" : filter).
                        subscribeOn(Schedulers.io()).
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribe(new Consumer<Map<String, Participant>>() {
                            @Override
                            public void accept(Map<String, Participant> stringParticipantMap) throws Exception {
                                searchParticipantsReady(selectedParticipants, stringParticipantMap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.d(TAG, "Error during finding matching participants", throwable);
                            }
                        }));
            }
        }

        void destroy() {
            compositeDisposable.clear();
        }

        private void searchParticipantsReady(Set<Participant> selectedParticipants, Map<String, Participant> participants) {
            Identity authenticatedUser = mLayerClient.getAuthenticatedUser();
            final String userId = authenticatedUser != null ? authenticatedUser.getUserId() : null;
            mParticipantMap.clear();
            mParticipantMap.putAll(participants);
            mParticipants.clear();
            for (Map.Entry<String, Participant> entry : mParticipantMap.entrySet()) {
                // Don't show participants we've already selected
                if (selectedParticipants.contains(entry.getValue())) {
                    continue;
                }
                if (entry.getKey().equals(userId)) {
                    continue;
                }
                mParticipants.add(entry.getValue());
            }
            Collections.sort(mParticipants);

            mParticipantIds.clear();
            for (Participant p : mParticipants) {
                mParticipantIds.add(p.getId());
            }

            notifyDataSetChanged();
            // TODO: compute add/remove/move and notify those instead of complete notify

            if (mShowConversations) {
                queryConversations(generateIds(mSelectedParticipants));
            }
        }

        private void queryConversations(Set<String> selectedParticipants) {
            // Filter down to only those conversations including the selected participants, hiding one-on-one conversations
            Query.Builder<Conversation> builder = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING));
            if (selectedParticipants.isEmpty()) {
                builder.predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2));
            } else {
                builder.predicate(new CompoundPredicate(CompoundPredicate.Type.AND,
                        new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2),
                        new Predicate(Conversation.Property.PARTICIPANTS, Predicate.Operator.IN, selectedParticipants)));
            }
            mQueryController.setQuery(builder.build()).execute();
        }


        //==============================================================================================
        // Adapter
        //==============================================================================================

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(parent);
            viewHolder.mAvatar
                    .setShouldShowPresence(false)
                    .init(mPicasso)
                    .setStyle(mAvatarStyle);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int position) {
            synchronized (mParticipantIds) {
                switch (getType(position)) {
                    case PARTICIPANT: {
                        position = adapterPositionToParticipantPosition(position);
                        Participant participant = mParticipants.get(position);
                        viewHolder.mTitle.setText(participant.getName());
                        viewHolder.itemView.setTag(participant);
                        viewHolder.itemView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectParticipant((Participant) v.getTag());
                            }
                        });
                        viewHolder.mAvatar.setParticipants(participant);
                    }
                    break;

                    case CONVERSATION: {
                        position = adapterPositionToConversationPosition(position);
                        final Conversation conversation = mQueryController.getItem(position);
                        Identity user = mLayerClient.getAuthenticatedUser();
                        Set<Identity> ids = conversation.getParticipants();
                        ids.remove(user);
                        compositeDisposable.add(chatParticipantProvider.getParticipants(Util.getIdsFromIdentities(ids)).
                                subscribeOn(Schedulers.io()).
                                observeOn(AndroidSchedulers.mainThread()).
                                subscribe(new Consumer<List<Participant>>() {
                                    @Override
                                    public void accept(List<Participant> participants) throws Exception {
                                        conversationParticipantsReady(viewHolder, conversation, participants);
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Log.d(TAG, "Error during getting participants", throwable);
                                    }
                                }));
                    }
                    break;
                }
            }
        }

        private void conversationParticipantsReady(final ViewHolder viewHolder, final Conversation conversation, List<Participant> participants) {
            List<String> names = new ArrayList<>();
            for (Participant participant : participants) {
                if (participant == null) continue;
                names.add(participant.getName());
            }
            viewHolder.mTitle.setText(TextUtils.join(", ", names));
            viewHolder.itemView.setTag(conversation);
            viewHolder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnConversationClickListener == null) return;
                    mOnConversationClickListener.onConversationClick(AtlasAddressBar.this, (Conversation) v.getTag());
                }
            });
            viewHolder.mAvatar.setParticipants(new LinkedHashSet<>(participants));
        }

        // first are participants; then are conversations
        Type getType(int position) {
            synchronized (mParticipantIds) {
                return (position < mParticipantIds.size()) ? Type.PARTICIPANT : Type.CONVERSATION;
            }
        }

        int adapterPositionToParticipantPosition(int position) {
            return position;
        }

        int adapterPositionToConversationPosition(int position) {
            synchronized (mParticipantIds) {
                return position - mParticipantIds.size();
            }
        }

        int conversationPositionToAdapterPosition(int position) {
            synchronized (mParticipantIds) {
                return position + mParticipantIds.size();
            }
        }

        @Override
        public int getItemCount() {
            synchronized (mParticipantIds) {
                return mQueryController.getItemCount() + mParticipantIds.size();
            }
        }


        //==============================================================================================
        // Conversation UI update callbacks
        //==============================================================================================

        @Override
        public void onQueryDataSetChanged(RecyclerViewController controller) {
            notifyDataSetChanged();
        }

        @Override
        public void onQueryItemChanged(RecyclerViewController controller, int position) {
            notifyItemChanged(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeChanged(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemInserted(RecyclerViewController controller, int position) {
            notifyItemInserted(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeInserted(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemRemoved(RecyclerViewController controller, int position) {
            notifyItemRemoved(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeRemoved(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
            notifyItemMoved(conversationPositionToAdapterPosition(fromPosition), conversationPositionToAdapterPosition(toPosition));
        }


        //==============================================================================================
        // Inner classes
        //==============================================================================================

        protected class ViewHolder extends RecyclerView.ViewHolder {
            private AtlasAvatar mAvatar;
            private TextView mTitle;

            public ViewHolder(ViewGroup parent) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_address_bar_item, parent, false));
                mAvatar = (AtlasAvatar) itemView.findViewById(R.id.avatar);
                mTitle = (TextView) itemView.findViewById(R.id.title);
                mTitle.setTextColor(mListTextColor);
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mListTextSize);
                mTitle.setTypeface(mListTextTypeface, mListTextStyle);
            }
        }
    }


    public interface OnConversationClickListener {
        void onConversationClick(AtlasAddressBar conversationLauncher, Conversation conversation);
    }

    public interface OnParticipantSelectionChangeListener {
        void onParticipantSelectionChanged(AtlasAddressBar conversationLauncher, List<String> participants);
    }

    public interface OnParticipantSelectionFailedListener {
        void onMaxParticipantLimitExceeded();
    }
}