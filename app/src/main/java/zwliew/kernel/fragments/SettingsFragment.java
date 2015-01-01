package zwliew.kernel.fragments;

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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import zwliew.kernel.R;
import zwliew.kernel.Store;
import zwliew.kernel.services.BootReceiver;

public class SettingsFragment extends Fragment {

    @InjectView(R.id.auto_check)
    CheckBox autoCheckCB;
    @InjectView(R.id.auto_flash)
    CheckBox autoFlashCB;
    private SharedPreferences.Editor editor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.inject(this, rootView);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        autoCheckCB.setChecked(sharedPreferences.getBoolean(Store.AUTO_CHECK, true));
        autoFlashCB.setChecked(sharedPreferences.getBoolean(Store.AUTO_FLASH, true));

        return rootView;
    }

    @OnClick(R.id.auto_check_layout)
    void autoCheckToggle() {
        if (autoCheckCB.isChecked())
            autoCheckCB.setChecked(false);
        else
            autoCheckCB.setChecked(true);

        editor.putBoolean(Store.AUTO_CHECK, autoCheckCB.isChecked()).apply();

        if (autoCheckCB.isChecked())
            BootReceiver.scheduleAlarms(getActivity());

    }

    @OnClick(R.id.auto_flash_layout)
    void autoFlashToggle() {
        if (autoFlashCB.isChecked())
            autoFlashCB.setChecked(false);
        else
            autoFlashCB.setChecked(true);

        editor.putBoolean(Store.AUTO_FLASH, autoFlashCB.isChecked()).apply();
    }

    @OnClick(R.id.about)
    void goToProfile() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://plus.google.com/+ZhaoWeiLiew"));
        startActivity(i);
    }

}
