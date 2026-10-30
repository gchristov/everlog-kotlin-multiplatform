package com.everlog.android.common.mvp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.amulyakhare.textdrawable.TextDrawable;
import com.christophesmet.android.views.maskableframelayout.MaskableFrameLayout;
import com.everlog.android.common.mvp.R;
import com.everlog.android.common.data.model.exercise.ELExercise;
import com.everlog.android.common.data.model.exercise.ExerciseSuggestion;
import com.everlog.android.common.mvp.util.ELGlideModule;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ExerciseImageView extends LinearLayout {

    MaskableFrameLayout mMaskableLayout;
    View mImageViewContainer;
    ImageView mImageView;

    private boolean mSquareHeight;
    private boolean mSquareWidth;
    private boolean mShowLetter;
    private boolean mFitCenter;

    public ExerciseImageView(Context context) {
        super(context);
        init(null, 0);
    }

    public ExerciseImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ExerciseImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSquareWidth) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        } else if (mSquareHeight) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    public void setExercise(ELExercise exercise) {
        if (TextUtils.isEmpty(exercise.getImageUrl())) {
            mImageView.setImageDrawable(null);
            if (!TextUtils.isEmpty(exercise.getName()) && mShowLetter) {
                mImageView.setImageDrawable(TextDrawable.builder()
                        .beginConfig()
                        .textColor(ContextCompat.getColor(getContext(), com.everlog.android.common.design.R.color.background_base))
                        .endConfig()
                        .buildRect(exercise.getFirstChar(), Color.WHITE));
            }
        } else {
            ELGlideModule.loadImage(exercise.getImageUrl(), mImageView);
        }
        setVisibility(View.VISIBLE);
    }

    public void setExercise(ExerciseSuggestion exercise) {
        mImageView.setImageDrawable(null);
        if (!TextUtils.isEmpty(exercise.getName()) && mShowLetter) {
            mImageView.setImageDrawable(TextDrawable.builder()
                    .beginConfig()
                    .textColor(ContextCompat.getColor(getContext(), com.everlog.android.common.design.R.color.background_base))
                    .endConfig()
                    .buildRect(exercise.getFirstChar(), Color.WHITE));
        }
        setVisibility(View.VISIBLE);
    }

    public void applyMask(int maskResId) {
        mMaskableLayout.setMask(maskResId);
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        View view = View.inflate(getContext(), R.layout.view_exercise_image, this);
        mMaskableLayout = view.findViewById(R.id.maskableLayout);
        mImageViewContainer = view.findViewById(R.id.imageViewContainer);
        mImageView = view.findViewById(R.id.imageView);

        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ExerciseImageView, defStyleAttr, 0);

            try {
                mSquareHeight = typedArray.getBoolean(R.styleable.ExerciseImageView_eivSquareHeight, true);
                mSquareWidth = typedArray.getBoolean(R.styleable.ExerciseImageView_eivSquareWidth, false);
                mShowLetter = typedArray.getBoolean(R.styleable.ExerciseImageView_eivShowLetter, true);
                mFitCenter = typedArray.getBoolean(R.styleable.ExerciseImageView_eivFitCenter, false);
            } finally {
                typedArray.recycle();
            }
        }

        if (mFitCenter) {
            mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }
}
