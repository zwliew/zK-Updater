package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import zwliew.kernel.MainActivity;
import zwliew.kernel.R;
import zwliew.kernel.Store;

public class BackupFragment extends Fragment {
    public static BackupFragment newInstance(int sectionNumber) {
        BackupFragment fragment = new BackupFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_backup, container, false);

        return rootView;
    }
}
