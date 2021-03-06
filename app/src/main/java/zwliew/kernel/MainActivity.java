package zwliew.kernel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.androguide.cmdprocessor.Helpers;

import zwliew.kernel.billing.IabHelper;
import zwliew.kernel.billing.IabResult;
import zwliew.kernel.billing.Inventory;
import zwliew.kernel.fragments.BackupFragment;
import zwliew.kernel.fragments.ProcessorFragment;
import zwliew.kernel.fragments.SettingsFragment;
import zwliew.kernel.fragments.UpdaterFragment;
import zwliew.kernel.navigationdrawer.NavigationDrawerCallbacks;
import zwliew.kernel.navigationdrawer.NavigationDrawerFragment;
import zwliew.kernel.services.BootReceiver;

public class MainActivity extends ActionBarActivity implements NavigationDrawerCallbacks {

    public static IabHelper mHelper;

    private Toolbar toolbar;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.fragment_drawer);
        mNavigationDrawerFragment.setup(R.id.fragment_drawer,
                (DrawerLayout) findViewById(R.id.drawer), toolbar);

        if (!Store.IS_SUPPORTED) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.not_supported_title))
                    .content(getString(R.string.not_supported_desc))
                    .positiveText(android.R.string.ok)
                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        } else if (!Helpers.checkReqs()) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.no_requirements_title))
                    .content(getString(R.string.no_requirements_desc))
                    .positiveText(android.R.string.ok)
                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        }

        if (getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE).
                getBoolean(Store.AUTO_CHECK, true) && Store.IS_SUPPORTED)
            BootReceiver.scheduleAlarms(this);

        mHelper = new IabHelper(this,
                Store.base64EncodedPublicKey0 + Store.base64EncodedPublicKey1);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(Store.TAG, "Problem setting up In-app Billing: " + result);

                    return;
                }

                if (mHelper == null)
                    return;

                mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                        if (result.isFailure())
                            Log.d(Store.TAG, "Failed to query inventory: " + result);
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHelper != null)
            mHelper.dispose();

        mHelper = null;


    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen())
            mNavigationDrawerFragment.closeDrawer();
        else
            super.onBackPressed();
    }

    @Override
    public void onNavigationDrawerItemSelected(final int position) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (position) {
                    case 0:
                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, new UpdaterFragment())
                                .commitAllowingStateLoss();
                        toolbar.setTitle(R.string.app_name);
                        toolbar.setSubtitle("Current kernel: " + getString(R.string.unknown_val));
                        break;
                    case 1:
                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, new BackupFragment())
                                .commitAllowingStateLoss();
                        toolbar.setTitle(R.string.backup_title);
                        toolbar.setSubtitle(getString(R.string.no_backup_file));
                        break;
                    case 2:
                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, new SettingsFragment())
                                .commitAllowingStateLoss();
                        toolbar.setTitle(R.string.settings_title);
                        toolbar.setSubtitle(getString(R.string.settings_subtitle));
                        break;
                    case 3:
                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, new ProcessorFragment())
                                .commitAllowingStateLoss();
                        toolbar.setTitle(R.string.processor_title);
                        toolbar.setSubtitle(getString(R.string.processor_subtitle));
                        break;
                    default:
                        break;
                }
            }
        }, 250);
    }
}