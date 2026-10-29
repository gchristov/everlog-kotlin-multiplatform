package com.everlog.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import timber.log.Timber;

public class NumberUtils {

    private static final String TAG = "NumberUtils";

    public static boolean isWhole(float value) {
        return value % 1 == 0;
    }

    public static float parseFloat(String floatStr) {
        Float val = (float) 0;
        if (floatStr != null && !floatStr.isEmpty()) {
            try {
                val = Float.valueOf(floatStr);
            } catch (NumberFormatException ex) {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setDecimalSeparator(',');
                DecimalFormat format = new DecimalFormat("0.#");
                format.setDecimalFormatSymbols(symbols);
                try {
                    val = format.parse(floatStr).floatValue();
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                    Timber.tag(TAG).e(ex2);
                }
            }
        }
        return val;
    }
}
