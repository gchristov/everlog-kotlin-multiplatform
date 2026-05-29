package com.everlog.utils.glide;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import androidx.annotation.Nullable;

public class ELGlideModule {

    public static void loadImage(String url, ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(url)
                .into(imageView);
    }

    /**
     * Loads an image into an ImageView and sets the background of another view to the color of the
     * top-left pixel (0,0) of the loaded image. This is useful for matching the container background
     * to the image's own background color.
     */
    public static void loadImage(String url, ImageView imageView, final View backgroundView) {
        Glide.with(imageView.getContext())
                .asBitmap()
                .load(url)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        if (resource != null && resource.getWidth() > 0 && resource.getHeight() > 0) {
                            int color = resource.getPixel(0, 0);
                            backgroundView.setBackgroundColor(color);
                        }
                        return false;
                    }
                })
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
