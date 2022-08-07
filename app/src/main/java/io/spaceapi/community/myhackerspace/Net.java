/*
 * Copyright (C) 2012-2017 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2020-2022 Danilo Bargen (dbrgn)
 * Licensed under GNU's GPL 3, see README
 */
package io.spaceapi.community.myhackerspace;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

// From CommonsWare and Android Blog
// https://github.com/commonsguy/cw-android/tree/master/Internet
// http://android-developers.blogspot.ch/2010/07/multithreading-for-performance.html
public class Net {

    private final String USERAGENT = "Android/" + Build.VERSION.RELEASE + " ("
            + Build.MODEL + ") MyHackerspace/" + BuildConfig.VERSION_NAME;

    private static final String TAG = "MyHackerspace_Net";

    private HttpURLConnection mUrlConnection;
    private InputStream mInputStream;

    @WorkerThread
    public Net(@NonNull String urlStr) throws Throwable {
        this(urlStr, true);
    }

    @WorkerThread
    public Net(@NonNull String urlStr, boolean useCache) throws Throwable {
        // Connect to URL
        URL url;
        int responseCode;
        int redirect_limt = 10;
        do {
            Log.v(TAG, "fetching " + urlStr);
            url = new URL(urlStr);
            mUrlConnection = (HttpURLConnection) url.openConnection();
            mUrlConnection.setRequestProperty("User-Agent", USERAGENT);
            mUrlConnection.setUseCaches(useCache);

            mUrlConnection.connect();
            responseCode = mUrlConnection.getResponseCode();

            // HttpsURLConnection does not support redirect with protocol switch,
            // so we take care of that here:
            if(responseCode == HttpURLConnection.HTTP_MOVED_TEMP
            || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                urlStr = mUrlConnection.getHeaderField("Location");
                redirect_limt -= 1;
            } else {
                break;
            }
        } while(redirect_limt > 0);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String msg = mUrlConnection.getResponseMessage();
            mUrlConnection.disconnect();
            throw new Throwable(msg);
        }

        mInputStream = mUrlConnection.getInputStream();
    }

    @WorkerThread
    @NonNull
    public String getString() throws Throwable {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(mInputStream));
            StringBuilder str = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                str.append(line);
            }
            return str.toString();
        } finally {
            if (mInputStream != null) {
                mInputStream.close();
            }
            mUrlConnection.disconnect();
        }
    }

    @WorkerThread
    @NonNull
    public Bitmap getBitmap() throws Throwable {
        try {
            return BitmapFactory.decodeStream(new FlushedInputStream(mInputStream));
        } finally {
            if (mInputStream != null) {
                mInputStream.close();
            }
            mUrlConnection.disconnect();
        }
    }

    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

}
