package com.imagepick.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;

public class UriUtils {

    private static final String PROVIDER = ".provider";

    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, getAuthority(context), file);
    }

    public static File getImageFile(File parent, String fileName) {
        File path = new File(parent, "camera");
        if (!path.exists()) path.mkdirs();
        return new File(path, fileName);
    }

    private static String getAuthority(Context context) {
        return context.getApplicationContext().getPackageName() + PROVIDER;
    }
}
