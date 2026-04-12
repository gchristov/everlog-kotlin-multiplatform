package com.imagepick.client.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.imagepick.utils.UriUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

public class ELImageShare implements ELImageShareBuilder {

    private WeakReference<Activity> activity;
    private Bitmap toShare;
    private String caption;
    private ShareErrorListener shareErrorListener;

    private ELImageShare(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public static ELImageShareBuilder withActivity(Activity activity) {
        return new ELImageShare(activity);
    }

    @Override
    public ELImageShareBuilder withShareErrorListener(ShareErrorListener listener) {
        this.shareErrorListener = listener;
        return this;
    }

    @Override
    public ELImageShareBuilder withBitmap(Bitmap bitmap) {
        this.toShare = bitmap;
        return this;
    }

    @Override
    public ELImageShareBuilder withCaption(String caption) {
        this.caption = caption;
        return this;
    }

    @Override
    public void share() {
        if (toShare != null) {
            shareImage();
        }
    }

    // Share

    private void shareImage() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/*");
        try {
            i.putExtra(Intent.EXTRA_TEXT, caption);
            i.putExtra(Intent.EXTRA_STREAM, getImageUri(activity.get(), toShare));
            activity.get().startActivity(Intent.createChooser(i, "Share via..."));
        } catch (Exception ex) {
            ex.printStackTrace();
            if (shareErrorListener != null) {
                shareErrorListener.onError(ex);
            }
        }
    }

    private static Uri getImageUri(Context inContext, Bitmap inImage) throws Exception {
        File filePath = UriUtils.getImageFile(inContext.getCacheDir(), "shared_workout.jpg");
        FileOutputStream stream = new FileOutputStream(filePath);
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        stream.close();
        return UriUtils.getUriForFile(inContext, filePath);
    }
}
