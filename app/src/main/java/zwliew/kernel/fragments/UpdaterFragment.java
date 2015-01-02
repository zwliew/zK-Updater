package zwliew.kernel.fragments;

import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.androguide.cmdprocessor.CMDProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import zwliew.kernel.R;
import zwliew.kernel.Store;

public class UpdaterFragment extends Fragment {
    @InjectView(R.id.updater_new_desc)
    TextView newVerTV;
    @InjectView(R.id.updater_swipe_container)
    SwipeRefreshLayout swipeLayout;
    Toolbar toolbar;

    private String latestRelease;
    private SharedPreferences.Editor editor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_updater, container, false);
        ButterKnife.inject(this, rootView);

        swipeLayout.setColorSchemeResources(R.color.green,
                R.color.red,
                R.color.blue,
                R.color.orange);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

                new UpdaterFragment.getKernelInfo().execute();

                if (networkInfo != null && networkInfo.isConnected()) {
                    new UpdaterFragment.getURLContent().execute();
                } else {
                    swipeLayout.setRefreshing(false);
                    Toast.makeText(getActivity(),
                            R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (swipeLayout != null) {
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(true);
                }
            });
        }

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        editor = getActivity().getSharedPreferences(
                Store.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();

        if (Store.IS_SUPPORTED)
            new UpdaterFragment.getKernelInfo().execute();

        if (networkInfo != null && networkInfo.isConnected()) {
            new UpdaterFragment.getURLContent().execute();
        } else {
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(false);
                }
            });
            Toast.makeText(getActivity(),
                    R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @OnClick(R.id.updater_new_log)
    void viewLog() {
        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl(Store.CHANGELOG_URL);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);

                    return true;
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.updater_new_changelog)
                    .customView(webView, false)
                    .show();
        } else {
            Toast.makeText(getActivity(),
                    R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.download_button)
    void downloadKernel() {
        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(Store.DOWNLOAD_URL + latestRelease + Store.ZIP_ENDING))
                    .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "zwliew_Kernel-" + Store.DEVICE_MODEL + "-" + latestRelease + Store.ZIP_ENDING);

            Store.downloadReference = ((DownloadManager) getActivity().
                    getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

        } else {
            Toast.makeText(getActivity(),
                    R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
    }

    private class getURLContent extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            URL url;

            try {
                url = new URL(Store.LATEST_RELEASE_URL);
                URLConnection conn = url.openConnection();

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
                editor.putInt(Store.NEW_KERNEL,
                        Integer.valueOf(latestRelease.substring(1))).apply();

                if (newVerTV != null)
                    newVerTV.setText(latestRelease);
            }

            if (swipeLayout != null)
                swipeLayout.setRefreshing(false);
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
            if (curVersion.equals(Store.NOT_ZWLIEW_KERNEL)) {
                editor.putInt(Store.CUR_KERNEL, -1).apply();
                toolbar.setSubtitle(curVersion);
            } else {
                editor.putInt(Store.CUR_KERNEL, Integer.valueOf(curVersion)).apply();
                toolbar.setSubtitle("zwliew_Kernel-r" + curVersion);
            }
        }
    }
}
