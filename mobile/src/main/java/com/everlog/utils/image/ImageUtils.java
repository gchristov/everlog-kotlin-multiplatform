package com.everlog.utils.image;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class ImageUtils {

    public static Drawable tintIcon(Context context, int resource, int color) {
        Drawable upArrow = context.getResources().getDrawable(resource);
        upArrow = upArrow.mutate();
        upArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return upArrow;
    }

    /*public static void setAvatar(ImageView imageView, GTUser user) {
        int p = R.drawable.default_avatar;
        if (user.hasAvatar()) {
            GlideApp
                    .with(imageView.getContext())
                    .load(user.avatar.thumbnail)
                    .placeholder(p)
                    .error(p)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .into(imageView);
        } else {
            GlideApp
                    .with(imageView.getContext())
                    .load(p)
                    .placeholder(p)
                    .into(imageView);
        }
    }*/
}
