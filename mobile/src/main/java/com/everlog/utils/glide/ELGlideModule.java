package com.everlog.utils.glide;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestOptions;

public class ELGlideModule {

    public static void loadImage(String url, ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(url)
                .into(imageView);
    }

    public static void loadImage(String url,
                                 ImageView imageView,
                                 Transformation<Bitmap> transformation) {
        Glide.with(imageView.getContext())
                .load(url)
                .apply(RequestOptions.bitmapTransform(transformation))
                .into(imageView);
    }

    public static void loadImage(String url,
                                 float thumbnail,
                                 ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(url)
                .thumbnail(thumbnail)
                .fitCenter()
                .into(imageView);
    }

    public static void loadImage(int imageResId, ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(imageResId)
                .into(imageView);
    }
}
