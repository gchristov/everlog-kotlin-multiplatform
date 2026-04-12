package com.everlog.utils.device;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.everlog.R;

import org.apache.commons.lang3.text.WordUtils;

public class DeviceUtils {

	public static boolean isScreenOn(Context c) {
		DisplayManager dm = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
		if (dm != null && dm.getDisplays() != null) {
			for (Display display : dm.getDisplays()) {
				if (display.getState() == Display.STATE_ON
						|| display.getState() == Display.STATE_UNKNOWN) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isLandscape(Context c) {
		return getScreenOrientation(c) == Configuration.ORIENTATION_LANDSCAPE;
	}
	
	public static int getScreenOrientation(Context context) {
		Display display = getScreenDisplay(context);
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		int orientation;
		if (width < height) {
			orientation = Configuration.ORIENTATION_PORTRAIT;
		} else {
			orientation = Configuration.ORIENTATION_LANDSCAPE;
		}
		return orientation;
	}
	
	public static Display getScreenDisplay( Context ctxt ) {
		WindowManager wm = (WindowManager) ctxt.getSystemService( Context.WINDOW_SERVICE );
		return wm.getDefaultDisplay();
	}
	
	public static int getScreenWidth(Context c) {
		Display display = getScreenDisplay(c);
		Point size = new Point();
		display.getSize(size);
		return size.x;
	}
	
	public static int getScreenHeight(Context c) {
		Display display = getScreenDisplay(c);
		Point size = new Point();
		display.getSize(size);
		return size.y;
	}
	
	public static boolean isTablet(Context c) {
		return c.getResources().getBoolean(R.bool.isTablet);
	}

	public static boolean isAndroidO() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}

	public static boolean isAndroidQ() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
	}

	public static boolean isAndroidU() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
	}

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return WordUtils.capitalize(model.toLowerCase());
        } else {
            return WordUtils.capitalize(manufacturer.toLowerCase()) + " " + model;
        }
    }
}
