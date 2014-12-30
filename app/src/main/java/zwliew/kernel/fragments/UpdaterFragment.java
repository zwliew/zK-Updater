package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import zwliew.kernel.util.IabHelper;
import zwliew.kernel.util.IabResult;
import zwliew.kernel.util.Purchase;

public class UpdaterFragment extends Fragment {
    private static String latestRelease;
    private static TextView newVerTV;
    private static TextView curVerTV;
    private static SwipeRefreshLayout swipeLayout;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

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

                new UpdaterFragment.getKernelInfo().execute();

                if (networkInfo != null && networkInfo.isConnected()) {
                    new UpdaterFragment.getURLContent().execute();
                } else {
                    swipeLayout.setRefreshing(false);
                    Toast.makeText(getActivity(),
                            getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
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
                    getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
        }

        return rootView;
    }

    @OnClick(R.id.updater_support_donate)
    void donateMe() {
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
            webView.loadUrl(Store.CHANGELOG_URL);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);

                    return true;
                }
            });

            new MaterialDialog.Builder(getActivity())
                    .title(getString(R.string.updater_new_changelog))
                    .customView(webView, false)
                    .show();
        } else {
            Toast.makeText(getActivity(),
                    getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
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
                    getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
        }
    }

    public static class getURLContent extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
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
            if (latestRelease == null) {
                newVerTV.setText("Unknown");
            } else {
                if (editor != null)
                    editor.putInt(Store.NEW_KERNEL,
                            Integer.valueOf(latestRelease.substring(1))).apply();

                if (newVerTV != null)
                    newVerTV.setText(latestRelease);
            }

            if (swipeLayout != null)
                swipeLayout.setRefreshing(false);
        }
    }

    public static class getKernelInfo extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String[] version = new String[2];
            version[0] = CMDProcessor.runShellCommand(Store.SYSTEM_INFO_CMD).getStdout();

            if (!version[0].contains(Store.IS_ZWLIEW_KERNEL))
                return Store.NOT_ZWLIEW_KERNEL;

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
            if (editor != null) {
                if (curVersion.equals(Store.NOT_ZWLIEW_KERNEL)) {
                    editor.putInt(Store.CUR_KERNEL, -1).apply();
                    curVerTV.setText(curVersion);
                } else {
                    editor.putInt(Store.CUR_KERNEL, Integer.valueOf(curVersion)).apply();
                    curVerTV.setText("r" + curVersion);
                }
            }
        }
    }
}
