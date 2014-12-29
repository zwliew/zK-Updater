package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.androguide.cmdprocessor.CMDProcessor;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.EventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;
import zwliew.kernel.util.IabHelper;
import zwliew.kernel.util.IabResult;
import zwliew.kernel.util.Purchase;

public class UpdaterFragment extends Fragment {

    private static final String DEVICE_MODEL = Build.DEVICE;
    private static final String LATEST_RELEASE_URL = Store.SERVER_URL +
            DEVICE_MODEL + "/appfiles/latest";
    private final String DOWNLOAD_URL = Store.SERVER_URL + DEVICE_MODEL
            + "/releases/zwliew_Kernel-" + DEVICE_MODEL + "-";
    private final String CHANGELOG_URL = Store.SERVER_URL +
            DEVICE_MODEL + "/appfiles/changelog.html";

    private static String latestRelease;
    private static TextView newVerTV;
    private static TextView curVerTV;
    private static SwipeRefreshLayout swipeLayout;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    @InjectView(R.id.download_button)
    FloatingActionButton fabButton;
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {

            if (MainActivity.mHelper == null)
                return;

            if (result.isFailure())
                Log.d(Store.TAG, "Error while consuming: " + result);
        }
    };
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (MainActivity.mHelper == null)
                return;

            if (result.isFailure()) {
                Log.d(Store.TAG, "Error purchasing: " + result);
                return;
            }

            MainActivity.mHelper.consumeAsync(purchase, mConsumeFinishedListener);
        }
    };

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
        swipeLayout.setColorSchemeResources(R.color.green,
                R.color.red,
                R.color.blue,
                R.color.orange);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

                if (Store.isBusybox)
                    new UpdaterFragment.getKernelInfo().execute();

                if (networkInfo != null && networkInfo.isConnected()) {
                    new UpdaterFragment.getURLContent().execute();
                } else {
                    swipeLayout.setRefreshing(false);
                    SnackbarManager.show(Snackbar.with(getActivity())
                                    .text(getString(R.string.no_connection))
                                    .eventListener(new EventListener() {
                                        @Override
                                        public void onShow(Snackbar snackbar) {
                                            fabButton.animate().y(130);
                                        }

                                        @Override
                                        public void onShown(Snackbar snackbar) {

                                        }

                                        @Override
                                        public void onDismiss(Snackbar snackbar) {
                                            fabButton.animate().y(225);
                                        }

                                        @Override
                                        public void onDismissed(Snackbar snackbar) {

                                        }
                                    })
                    );
                }

                if (sharedPreferences.getInt(Store.CUR_KERNEL, 0) <
                        sharedPreferences.getInt(Store.NEW_KERNEL, 0)
                        && sharedPreferences.getInt(Store.CUR_KERNEL, 0) > 0
                        && sharedPreferences.getInt(Store.NEW_KERNEL, 0) > 0) {
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

        swipeLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(true);
            }
        });

        curVerTV = (TextView) rootView.findViewById(R.id.updater_cur_desc);
        newVerTV = (TextView) rootView.findViewById(R.id.updater_new_desc);

        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (Store.isBusybox)
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
            SnackbarManager.show(Snackbar.with(getActivity())
                            .text(getString(R.string.no_connection))
                            .eventListener(new EventListener() {
                                @Override
                                public void onShow(Snackbar snackbar) {
                                    fabButton.animate().y(130);
                                }

                                @Override
                                public void onShown(Snackbar snackbar) {

                                }

                                @Override
                                public void onDismiss(Snackbar snackbar) {
                                    fabButton.animate().y(225);
                                }

                                @Override
                                public void onDismissed(Snackbar snackbar) {

                                }
                            })
            );
        }

        if (sharedPreferences.getInt(Store.CUR_KERNEL, 0) <
                sharedPreferences.getInt(Store.NEW_KERNEL, 0)
                && sharedPreferences.getInt(Store.CUR_KERNEL, 0) > 0
                && sharedPreferences.getInt(Store.NEW_KERNEL, 0) > 0) {
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

    @OnClick(R.id.updater_support_donate)
    void donateMe() {
        if (MainActivity.mHelper == null) {
            MainActivity.mHelper = new IabHelper(getActivity(),
                    Store.base64EncodedPublicKey0 + Store.base64EncodedPublicKey1);
            MainActivity.mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess())
                        Log.d(Store.TAG, "Problem setting up In-app Billing: " + result);
                }
            });
        }

        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.updater_support_donate))
                .items(getResources().getStringArray(R.array.donate_items))
                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog,
                                            View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                        Store.SKU_COFFEE, Store.RC_REQUEST,
                                        mPurchaseFinishedListener, Store.PAYLOAD + "a");
                                break;
                            case 1:
                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                        Store.SKU_MCDONALDS, Store.RC_REQUEST,
                                        mPurchaseFinishedListener, Store.PAYLOAD + "s");
                                break;
                            case 2:
                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                        Store.SKU_BUS, Store.RC_REQUEST,
                                        mPurchaseFinishedListener, Store.PAYLOAD + "d");
                                break;
                            case 3:
                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                        Store.SKU_ELECTRICITY, Store.RC_REQUEST,
                                        mPurchaseFinishedListener, Store.PAYLOAD + "f");
                                break;
                            default:
                                break;

                        }

                    }
                })
                .positiveText(android.R.string.ok)
                .show();
    }

    @OnClick(R.id.updater_support_share)
    void shareKernel() {
        Intent shareIntent = new Intent()
                .setAction(android.content.Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_title))
                .putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share_desc));

        if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null)
            startActivity(Intent.createChooser(shareIntent,
                    getResources().getString(R.string.share_chooser_title)));
        else
            Toast.makeText(getActivity(), R.string.app_not_available, Toast.LENGTH_SHORT).show();


    }

    @OnClick(R.id.updater_support_info)
    void supportInfo() {
        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.updater_support_title))
                .content(getString(R.string.updater_support_info))
                .icon(getResources().getDrawable(R.drawable.ic_info))
                .show();
    }

    @OnClick(R.id.updater_new_log)
    void viewLog() {
        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl(CHANGELOG_URL);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);

                    return true;
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .title(getString(R.string.updater_new_changelog))
                    .customView(webView)
                    .show();
        } else {
            SnackbarManager.show(Snackbar.with(getActivity())
                            .text(getString(R.string.no_connection))
                            .eventListener(new EventListener() {
                                @Override
                                public void onShow(Snackbar snackbar) {
                                    fabButton.animate().y(130);
                                }

                                @Override
                                public void onShown(Snackbar snackbar) {

                                }

                                @Override
                                public void onDismiss(Snackbar snackbar) {
                                    fabButton.animate().y(225);
                                }

                                @Override
                                public void onDismissed(Snackbar snackbar) {

                                }
                            })
            );
        }
    }

    @OnClick(R.id.updater_cur_info)
    void curInfo() {
        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.updater_cur_title))
                .content(getString(R.string.updater_cur_info))
                .icon(getResources().getDrawable(R.drawable.ic_info))
                .show();
    }

    @OnClick(R.id.updater_new_info)
    void newInfo() {
        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.updater_new_title))
                .content(getString(R.string.updater_new_info))
                .icon(getResources().getDrawable(R.drawable.ic_info))
                .show();
    }

    @OnClick(R.id.download_button)
    void downloadZip() {
        NetworkInfo networkInfo = ((ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
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
                new MaterialDialog.Builder(getActivity())
                        .content(getString(R.string.confirm_recovery_desc))
                        .title(getString(R.string.confirm_recovery_title))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                ProgressDialog progress =
                                        new ProgressDialog(getActivity());
                                progress.setMessage(getString(R.string.progress_recovery));
                                progress.setIndeterminate(true);
                                progress.setCancelable(false);
                                progress.show();

                                // Start reboot
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Reboot
                                        CMDProcessor.runSuCommand(Store.REBOOT_RECOVERY_CMD);
                                    }
                                }).start();
                            }
                        })
                        .show();
            }


        } else {
            SnackbarManager.show(Snackbar.with(getActivity())
                            .text(getString(R.string.no_connection))
                            .eventListener(new EventListener() {
                                @Override
                                public void onShow(Snackbar snackbar) {
                                    fabButton.animate().y(130);
                                }

                                @Override
                                public void onShown(Snackbar snackbar) {

                                }

                                @Override
                                public void onDismiss(Snackbar snackbar) {
                                    fabButton.animate().y(225);
                                }

                                @Override
                                public void onDismissed(Snackbar snackbar) {

                                }
                            })
            );
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
            version[0] = CMDProcessor.runShellCommand(Store.SYSTEM_INFO_CMD).getStdout();
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
