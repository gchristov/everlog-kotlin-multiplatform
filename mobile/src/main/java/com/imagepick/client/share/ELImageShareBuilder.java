package com.imagepick.client.share;

import android.graphics.Bitmap;

public interface ELImageShareBuilder {

    ELImageShareBuilder withShareErrorListener(ShareErrorListener errorListener);

    ELImageShareBuilder withBitmap(Bitmap bitmap);

    ELImageShareBuilder withCaption(String caption);

    void share();

    interface ShareErrorListener {

        void onError(Throwable throwable);
    }
}
