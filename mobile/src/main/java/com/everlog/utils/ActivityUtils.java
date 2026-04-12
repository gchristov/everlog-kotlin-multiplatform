package com.everlog.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.everlog.R;
import com.everlog.utils.device.DeviceUtils;
import com.everlog.utils.glide.ELGlideModule;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityUtils {

    public static void setupWorkoutCoverImage(ImageView imageView) {
        ELGlideModule.loadImage(R.drawable.background_workout, imageView);
    }

    public static void setupBackgroundImage(Activity activity,
                                            int imageResId,
                                            int backgroundResId) {
        if (activity.findViewById(backgroundResId) != null) {
            ImageView background = activity.findViewById(backgroundResId);
            ELGlideModule.loadImage(imageResId, background);
        }
    }

    public static void setupBackgroundImage(View view,
                                            int imageResId,
                                            int backgroundResId) {
        if (view.findViewById(backgroundResId) != null) {
            ImageView background = view.findViewById(backgroundResId);
            ELGlideModule.loadImage(imageResId, background);
        }
    }

	public static void setOrientation(AppCompatActivity a) {
		if ( !DeviceUtils.isTablet(a) )
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	public static void setCurrentOrientationAsFixed(AppCompatActivity a) {
        if (a.getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
	
	public static void showAsPopup(AppCompatActivity activity, int width, int height) {
	    //To show activity as dialog and dim the background, you need to declare android:theme="@style/PopupTheme" on for the chosen activity on the manifest
	    activity.getWindow().setFlags(LayoutParams.FLAG_DIM_BEHIND,
	        LayoutParams.FLAG_DIM_BEHIND);
	    LayoutParams params = activity.getWindow().getAttributes(); 
	    params.alpha = 1.0f;
	    params.dimAmount = 0.5f;
	    activity.getWindow().setAttributes(params);

	    // This sets the window size, while working around the IllegalStateException thrown by ActionBarView
	    activity.getWindow().setLayout(width, height);
	}

	public static void hideActionBar(AppCompatActivity activity) {
		activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);
		activity.getSupportActionBar().hide();
	}

	public static void enableFullScreen(AppCompatActivity activity) {
		activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
		activity.getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,
				LayoutParams.FLAG_FULLSCREEN);
	}

	public static void colorMenu(Context context, Menu menu) {
        if (menu == null) return;

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(context.getResources().getColor(R.color.white_base), PorterDuff.Mode.SRC_ATOP);
            }
        }
	}

    public static void colorMenu(Context context, Menu menu, int colorResId) {
        if (menu == null) return;

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(context.getResources().getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }
}
