package com.ghelius.narodmon;

/**
 * Created with IntelliJ IDEA.
 * User: eugene
 * Date: 10/2/12
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WakefulIntentService.sendWakefulWork(context, WatchService.class);
    }
}