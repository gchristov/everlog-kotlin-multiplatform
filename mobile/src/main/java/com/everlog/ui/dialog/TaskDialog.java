package com.everlog.ui.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.everlog.R;

public class TaskDialog {

    public static TaskDialog td;

    private ProgressDialog dialog;
    private AsyncTask<?, ?, ?> task;

    private TaskDialog() {
        // No-op.
    }

    public static TaskDialog getInstance() {
        if (td == null)
            td = new TaskDialog();

        return td;
    }

    public void showProcessingDialog(Context a) {
        showDialog(null, a, null);
    }

    public void showDialog(String title, Context a) {
        showDialog(title, a, null);
    }

    public void showDialog(String msg, Context a, AsyncTask<?, ?, ?> task) {
        showDialog(msg, a, task, false);
    }

    public void showDialog(String msg, Context a, AsyncTask<?, ?, ?> task, boolean cancelable) {
        if (dialog != null) return;

        this.task = task;

        Context target = a;
        if (a instanceof Activity) {
            Activity activity = (Activity) a;
            target = activity.getParent() != null ? activity.getParent() : activity;
        }
        dialog = new ProgressDialog(target);
        dialog.setMessage(msg == null || msg.isEmpty() ? a.getString(R.string.processing) : msg);
        dialog.show();
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(dialog -> {
            if (TaskDialog.this.task != null)
                TaskDialog.this.task.cancel(true);
        });
        View tv1 = dialog.findViewById(android.R.id.message);
        if (tv1 != null) {
            TextView message = (TextView) tv1;
            message.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.black_lighter));
        }
    }

    public void hideDialog() {
        try {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
        }
    }
}
