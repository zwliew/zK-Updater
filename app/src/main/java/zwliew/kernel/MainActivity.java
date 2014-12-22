package zwliew.kernel;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.androguide.cmdprocessor.CMDProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import zwliew.zwliew_kernelupdater.FloatingActionButton;

/**
 * The Activity extends ActionBarActivity because that's a requirement per the new API for it to
 * bound with the Toolbar. I suggest you to read Chris Banes (Google programmer) blog about this
 */
public class MainActivity extends ActionBarActivity {

    /**
     * It's good practice to declare immutable variables in upper case
     */
    private final String DEVICE_MODEL = Build.DEVICE;
    private final String SERVER_URL = "http://128.199.239.125/";
    private final String LATEST_RELEASE_URL = SERVER_URL + DEVICE_MODEL + "/appfiles/latest";
    private final String DOWNLOAD_URL = SERVER_URL + DEVICE_MODEL
            + "/releases/zwliew_Kernel-" + DEVICE_MODEL + "-";
    private final String CHANGELOG_URL = SERVER_URL + DEVICE_MODEL + "/appfiles/changelog.html";

    private String latestRelease;

    private TextView newVerTV, curVerTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new getRootAvail().execute();

        /**
         * As per new Support API we can use a Toolbar instead of a ActionBar. The big difference
         * functionality wise is that the Toolbar is part of the View hierarchy of your layout
         * and because of that you have full control over it like a normal View,
         * which is pretty useful. You can also use a normal Navigation Drawer with it,
         * but for that you should consult API details as it's not the purpose of this app.
         */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FloatingActionButton fabButton = new FloatingActionButton.Builder(this)
                .withDrawable(getResources().getDrawable(R.drawable.ic_download))
                .withButtonColor(getResources().getColor(R.color.accent))
                .withGravity(Gravity.BOTTOM | Gravity.END)
                .withMargins(0, 0, 8, 8)
                .create();

        curVerTV = (TextView) findViewById(R.id.updater_cur_desc);
        newVerTV = (TextView) findViewById(R.id.updater_new_desc);
        ImageButton newLogBtn = (ImageButton) findViewById(R.id.updater_new_log);

        new getURLContent().execute();
        new getKernelInfo().execute();

        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connMgr =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    Uri uri = Uri.parse(DOWNLOAD_URL + latestRelease + ".zip");
                    DownloadManager.Request r = new DownloadManager.Request(uri);
                    // This put the download in the same Download dir the browser uses
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "zwliew_Kernel-" + DEVICE_MODEL + "-" + latestRelease + ".zip");
                    r.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    DownloadManager dm =
                            (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(r);

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Do you want to reboot to recovery?")
                            .setTitle("Reboot recovery?")
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // Reboot if user pressed "OK"
                                            ProgressDialog progress =
                                                    new ProgressDialog(MainActivity.this);
                                            progress.setMessage("Rebooting to recovery");
                                            progress.setIndeterminate(true);
                                            progress.setCancelable(false);
                                            progress.show();

                                            // Start reboot
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // Reboot
                                                    CMDProcessor.runSuCommand("reboot recovery");
                                                }
                                            }).start();
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // Just cancel if user pressed "Cancel"
                                        }
                                    })
                            .create()
                            .show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "No network connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        newLogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Latest Changelog");
                WebView wv = new WebView(MainActivity.this);
                wv.loadUrl(CHANGELOG_URL);
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);

                        return true;
                    }
                });

                alert.setView(wv);
                alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                alert.show();
            }
        });
    }

    private class getRootAvail extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return (CMDProcessor.canSU());
        }

        protected void onPostExecute(Boolean isRoot) {
            if (!isRoot)
                Toast.makeText(MainActivity.this,
                        "Root is not available!", Toast.LENGTH_LONG).show();
        }

    }

    private class getURLContent extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            URL url;

            try {
                // get URL content
                url = new URL(LATEST_RELEASE_URL);
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
            newVerTV.setText(latestRelease);
        }
    }

    private class getKernelInfo extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String[] version = new String[2];
            version[0] = CMDProcessor.runShellCommand("uname -r").getStdout();
            version[1] = String.valueOf(version[0].charAt(23));
            version[0] = String.valueOf(version[0].charAt(22));

            try {
                Integer.parseInt(version[1]);
            } catch (NumberFormatException e) {
                return version[0];
            }

            return version[0] + version[1];
        }

        protected void onPostExecute(String curVersion) {
            curVerTV.setText("r" + curVersion);
        }
    }
}
