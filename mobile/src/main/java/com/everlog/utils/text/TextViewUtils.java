package com.everlog.utils.text;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import com.everlog.R;

import androidx.core.content.ContextCompat;

public class TextViewUtils {

    public static void addClickableSpans(TextView textView,
                                         TouchableSpan[] actions,
                                         String text,
                                         String[] clickableTexts) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
        for (int i = 0; i < clickableTexts.length; i++) {
            String clickableText = clickableTexts[i];
            TouchableSpan action = actions[i];
            int startIndex = text.indexOf(clickableText);
            stringBuilder.setSpan(action, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setMovementMethod(LinkTouchMovementMethod.getSharedInstance());
        textView.setHighlightColor(Color.TRANSPARENT);
        textView.setText(stringBuilder, TextView.BufferType.SPANNABLE);
    }

    public static void addWorkoutLinkSpan(TextView textView,
                                          String text,
                                          int start,
                                          int end) {
        Spannable spannedTitle = new SpannableString(text);
        spannedTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(textView.getContext(), R.color.main_accent)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannedTitle.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannedTitle);
    }

    public static void addProFeatureSpan(TextView textView,
                                         String text,
                                         int start,
                                         int end) {
        Spannable spannedTitle = new SpannableString(text);
        spannedTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(textView.getContext(), R.color.white_base)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannedTitle.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannedTitle);
    }
}
