package com.everlog.managers.auth.samples;

import android.content.Context;
import android.os.AsyncTask;

import com.everlog.data.datastores.ELDatastore;
import com.everlog.data.model.ELRoutine;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import timber.log.Timber;

public class CreateSampleRoutinesAsyncTask extends AsyncTask<Boolean, Boolean, Boolean> {

    private static final String TAG = "CreateSampleRoutinesAsy";

    private WeakReference<Context> mContext;
    private OnCreateSampleRoutinesListener mListener;

    public CreateSampleRoutinesAsyncTask(Context context, OnCreateSampleRoutinesListener listener) {
        this.mContext = new WeakReference<>(context);
        this.mListener = listener;
    }

    @Override
    protected Boolean doInBackground(Boolean... booleans) {
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            Iterator<String> iterator = obj.keys();
            Gson gson = new Gson();
            while (iterator.hasNext()) {
                String key = iterator.next();
                JSONObject routineJson = obj.getJSONObject(key);
                ELRoutine routine = gson.fromJson(routineJson.toString(), ELRoutine.class);
                ELDatastore.routineStore().create(routine, SetOptions.merge());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Timber.tag(TAG).e(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (mListener != null) {
            mListener.onComplete();
        }
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = mContext.get().getAssets().open("sample_routines.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Timber.tag(TAG).e(ex);
            return null;
        }
        return json;
    }

    public interface OnCreateSampleRoutinesListener {

        void onComplete();
    }
}
