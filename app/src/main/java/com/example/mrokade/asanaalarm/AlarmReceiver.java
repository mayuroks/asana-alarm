package com.example.mrokade.asanaalarm;

/**
 * Created by mrokade on 3/21/16.
 */
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by mrokade on 3/1/16.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {
    Boolean close;
    private final String TAG = AlarmReceiver.class.getSimpleName();


    @Override
    public void onReceive(final Context context, Intent intent) {
        close = intent.getExtras().getBoolean("KILL");
        Intent i = new Intent(context, RingToneService.class);
        Log.i(TAG, "Called onReceive");
        if(close == false) {
            context.startService(i);
            Intent lockScreenIntent = new Intent(context, AlarmLockScreenActivity.class);
//            lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lockScreenIntent);
            Log.i(TAG, "Playing ringtone");
        } else if (close == true) {
            context.stopService(i);
            Log.i(TAG, "Stopping ringtone");
        }

    }
}
