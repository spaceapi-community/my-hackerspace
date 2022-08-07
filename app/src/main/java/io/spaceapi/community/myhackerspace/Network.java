/*
 * Copyright (C) 2012-2017 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2020-2022 Danilo Bargen (dbrgn)
 * Licensed under GNU's GPL 3, see README
 */
package io.spaceapi.community.myhackerspace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Network extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctxt, Intent intent) {
        if (Main.hasNetwork(ctxt)) {
            Log.i(Main.TAG, "Update widget on " + intent.getAction());
            Widget.UpdateAllWidgets(ctxt, true);
        } else {
            Log.e(Main.TAG, "Network not ready on " + intent.getAction());
        }
    }

}
