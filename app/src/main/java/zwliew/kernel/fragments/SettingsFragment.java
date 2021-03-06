package zwliew.kernel.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;
import zwliew.kernel.billing.IabHelper;
import zwliew.kernel.billing.IabResult;
import zwliew.kernel.billing.Purchase;
import zwliew.kernel.services.BootReceiver;

public class SettingsFragment extends Fragment {

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
    private SharedPreferences.Editor editor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        final SwitchCompat autoCheckSwitch = (SwitchCompat) rootView.findViewById(R.id.auto_check);
        final SwitchCompat autoFlashSwitch = (SwitchCompat) rootView.findViewById(R.id.auto_flash);
        final SwitchCompat backupFlashSwitch = (SwitchCompat) rootView.findViewById(R.id.backup_flash);
        autoCheckSwitch.setChecked(sharedPreferences.getBoolean(Store.AUTO_CHECK, true));
        autoFlashSwitch.setChecked(sharedPreferences.getBoolean(Store.AUTO_FLASH, true));
        backupFlashSwitch.setChecked(sharedPreferences.getBoolean(Store.BACKUP_FLASH, true));

        RelativeLayout autoCheckLayout = (RelativeLayout) rootView.findViewById(R.id.auto_check_layout);
        autoCheckLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCheckSwitch.setChecked(!autoCheckSwitch.isChecked());

                editor.putBoolean(Store.AUTO_CHECK, autoCheckSwitch.isChecked()).apply();

                if (autoCheckSwitch.isChecked())
                    BootReceiver.scheduleAlarms(getActivity());
            }
        });

        RelativeLayout autoFlashLayout = (RelativeLayout) rootView.findViewById(R.id.auto_flash_layout);
        autoFlashLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoFlashSwitch.setChecked(!autoFlashSwitch.isChecked());

                editor.putBoolean(Store.AUTO_FLASH, autoFlashSwitch.isChecked()).apply();
            }
        });

        RelativeLayout backupFlashLayout = (RelativeLayout) rootView.findViewById(R.id.backup_flash_layout);
        backupFlashLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backupFlashSwitch.setChecked(!backupFlashSwitch.isChecked());

                editor.putBoolean(Store.BACKUP_FLASH, backupFlashSwitch.isChecked()).apply();
            }
        });


        LinearLayout aboutLayout = (LinearLayout) rootView.findViewById(R.id.about);
        aboutLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://plus.google.com/+ZhaoWeiLiew"));
                startActivity(i);
            }
        });

        RelativeLayout emailLayout = (RelativeLayout) rootView.findViewById(R.id.support_email_layout);
        emailLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "zhaoweiliew@gmail.com", null));
                i.putExtra(Intent.EXTRA_SUBJECT, "zwliew_Kernel: ");

                if (i.resolveActivity(getActivity().getPackageManager()) == null)
                    Toast.makeText(getActivity(), R.string.app_not_available, Toast.LENGTH_SHORT).show();
                else
                    startActivity(Intent.createChooser(i, getString(R.string.email_chooser_title)));
            }
        });

        RelativeLayout supportLayout = (RelativeLayout) rootView.findViewById(R.id.support_me_layout);
        supportLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.support_chooser_title)
                        .items(R.array.support_me_items)
                        .negativeText(android.R.string.cancel)
                        .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        Intent shareIntent = new Intent()
                                                .setAction(android.content.Intent.ACTION_SEND)
                                                .setType("text/plain")
                                                .putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_title))
                                                .putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share_desc));

                                        if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null)
                                            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
                                        else
                                            Toast.makeText(getActivity(), R.string.app_not_available, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 1:
                                        new MaterialDialog.Builder(getActivity())
                                                .title(R.string.updater_support_donate)
                                                .items(R.array.donate_items)
                                                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                                                    @Override
                                                    public void onSelection(MaterialDialog dialog,
                                                                            View view, int which, CharSequence text) {
                                                        switch (which) {
                                                            case 0:
                                                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                                                        Store.SKU_LOLLIPOP, Store.RC_REQUEST,
                                                                        mPurchaseFinishedListener, Store.PAYLOAD + "a");
                                                                break;
                                                            case 1:
                                                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                                                        Store.SKU_COFFEE, Store.RC_REQUEST,
                                                                        mPurchaseFinishedListener, Store.PAYLOAD + "s");
                                                                break;
                                                            case 2:
                                                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                                                        Store.SKU_MCDONALDS, Store.RC_REQUEST,
                                                                        mPurchaseFinishedListener, Store.PAYLOAD + "d");
                                                                break;
                                                            case 3:
                                                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                                                        Store.SKU_BUS, Store.RC_REQUEST,
                                                                        mPurchaseFinishedListener, Store.PAYLOAD + "f");
                                                                break;
                                                            case 4:
                                                                MainActivity.mHelper.launchPurchaseFlow(getActivity(),
                                                                        Store.SKU_ELECTRICITY, Store.RC_REQUEST,
                                                                        mPurchaseFinishedListener, Store.PAYLOAD + "g");
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }
                                                })
                                                .positiveText(android.R.string.ok)
                                                .negativeText(android.R.string.cancel)
                                                .show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .show();
            }
        });
        return rootView;
    }
}
