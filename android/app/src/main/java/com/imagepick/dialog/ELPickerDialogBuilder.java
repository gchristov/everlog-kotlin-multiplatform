package com.imagepick.dialog;

public interface ELPickerDialogBuilder {

    ELPickerDialogBuilder actionListener(ActionListener actionListener);

    ELPickerDialogBuilder menuLayout(int layoutResId);

    ELPickerDialogBuilder title(int titleResId);

    ELPickerDialogBuilder dismissListener(OnDismissListener dismissListener);

    void show();

    interface ActionListener {

        void onAction(int actionId);
    }

    interface OnDismissListener {

        void onDismiss();
    }
}
