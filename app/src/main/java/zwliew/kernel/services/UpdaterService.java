package zwliew.kernel.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.androguide.cmdprocessor.CMDProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;

public class UpdaterService extends IntentService {

    private SharedPreferences.Editor editor;

    public UpdaterService() {
        super("UpdaterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = getApplicationContext().
                getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        NetworkInfo networkInfo = ((ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        new getKernelInfo().execute();

        if (networkInfo != null && networkInfo.isConnected())
            new getURLContent().execute();

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

    private class getURLContent extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String latestRelease = null;
            URL url;

            try {
                // get URL content
                url = new URL(Store.LATEST_RELEASE_URL);
                URLConnection conn = url.openConnection();

                // open the stream and put it into BufferedReader
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                latestRelease = br.readLine();

                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return latestRelease;
        }

        protected void onPostExecute(String latestRelease) {
            if (latestRelease != null) {
                editor.putInt(Store.NEW_KERNEL,
                        Integer.valueOf(latestRelease.substring(1))).apply();
            }
        }


    }

    private class getKernelInfo extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String[] version = new String[2];
            version[0] = CMDProcessor.runShellCommand(Store.SYSTEM_INFO_CMD).getStdout();

            if (!version[0].contains(Store.IS_ZWLIEW_KERNEL))
                return Store.NOT_ZWLIEW_KERNEL;

            if (Store.DEVICE_MODEL.equals("ghost")) {
                version[1] = String.valueOf(version[0].charAt(23));
                version[0] = String.valueOf(version[0].charAt(22));
            } else {
                version[1] = String.valueOf(version[0].charAt(22));
                version[0] = String.valueOf(version[0].charAt(21));
            }

            try {
                Integer.parseInt(version[1]);
            } catch (NumberFormatException e) {
                return version[0];
            }

            return version[0] + version[1];
        }

        protected void onPostExecute(String curVersion) {
            if (curVersion.equals(Store.NOT_ZWLIEW_KERNEL))
                editor.putInt(Store.CUR_KERNEL, -1).apply();
            else
                editor.putInt(Store.CUR_KERNEL, Integer.valueOf(curVersion)).apply();
        }
    }
}
