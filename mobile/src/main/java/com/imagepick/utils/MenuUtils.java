package com.imagepick.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.appcompat.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.cocosw.bottomsheet.BottomSheet;
import com.everlog.R;

public final class MenuUtils {

    public static Menu inflateMenu(Activity context, int menuResId) {
        PopupMenu p  = new PopupMenu(context, null);
        Menu menu = p.getMenu();
        MenuInflater inflater = context.getMenuInflater();
        inflater.inflate(menuResId, menu);
        return menu;
    }

    public static BottomSheet.Builder populateSheetWithCameraMenuActions(Context context,
                                                                         Menu menu,
                                                                         BottomSheet.Builder builder) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.action_take_new) {
                PackageManager pm = context.getPackageManager();
                if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    // Camera is not present on this device, so don't add in the menu.
                    continue;
                }
            }
            builder = builder.sheet(item.getItemId(), item.getIcon(), item.getTitle());
        }
        return builder;
    }

    public static void showMenu(Context context, BottomSheet.Builder builder) {
        BottomSheet sheet = builder.show();
        if (sheet.getWindow() != null) {
            sheet.getWindow().setLayout(context.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_width), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
