package com.everlog.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.everlog.R;
import com.everlog.databinding.RowFooterLinkBinding;

public class FooterView extends LinearLayout {

    private RowFooterLinkBinding binding;

    public FooterView(Context context) {
        super(context);
        init(null, 0);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setupLayout(attrs, defStyle);
    }

    // Setup

    private void setupLayout(AttributeSet attrs, int defStyleAttr) {
        binding = RowFooterLinkBinding.inflate(LayoutInflater.from(getContext()), this, true);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.FooterView, defStyleAttr, 0);

            try {
                boolean hasTitle = typedArray.hasValue(R.styleable.FooterView_fvTitle);
                if (hasTitle) {
                    binding.footerText.setText(typedArray.getString(R.styleable.FooterView_fvTitle));
                }
                boolean hasLink = typedArray.hasValue(R.styleable.FooterView_fvLink);
                if (hasLink) {
                    binding.footerLink.setText(typedArray.getString(R.styleable.FooterView_fvLink));
                }

                SpannableString content = new SpannableString(binding.footerLink.getText());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                binding.footerLink.setText(content);
            } finally {
                typedArray.recycle();
            }
        }
    }
}
