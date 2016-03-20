package com.example.mrokade.asanaalarm;

/**
 * Created by mrokade on 3/21/16.
 */
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by mrokade on 3/3/16.
 */
public class RingToneService extends Service {
    static Ringtone r;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("RINGTONE", "ringtone playing");
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(getBaseContext(), uri);
        r.play();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("RINGTONE", "ringtone playing");
        r.stop();
    }
}