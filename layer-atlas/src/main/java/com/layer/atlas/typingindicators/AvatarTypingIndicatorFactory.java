package com.layer.atlas.typingindicators;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.AtlasTypingIndicator;
import com.layer.atlas.R;
import com.layer.atlas.support.ChatAttendeesProvider;
import com.layer.atlas.support.Participant;
import com.layer.atlas.util.Util;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Identity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class AvatarTypingIndicatorFactory implements AtlasTypingIndicator.TypingIndicatorFactory<LinearLayout> {
    private static final String TAG = AvatarTypingIndicatorFactory.class.getSimpleName();

    private static final int DOT_RES_ID = R.drawable.atlas_typing_indicator_dot;
    private static final float DOT_ON_ALPHA = 0.31f;
    private static final long ANIMATION_PERIOD = 600;
    private static final long ANIMATION_OFFSET = ANIMATION_PERIOD / 3;

    private final Picasso mPicasso;
    private final ChatAttendeesProvider chatAttendeesProvider;
    private final CompositeSubscription compositeSubscription = new CompositeSubscription();

    public AvatarTypingIndicatorFactory(Picasso picasso, ChatAttendeesProvider chatAttendeesProvider) {
        mPicasso = picasso;
        this.chatAttendeesProvider = chatAttendeesProvider;
    }

    @Override
    public LinearLayout onCreateView(Context context) {
        Tag tag = new Tag();

        Resources r = context.getResources();
        int dotSize = r.getDimensionPixelSize(R.dimen.atlas_typing_indicator_dot_size);
        int dotSpace = r.getDimensionPixelSize(R.dimen.atlas_typing_indicator_dot_space);

        LinearLayout l = new LinearLayout(context);
        l.setGravity(Gravity.CENTER);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutParams(new AtlasTypingIndicator.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView v;
        LinearLayout.LayoutParams vp;

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        vp.setMargins(0, 0, dotSpace, 0);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        tag.mDots.add(v);
        l.addView(v);

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        vp.setMargins(0, 0, dotSpace, 0);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        tag.mDots.add(v);
        l.addView(v);

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        tag.mDots.add(v);
        l.addView(v);

        l.setTag(tag);
        return l;
    }

    @Override
    public void onBindView(final LinearLayout l, final Map<Identity, LayerTypingIndicatorListener.TypingIndicator> typingUserIds) {
        @SuppressWarnings("unchecked")
        final Tag tag = (Tag) l.getTag();

        final int avatarSpace = l.getResources().getDimensionPixelSize(R.dimen.atlas_padding_narrow);
        final int avatarDim = l.getResources().getDimensionPixelSize(R.dimen.atlas_message_avatar_item_single);

        final List<String> participantsIdsList = Util.getIdsFromIdentities(typingUserIds.keySet());

        Subscription participantsSingleSubscription = chatAttendeesProvider.getParticipants(participantsIdsList).
                subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Action1<List<Participant>>() {
                    @Override
                    public void call(List<Participant> participants) {
                        // Iterate over existing typists and remove non-typists
                        List<AtlasAvatar> newlyFinished = new ArrayList<>();
                        Set<Identity> newlyActives = new HashSet<>(typingUserIds.keySet());
                        for (AtlasAvatar avatar : tag.mActives) {
                            if (!avatar.getParticipants().keySet().iterator().hasNext())
                                return;

                            String existingTypistKey = avatar.getParticipants().keySet().iterator().next();
                            Identity existingTypist = findTypist(existingTypistKey, typingUserIds.keySet());
                            if (existingTypist == null) {
                                return;
                            }
                            if (!typingUserIds.containsKey(existingTypist) || (typingUserIds.get(existingTypist) == LayerTypingIndicatorListener.TypingIndicator.FINISHED)) {
                                // Newly finished
                                newlyFinished.add(avatar);
                            } else {
                                // Existing started or paused
                                avatar.setAlpha(typingUserIds.get(existingTypist) == LayerTypingIndicatorListener.TypingIndicator.STARTED ? 1f : 0.5f);
                                newlyActives.remove(existingTypist);
                            }
                        }
                        for (AtlasAvatar avatar : newlyFinished) {
                            tag.mActives.remove(avatar);
                            tag.mPassives.add(avatar);
                            l.removeView(avatar);
                        }

                        // Add new typists
                        for (Identity typist : newlyActives) {
                            AtlasAvatar avatar = tag.mPassives.poll();
                            if (avatar == null) {
                                // TODO: allow styling
                                avatar = new AtlasAvatar(l.getContext()).init(mPicasso);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(avatarDim, avatarDim);
                                params.setMargins(0, 0, avatarSpace, 0);
                                avatar.setLayoutParams(params);
                            }
                            avatar.setAlpha(typingUserIds.get(typist) == LayerTypingIndicatorListener.TypingIndicator.STARTED ? 1f : 0.5f);
                            tag.mActives.add(avatar);
                            l.addView(avatar, 0);
                            Participant participant = findParticipant(typist.getUserId(), participants);
                            avatar.setParticipant(participant);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "Problem with getting participants", throwable);
                    }
                });
        compositeSubscription.add(participantsSingleSubscription);

        // Dot animations
        View dot1 = tag.mDots.get(0);
        View dot2 = tag.mDots.get(1);
        View dot3 = tag.mDots.get(2);

        Boolean animating = (Boolean) dot1.getTag();
        if (animating == null) animating = false;

        if (animating && typingUserIds.isEmpty()) {
            // Stop animating
            dot1.clearAnimation();
            dot2.clearAnimation();
            dot3.clearAnimation();
            dot1.setTag(true);
        } else if (!animating && !typingUserIds.isEmpty()) {
            // Start animating
            dot1.setAlpha(DOT_ON_ALPHA);
            dot2.setAlpha(DOT_ON_ALPHA);
            dot3.setAlpha(DOT_ON_ALPHA);
            startAnimation(dot1, ANIMATION_PERIOD, 0);
            startAnimation(dot2, ANIMATION_PERIOD, ANIMATION_OFFSET);
            startAnimation(dot3, ANIMATION_PERIOD, ANIMATION_OFFSET + ANIMATION_OFFSET);
            dot1.setTag(false);
        }
    }

    /**
     * Call onDestroy to prevent memory leaks.
     */
    public void onDestroy() {
        compositeSubscription.clear();
    }

    @Nullable
    private Participant findParticipant(String typistUserId, List<Participant> participants) {
        for (Participant participant : participants) {
            if (participant.getId().equals(typistUserId)) {
                return participant;
            }
        }
        return null;
    }

    @Nullable
    private Identity findTypist(String existingTypistKey, Set<Identity> identities) {
        for (Identity setIdentity : identities) {
            if (setIdentity.getUserId().equals(existingTypistKey)) {
                return setIdentity;
            }
        }
        return null;
    }

    /**
     * Starts a repeating fade out / fade in with the given period and offset in milliseconds.
     *
     * @param view        View to start animating.
     * @param period      Length of time in milliseconds for the fade out / fade in period.
     * @param startOffset Length of time in milliseconds to delay the initial start.
     */
    private void startAnimation(final View view, long period, long startOffset) {
        final AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(period / 2);
        fadeOut.setStartOffset(startOffset);
        fadeOut.setInterpolator(COSINE_INTERPOLATOR);

        final AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(period / 2);
        fadeIn.setStartOffset(0);
        fadeIn.setInterpolator(COSINE_INTERPOLATOR);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeIn.setStartOffset(0);
                fadeIn.reset();
                view.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeOut.setStartOffset(0);
                fadeOut.reset();
                view.startAnimation(fadeOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.startAnimation(fadeOut);
    }

    /**
     * Ease in/out
     */
    private final Interpolator COSINE_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            return (float) (1.0 - Math.cos(input * Math.PI / 2.0));
        }
    };

    private static class Tag {
        public final ArrayList<View> mDots = new ArrayList<View>(3);
        public final LinkedList<AtlasAvatar> mActives = new LinkedList<AtlasAvatar>();
        public final LinkedList<AtlasAvatar> mPassives = new LinkedList<AtlasAvatar>();
    }
}
