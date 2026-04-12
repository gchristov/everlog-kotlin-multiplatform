package com.imagepick.picker.dialog;

import android.app.Activity;
import android.view.Menu;

import com.cocosw.bottomsheet.BottomSheet;
import com.everlog.R;
import com.imagepick.utils.MenuUtils;

import java.lang.ref.WeakReference;

public class ELPickerDialog implements ELPickerDialogBuilder {

    private WeakReference<Activity> activity;
    private ActionListener actionListener;
    private int layoutResId;
    private int titleResId;

    private ELPickerDialog(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public static ELPickerDialogBuilder withActivity(Activity activity) {
        return new ELPickerDialog(activity);
    }

    @Override
    public ELPickerDialogBuilder actionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    @Override
    public ELPickerDialogBuilder menuLayout(int layoutResId) {
        this.layoutResId = layoutResId;
        return this;
    }

    @Override
    public ELPickerDialogBuilder title(int titleResId) {
        this.titleResId = titleResId;
        return this;
    }

    @Override
    public void show() {
        showSourcePicker();
    }

    // Image source

    private void showSourcePicker() {
        BottomSheet.Builder builder = buildImagePickerSheet();
        builder = builder.listener((dialog, which) -> {
            if (actionListener != null) {
                actionListener.onAction(which);
            }
        });
        MenuUtils.showMenu(activity.get(), builder);
    }

    private BottomSheet.Builder buildImagePickerSheet() {
        Menu menu = MenuUtils.inflateMenu(activity.get(), layoutResId);
        BottomSheet.Builder builder = new BottomSheet.Builder(activity.get(),
                R.style.BottomSheet_StyleDialog_Dark)
                .title(titleResId);
        builder = MenuUtils.populateSheetWithCameraMenuActions(activity.get(), menu, builder);
        return builder;
    }
}
