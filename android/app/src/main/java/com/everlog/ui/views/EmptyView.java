package com.everlog.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.everlog.R;
import com.everlog.databinding.ViewEmptyStateBinding;

import androidx.annotation.Nullable;
import android.widget.LinearLayout;
import rx.Observable;
import rx.subjects.PublishSubject;

public class EmptyView extends LinearLayout {

    private ViewEmptyStateBinding binding;

    private PublishSubject<Void> mOnClickAction = PublishSubject.create();
    private PublishSubject<Void> mOnClickActionSecondary = PublishSubject.create();

    public EmptyView(Context context) {
        super(context);
        init(null, 0);
    }

    public EmptyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EmptyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    public void setImage(int imageResId) {
        if (imageResId < 0) {
            binding.emptyImage.setVisibility(GONE);
        } else {
            binding.emptyImage.setVisibility(VISIBLE);
            binding.emptyImage.setImageResource(imageResId);
        }
    }

    public void setTitle(int textResId) {
        setTitle(getContext().getString(textResId));
    }

    public void setTitle(String text) {
        binding.emptyTitle.setVisibility(text != null ? VISIBLE : GONE);
        binding.emptyTitle.setText(text);
    }

    public void setSubtitle(int textResId) {
        binding.emptySubtitle.setVisibility(textResId > 0 ? VISIBLE : GONE);
        if (textResId > 0) {
            binding.emptySubtitle.setText(textResId);
        }
    }

    public void setActionStyle(int backgroundResId, int textColorResId) {
        binding.emptyAction.setBackgroundResource(backgroundResId);
        binding.emptyAction.setTextColor(textColorResId);
    }

    public void setActionSecondaryText(int textResId) {
        setActionSecondaryText(getContext().getString(textResId));
    }

    public void setActionSecondaryText(String text) {
        binding.emptyActionSecondary.setVisibility(text != null ? VISIBLE : GONE);
        binding.emptyActionSecondary.setText(text);
    }

    public void setActionSecondaryStyle(int backgroundResId, int textColorResId) {
        binding.emptyActionSecondary.setBackgroundResource(backgroundResId);
        binding.emptyActionSecondary.setTextColor(textColorResId);
    }

    public Observable<Void> onActionClick() {
        return mOnClickAction;
    }

    public Observable<Void> onActionSecondaryClick() {
        return mOnClickActionSecondary;
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(getContext()), this, true);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.EmptyView, defStyleAttr, 0);

            try {
                boolean hasImage = typedArray.hasValue(R.styleable.EmptyView_evImage);
                binding.emptyImage.setVisibility(hasImage ? VISIBLE : GONE);
                if (hasImage) {
                    binding.emptyImage.setImageDrawable(typedArray.getDrawable(R.styleable.EmptyView_evImage));
                }
                boolean hastTitle = typedArray.hasValue(R.styleable.EmptyView_evTitle);
                binding.emptyTitle.setVisibility(hastTitle ? VISIBLE : GONE);
                if (hastTitle) {
                    binding.emptyTitle.setText(typedArray.getString(R.styleable.EmptyView_evTitle));
                }
                boolean hasSubtitle = typedArray.hasValue(R.styleable.EmptyView_evSubtitle);
                binding.emptySubtitle.setVisibility(hasSubtitle ? VISIBLE : GONE);
                if (hasSubtitle) {
                    binding.emptySubtitle.setText(typedArray.getString(R.styleable.EmptyView_evSubtitle));
                }
                boolean hasActionText = typedArray.hasValue(R.styleable.EmptyView_evActionText);
                binding.emptyAction.setVisibility(hasActionText ? VISIBLE : GONE);
                if (hasActionText) {
                    binding.emptyAction.setText(typedArray.getString(R.styleable.EmptyView_evActionText));
                }
                binding.emptyAction.setOnClickListener(v -> {
                    mOnClickAction.onNext(null);
                });
                boolean hasActionSecondaryText = typedArray.hasValue(R.styleable.EmptyView_evActionSecondaryText);
                binding.emptyActionSecondary.setVisibility(hasActionSecondaryText ? VISIBLE : GONE);
                if (hasActionSecondaryText) {
                    binding.emptyActionSecondary.setText(typedArray.getString(R.styleable.EmptyView_evActionSecondaryText));
                }
                binding.emptyActionSecondary.setOnClickListener(v -> {
                    mOnClickActionSecondary.onNext(null);
                });
            } finally {
                typedArray.recycle();
            }
        }
    }
}
