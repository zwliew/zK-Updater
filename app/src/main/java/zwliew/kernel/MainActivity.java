package zwliew.kernel;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.androguide.cmdprocessor.CMDProcessor;
import com.androguide.cmdprocessor.Helpers;

import butterknife.ButterKnife;
import zwliew.kernel.fragments.SettingsFragment;
import zwliew.kernel.fragments.UpdaterFragment;
import zwliew.kernel.services.BootReceiver;
import zwliew.kernel.util.IabHelper;
import zwliew.kernel.util.IabResult;
import zwliew.kernel.util.Inventory;

/**
 * The Activity extends ActionBarActivity because that's a requirement per the new API for it to
 * bound with the Toolbar. I suggest you to read Chris Banes (Google programmer) blog about this
 */
public class MainActivity extends ActionBarActivity implements NavigationDrawerCallbacks {

    public static IabHelper mHelper;
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            if (mHelper == null)
                return;

            if (result.isFailure())
                Log.d(Store.TAG, "Failed to query inventory: " + result);
        }
    };
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        /**
         * As per new Support API we can use a Toolbar instead of a ActionBar. The big difference
         * functionality wise is that the Toolbar is part of the View hierarchy of your layout
         * and because of that you have full control over it like a normal View,
         * which is pretty useful. You can also use a normal Navigation Drawer with it,
         * but for that you should consult API details as it's not the purpose of this app.
         */
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.fragment_drawer);
        mNavigationDrawerFragment.setup(R.id.fragment_drawer,
                (DrawerLayout) findViewById(R.id.drawer), mToolbar);

        // Check for root. If none, warn the user.
        if (!CMDProcessor.canSU()) {
            Toast.makeText(getApplicationContext(),
                    "Not rooted! Some functions won't work.",
                    Toast.LENGTH_SHORT).show();
            Store.isRoot = false;
        } else {
            Store.isRoot = true;

            if (!Helpers.checkBusybox()) {
                Toast.makeText(getApplicationContext(),
                        "No busybox! Some functions won't work.",
                        Toast.LENGTH_SHORT).show();
                Store.isBusybox = false;
            } else {
                Store.isBusybox = true;
            }
        }

        SharedPreferences sharedPref =
                this.getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);

        if (sharedPref.getBoolean(Store.AUTO_CHECK, true))
            BootReceiver.scheduleAlarms(this);

        mHelper = new IabHelper(this,
                Store.base64EncodedPublicKey0 + Store.base64EncodedPublicKey1);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess())
                    Log.d(Store.TAG, "Problem setting up In-app Billing: " + result);

                if (mHelper == null)
                    return;

                mHelper.queryInventoryAsync(mGotInventoryListener);
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
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, UpdaterFragment.newInstance(position))
                        .commit();
                break;
            case 1:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, SettingsFragment.newInstance(position))
                        .commit();
                break;
            default:
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                TextView title = (TextView) findViewById(R.id.toolbar_title);
                if (title != null)
                    title.setText(getString(R.string.updater_title));
                break;
            case 1:
                title = (TextView) findViewById(R.id.toolbar_title);
                if (title != null)
                    title.setText(getString(R.string.settings_title));
                break;
            default:
                break;
        }
    }

}
