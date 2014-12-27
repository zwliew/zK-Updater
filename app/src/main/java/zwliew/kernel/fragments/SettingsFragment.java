package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.OnClick;
import zwliew.kernel.BootReceiver;
import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;

/**
 * Created by ZhaoWei on 23/12/2014.
 */
public class SettingsFragment extends Fragment {

    private CheckBox autoCheckCB;

    public static SettingsFragment newInstance(int sectionNumber) {
        SettingsFragment fragment = new SettingsFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView busyboxTV = (TextView) rootView.findViewById(R.id.busybox_status);
        TextView rootTV = (TextView) rootView.findViewById(R.id.root_status);
        autoCheckCB = (CheckBox) rootView.findViewById(R.id.auto_check);
        RelativeLayout autoCheckTV = (RelativeLayout) rootView.findViewById(R.id.auto_check_layout);

        SharedPreferences sharedPref =
                getActivity().getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        autoCheckCB.setChecked(sharedPref.getBoolean(Store.AUTO_CHECK, true));

        if (Store.isBusybox)
            busyboxTV.setText("Available");
        else
            busyboxTV.setText("Unavailable");

        if (Store.isRoot)
            rootTV.setText("Available");
        else
            rootTV.setText("Unavailable");

        autoCheckTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoCheckCB.isChecked())
                    autoCheckCB.setChecked(false);
                else
                    autoCheckCB.setChecked(true);

                SharedPreferences sharedPrefs = getActivity().
                        getSharedPreferences(Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean(Store.AUTO_CHECK, autoCheckCB.isChecked()).apply();

                if (autoCheckCB.isChecked()) {
                    BootReceiver.scheduleAlarms(getActivity());
                }
            }
        });
        return rootView;
    }

    @OnClick(R.id.about)
    void goToProfile() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://plus.google.com/+ZhaoWeiLiew"));
        startActivity(i);
    }

}
