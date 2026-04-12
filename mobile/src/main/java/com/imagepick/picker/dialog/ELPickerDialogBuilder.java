package com.imagepick.picker.dialog;

public interface ELPickerDialogBuilder {

    ELPickerDialogBuilder actionListener(ActionListener actionListener);

    ELPickerDialogBuilder menuLayout(int layoutResId);

    ELPickerDialogBuilder title(int titleResId);

    void show();

    interface ActionListener {

        void onAction(int actionId);
    }
}
