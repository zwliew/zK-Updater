package zwliew.kernel.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import zwliew.kernel.Store;

public class BootReceiver extends BroadcastReceiver {

    public static void scheduleAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UpdaterService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 1234, intent, 0);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(), 86400000, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Store.IS_SUPPORTED) {
            SharedPreferences sharedPref =
                    context.getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);

            if (sharedPref.getBoolean(Store.AUTO_CHECK, true))
                scheduleAlarms(context);
        }
    }
}