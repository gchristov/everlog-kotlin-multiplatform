package com.everlog.utils.input;

import android.content.Context;
import android.text.TextUtils;

import java.util.regex.Pattern;

public class ValidationUtils {

    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final Pattern EMAIL_PATTERN
            = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    public static String validateEmail(Context context, String email) {
        if (TextUtils.isEmpty(email)) {
            return "Please enter your email address";
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "This email address is not valid";
        }
        return null;
    }

    public static String validatePassword(Context context, String password) {
        if (TextUtils.isEmpty(password)) {
            return "Please enter your password";
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            return String.format("Your password must be at least %d characters long", MIN_PASSWORD_LENGTH);
        }
        return null;
    }

    public static String validateName(Context context, String name) {
        if (TextUtils.isEmpty(name)) {
            return "Please enter your name";
        }
        return null;
    }
}
