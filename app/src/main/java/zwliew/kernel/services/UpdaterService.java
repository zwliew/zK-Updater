package zwliew.kernel.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;

import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;
import zwliew.kernel.fragments.UpdaterFragment;

public class UpdaterService extends IntentService {

    public UpdaterService() {
        super("UpdaterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = getApplicationContext().
                getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        NetworkInfo networkInfo = ((ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        new UpdaterFragment.getKernelInfo().execute();

        if (networkInfo != null && networkInfo.isConnected())
            new UpdaterFragment.getURLContent().execute();

        if (sharedPreferences.getInt(Store.CUR_KERNEL, 0) <
                sharedPreferences.getInt(Store.NEW_KERNEL, 0)
                && sharedPreferences.getInt(Store.CUR_KERNEL, 0) != 0
                && sharedPreferences.getInt(Store.NEW_KERNEL, 0) > 0) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_new_release)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.avail_release))
                            .setAutoCancel(true)
                            .setContentIntent(PendingIntent.getActivity(
                                    getApplicationContext(),
                                    0,
                                    new Intent(getApplicationContext(), MainActivity.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            ));
            NotificationManager mNotifyManager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            mNotifyManager.notify(1, mBuilder.build());
        }
    }
}