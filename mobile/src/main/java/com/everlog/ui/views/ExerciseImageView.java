package com.everlog.ui.views;

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
import com.everlog.R;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.data.model.exercise.ExerciseSuggestion;
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity;
import com.everlog.ui.navigator.ELNavigator;
import com.everlog.utils.glide.ELGlideModule;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ExerciseImageView extends LinearLayout {

    MaskableFrameLayout mMaskableLayout;
    View mImageViewContainer;
    ImageView mImageView;

    private ELExercise mExercise;
    private boolean mSquareHeight;
    private boolean mSquareWidth;
    private boolean mClickable;
    private boolean mShowLetter;

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
        setOnClickListener(v -> {
            if (mClickable && mExercise != null) {
                new ELNavigator(getContext()).openExerciseDetails(new ExerciseDetailsActivity.Companion.Properties()
                        .exercise(mExercise));
            }
        });
    }

    public void setExercise(ELExercise exercise) {
        this.mExercise = exercise;
        if (TextUtils.isEmpty(exercise.getImageUrl())) {
            mImageView.setImageDrawable(null);
            if (!TextUtils.isEmpty(exercise.getName()) && mShowLetter) {
                mImageView.setImageDrawable(TextDrawable.builder()
                        .beginConfig()
                        .textColor(ContextCompat.getColor(getContext(), R.color.background_base))
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
                    .textColor(ContextCompat.getColor(getContext(), R.color.background_base))
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
                mClickable = typedArray.getBoolean(R.styleable.ExerciseImageView_eivClickable, true);
                mShowLetter = typedArray.getBoolean(R.styleable.ExerciseImageView_eivShowLetter, true);
            } finally {
                typedArray.recycle();
            }
        }
    }
}
