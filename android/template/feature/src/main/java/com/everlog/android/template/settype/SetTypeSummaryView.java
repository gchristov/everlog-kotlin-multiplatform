package com.everlog.android.template.settype;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.everlog.android.template.R;
import com.everlog.android.common.data.model.set.ELSetType;
import com.everlog.android.common.kotlin.format.SetTypeFormatKt;
import com.everlog.android.template.databinding.ViewSetTypeSummaryBinding;

import androidx.annotation.Nullable;
import android.widget.RelativeLayout;

public class SetTypeSummaryView extends RelativeLayout {

    private ViewSetTypeSummaryBinding binding;

    public SetTypeSummaryView(Context context) {
        super(context);
        init(null, 0);
    }

    public SetTypeSummaryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SetTypeSummaryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    public void setSetType(ELSetType type) {
        binding.setTypeLbl.setText(SetTypeFormatKt.setTypeTitle(getContext(), type.name()));
        binding.setDescriptionLbl.setText(SetTypeFormatKt.setTypeDescription(getContext(), type.name()));
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        binding = ViewSetTypeSummaryBinding.inflate(LayoutInflater.from(getContext()), this, true);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SetTypeSummaryView, defStyleAttr, 0);

            try {
                boolean hasTitle = typedArray.hasValue(R.styleable.SetTypeSummaryView_stsvTitle);
                if (hasTitle) {
                    binding.setTypeLbl.setText(typedArray.getString(R.styleable.SetTypeSummaryView_stsvTitle));
                }
                boolean hasTitleColor = typedArray.hasValue(R.styleable.SetTypeSummaryView_stsvTitleColor);
                if (hasTitleColor) {
                    binding.setTypeLbl.setTextColor(typedArray.getColor(R.styleable.SetTypeSummaryView_stsvTitleColor, Color.WHITE));
                }
                boolean hasDescription = typedArray.hasValue(R.styleable.SetTypeSummaryView_stsvDescription);
                if (hasDescription) {
                    binding.setDescriptionLbl.setText(typedArray.getString(R.styleable.SetTypeSummaryView_stsvDescription));
                }
                boolean hasSeparator = typedArray.hasValue(R.styleable.SetTypeSummaryView_stsvSeparatorVisible);
                if (hasSeparator) {
                    binding.separator.setVisibility(typedArray.getBoolean(R.styleable.SetTypeSummaryView_stsvSeparatorVisible, true) ? VISIBLE : GONE);
                }
            } finally {
                typedArray.recycle();
            }
        }
    }
}
