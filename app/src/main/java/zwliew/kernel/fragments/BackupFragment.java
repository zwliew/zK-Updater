package zwliew.kernel.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;
import zwliew.kernel.R;

public class BackupFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_backup, container, false);
        ButterKnife.inject(this, rootView);

        return rootView;
    }

    @OnClick(R.id.backup_button)
    void backupKernel() {

    }
}
