package com.layer.atlas.util;

import android.graphics.Typeface;
import android.support.annotation.ColorInt;

public final class ConversationStyle {

    private int mTitleTextColor;
    private int mTitleTextStyle;
    private Typeface mTitleTextTypeface;
    private int mTitleUnreadTextColor;
    private int mTitleUnreadTextStyle;
    private Typeface mTitleUnreadTextTypeface;
    private int mSubtitleTextColor;
    private int mSubtitleTextStyle;
    private Typeface mSubtitleTextTypeface;
    private int mSubtitleUnreadTextColor;
    private int mSubtitleUnreadTextStyle;
    private Typeface mSubtitleUnreadTextTypeface;
    private int mCellBackgroundColor;
    private int mCellUnreadBackgroundColor;
    private Typeface mDateTextTypeface;
    private int mDateTextColor;
    private Typeface mDateUnreadTextTypeface;
    private int mDateUnreadTextColor;
    private AvatarStyle mAvatarStyle;

    private ConversationStyle(Builder builder) {
        mTitleTextColor = builder.titleTextColor;
        mTitleTextStyle = builder.titleTextStyle;
        setTitleTextTypeface(builder.titleTextTypeface);
        mTitleUnreadTextColor = builder.titleUnreadTextColor;
        mTitleUnreadTextStyle = builder.titleUnreadTextStyle;
        setTitleUnreadTextTypeface(builder.titleUnreadTextTypeface);
        mSubtitleTextColor = builder.subtitleTextColor;
        mSubtitleTextStyle = builder.subtitleTextStyle;
        setSubtitleTextTypeface(builder.subtitleTextTypeface);
        mSubtitleUnreadTextColor = builder.subtitleUnreadTextColor;
        mSubtitleUnreadTextStyle = builder.subtitleUnreadTextStyle;
        setSubtitleUnreadTextTypeface(builder.subtitleUnreadTextTypeface);
        mCellBackgroundColor = builder.cellBackgroundColor;
        mCellUnreadBackgroundColor = builder.cellUnreadBackgroundColor;
        setDateTextTypeface(builder.dateTextTypeface);
        mDateTextColor = builder.dateTextColor;
        setDateUnreadTextTypeface(builder.dateUnreadTextTypeface);
        mDateUnreadTextColor = builder.dateUnreadTextColor;
        mAvatarStyle = builder.avatarStyle;
    }

    public void setTitleTextTypeface(Typeface titleTextTypeface) {
        this.mTitleTextTypeface = titleTextTypeface;
    }

    public void setTitleUnreadTextTypeface(Typeface titleUnreadTextTypeface) {
        this.mTitleUnreadTextTypeface = titleUnreadTextTypeface;
    }

    public void setSubtitleTextTypeface(Typeface subtitleTextTypeface) {
        this.mSubtitleTextTypeface = subtitleTextTypeface;
    }

    public void setSubtitleUnreadTextTypeface(Typeface subtitleUnreadTextTypeface) {
        this.mSubtitleUnreadTextTypeface = subtitleUnreadTextTypeface;
    }

    public void setDateTextTypeface(Typeface dateTextTypeface) {
        this.mDateTextTypeface = dateTextTypeface;
    }

    public void setDateUnreadTextTypeface(Typeface dateUnreadTextTypeface) {
        this.mDateUnreadTextTypeface = dateUnreadTextTypeface;
    }

    @ColorInt
    public int getTitleTextColor() {
        return mTitleTextColor;
    }

    public int getTitleTextStyle() {
        return mTitleTextStyle;
    }

    public Typeface getTitleTextTypeface() {
        return mTitleTextTypeface;
    }

    @ColorInt
    public int getTitleUnreadTextColor() {
        return mTitleUnreadTextColor;
    }

    public int getTitleUnreadTextStyle() {
        return mTitleUnreadTextStyle;
    }

    public Typeface getTitleUnreadTextTypeface() {
        return mTitleUnreadTextTypeface;
    }

    @ColorInt
    public int getSubtitleTextColor() {
        return mSubtitleTextColor;
    }

    public int getSubtitleTextStyle() {
        return mSubtitleTextStyle;
    }

    public Typeface getSubtitleTextTypeface() {
        return mSubtitleTextTypeface;
    }

    @ColorInt
    public int getSubtitleUnreadTextColor() {
        return mSubtitleUnreadTextColor;
    }

    public int getSubtitleUnreadTextStyle() {
        return mSubtitleUnreadTextStyle;
    }

    public Typeface getSubtitleUnreadTextTypeface() {
        return mSubtitleUnreadTextTypeface;
    }

    @ColorInt
    public int getCellBackgroundColor() {
        return mCellBackgroundColor;
    }

    @ColorInt
    public int getCellUnreadBackgroundColor() {
        return mCellUnreadBackgroundColor;
    }

    public Typeface getDateTextTypeface() {
        return mDateTextTypeface;
    }

    @ColorInt
    public int getDateTextColor() {
        return mDateTextColor;
    }

    public Typeface getDateUnreadTextTypeface() {
        return mDateUnreadTextTypeface;
    }

    @ColorInt
    public int getDateUnreadTextColor() {
        return mDateUnreadTextColor;
    }

    public AvatarStyle getAvatarStyle() {
        return mAvatarStyle;
    }

    public static final class Builder {
        private int titleTextColor;
        private int titleTextStyle;
        private Typeface titleTextTypeface;
        private int titleUnreadTextColor;
        private int titleUnreadTextStyle;
        private Typeface titleUnreadTextTypeface;
        private int subtitleTextColor;
        private int subtitleTextStyle;
        private Typeface subtitleTextTypeface;
        private int subtitleUnreadTextColor;
        private int subtitleUnreadTextStyle;
        private Typeface subtitleUnreadTextTypeface;
        private int cellBackgroundColor;
        private int cellUnreadBackgroundColor;
        private Typeface dateTextTypeface;
        private int dateTextColor;
        private Typeface dateUnreadTextTypeface;
        private int dateUnreadTextColor;
        private AvatarStyle avatarStyle;

        public Builder() {
        }

        public Builder titleTextColor(@ColorInt int val) {
            titleTextColor = val;
            return this;
        }

        public Builder titleTextStyle(int val) {
            titleTextStyle = val;
            return this;
        }

        public Builder titleTextTypeface(Typeface val) {
            titleTextTypeface = val;
            return this;
        }

        public Builder titleUnreadTextColor(@ColorInt int val) {
            titleUnreadTextColor = val;
            return this;
        }

        public Builder titleUnreadTextStyle(int val) {
            titleUnreadTextStyle = val;
            return this;
        }

        public Builder titleUnreadTextTypeface(Typeface val) {
            titleUnreadTextTypeface = val;
            return this;
        }

        public Builder subtitleTextColor(@ColorInt int val) {
            subtitleTextColor = val;
            return this;
        }

        public Builder subtitleTextStyle(int val) {
            subtitleTextStyle = val;
            return this;
        }

        public Builder subtitleTextTypeface(Typeface val) {
            subtitleTextTypeface = val;
            return this;
        }

        public Builder subtitleUnreadTextColor(@ColorInt int val) {
            subtitleUnreadTextColor = val;
            return this;
        }

        public Builder subtitleUnreadTextStyle(int val) {
            subtitleUnreadTextStyle = val;
            return this;
        }

        public Builder subtitleUnreadTextTypeface(Typeface val) {
            subtitleUnreadTextTypeface = val;
            return this;
        }

        public Builder cellBackgroundColor(@ColorInt int val) {
            cellBackgroundColor = val;
            return this;
        }

        public Builder cellUnreadBackgroundColor(@ColorInt int val) {
            cellUnreadBackgroundColor = val;
            return this;
        }

        public Builder dateTextTypeface(Typeface val) {
            dateTextTypeface = val;
            return this;
        }

        public Builder dateTextColor(@ColorInt int val) {
            dateTextColor = val;
            return this;
        }

        public Builder dateUnreadTextTypeface(Typeface val) {
            dateUnreadTextTypeface = val;
            return this;
        }

        public Builder dateUnreadTextColor(@ColorInt int val) {
            dateUnreadTextColor = val;
            return this;
        }

        public Builder avatarStyle(AvatarStyle val) {
            avatarStyle = val;
            return this;
        }

        public ConversationStyle build() {
            return new ConversationStyle(this);
        }
    }
}
