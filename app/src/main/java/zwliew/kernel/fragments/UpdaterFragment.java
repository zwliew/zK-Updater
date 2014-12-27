package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;
import com.androguide.cmdprocessor.CMDProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import butterknife.ButterKnife;
import butterknife.OnClick;
import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;

public class UpdaterFragment extends Fragment {

    private static final String DEVICE_MODEL = Build.DEVICE;
    private static final String SERVER_URL = "http://128.199.239.125/";
    private static final String LATEST_RELEASE_URL = SERVER_URL + DEVICE_MODEL + "/appfiles/latest";
    private final String DOWNLOAD_URL = SERVER_URL + DEVICE_MODEL
            + "/releases/zwliew_Kernel-" + DEVICE_MODEL + "-";
    private final String CHANGELOG_URL = SERVER_URL + DEVICE_MODEL + "/appfiles/changelog.html";

    private static String latestRelease;
    private static TextView newVerTV;
    private static TextView curVerTV;
    private static SwipeRefreshLayout swipeLayout;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public static UpdaterFragment newInstance(int sectionNumber) {
        UpdaterFragment fragment = new UpdaterFragment();
        Bundle args = new Bundle();
        args.putInt(Store.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(Store.ARG_SECTION_NUMBER));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_updater, container, false);
        ButterKnife.inject(this, rootView);

        sharedPreferences = getActivity().
                getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(R.color.green,
                R.color.red,
                R.color.blue,
                R.color.orange);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Store.isBusybox)
                    new UpdaterFragment.getKernelInfo().execute();
                new UpdaterFragment.getURLContent().execute();

                if (sharedPreferences.getInt(Store.CUR_KERNEL, 0) <
                        sharedPreferences.getInt(Store.NEW_KERNEL, 0)
                        && sharedPreferences.getInt(Store.CUR_KERNEL, 0) != 0) {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getActivity())
                                    .setSmallIcon(R.drawable.ic_new_release)
                                    .setContentTitle("zK Updater ")
                                    .setContentText("New release!");
                    NotificationManager mNotifyManager = (NotificationManager)
                            getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

                    mNotifyManager.notify(1, mBuilder.build());
                }
            }
        });

        curVerTV = (TextView) rootView.findViewById(R.id.updater_cur_desc);
        newVerTV = (TextView) rootView.findViewById(R.id.updater_new_desc);

        if (Store.isBusybox)
            new UpdaterFragment.getKernelInfo().execute();
        new UpdaterFragment.getURLContent().execute();

        if (sharedPreferences.getInt(Store.CUR_KERNEL, 0) <
                sharedPreferences.getInt(Store.NEW_KERNEL, 0)
                && sharedPreferences.getInt(Store.CUR_KERNEL, 0) != 0) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getActivity())
                            .setSmallIcon(R.drawable.ic_new_release)
                            .setContentTitle("zK Updater ")
                            .setContentText("New release!");
            NotificationManager mNotifyManager = (NotificationManager)
                    getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

            mNotifyManager.notify(1, mBuilder.build());
        }

        return rootView;

    }

    @OnClick(R.id.updater_new_log)
    void viewLog() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle("Changelog");
            WebView wv = new WebView(getActivity());
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
        } else {
            Toast.makeText(getActivity(),
                    "No network connection", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.updater_cur_info)
    void curInfo() {
        new MaterialDialog.Builder(getActivity())
                .title("Current Kernel")
                .content("This is the current version of zwliew_Kernel on your device.")
                .icon(getResources().getDrawable(R.drawable.ic_info))
                .show();
    }

    @OnClick(R.id.updater_new_info)
    void newInfo() {
        new MaterialDialog.Builder(getActivity())
                .title("Newest Kernel")
                .content("This is the latest available" +
                        " version of zwliew_Kernel for your device.")
                .icon(getResources().getDrawable(R.drawable.ic_info))
                .show();
    }

    @OnClick(R.id.download_button)
    void downloadZip() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    (DownloadManager) getActivity()
                            .getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(r);

            if (Store.isRoot) {
                MaterialDialogCompat.Builder builder =
                        new MaterialDialogCompat.Builder(getActivity());
                builder.setMessage("Do you want to reboot to recovery? ONLY select once the " +
                        "download is fully completed.")
                        .setTitle("Reboot recovery?")
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // Reboot if user pressed "OK"
                                        ProgressDialog progress =
                                                new ProgressDialog(getActivity());
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
            }


        } else {
            Toast.makeText(getActivity(),
                    "No network connection", Toast.LENGTH_SHORT).show();
        }
    }

    public static class getURLContent extends AsyncTask<Void, Void, String> {

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
            if (latestRelease == null) {
                newVerTV.setText("Unknown");
            } else {
                if (editor != null)
                    editor.putInt(Store.NEW_KERNEL,
                            Integer.valueOf(latestRelease.substring(1))).apply();
                if (newVerTV != null)
                    newVerTV.setText(latestRelease);
            }

            swipeLayout.setRefreshing(false);
        }
    }

    public static class getKernelInfo extends AsyncTask<Void, Void, String> {

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
            editor.putInt(Store.CUR_KERNEL, Integer.valueOf(curVersion)).apply();
            curVerTV.setText("r" + curVersion);
        }
    }
}
