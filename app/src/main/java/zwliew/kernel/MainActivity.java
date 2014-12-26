package zwliew.kernel;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.androguide.cmdprocessor.CMDProcessor;
import com.androguide.cmdprocessor.Helpers;

import zwliew.kernel.fragments.AboutFragment;
import zwliew.kernel.fragments.UpdaterFragment;

/**
 * The Activity extends ActionBarActivity because that's a requirement per the new API for it to
 * bound with the Toolbar. I suggest you to read Chris Banes (Google programmer) blog about this
 */
public class MainActivity extends ActionBarActivity implements NavigationDrawerCallbacks {

    public static final String ARG_SECTION_NUMBER = "section_number";
    public static boolean isBusybox = true;
    public static boolean isRoot = true;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * As per new Support API we can use a Toolbar instead of a ActionBar. The big difference
         * functionality wise is that the Toolbar is part of the View hierarchy of your layout
         * and because of that you have full control over it like a normal View,
         * which is pretty useful. You can also use a normal Navigation Drawer with it,
         * but for that you should consult API details as it's not the purpose of this app.
         */
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
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
            isRoot = false;
        } else if (!Helpers.checkBusybox()) {
            Toast.makeText(getApplicationContext(),
                    "No busybox! Some functions won't work.",
                    Toast.LENGTH_SHORT).show();
            isBusybox = false;
        }


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
                        .replace(R.id.container, AboutFragment.newInstance(position))
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
                    title.setText(getString(R.string.about_title));
                break;
            default:
                break;
        }
    }

}
