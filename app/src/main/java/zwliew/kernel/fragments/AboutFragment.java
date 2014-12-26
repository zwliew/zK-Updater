package zwliew.kernel.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import zwliew.kernel.MainActivity;
import zwliew.kernel.R;

/**
 * Created by ZhaoWei on 23/12/2014.
 */
public class AboutFragment extends Fragment {
    public static AboutFragment newInstance(int sectionNumber) {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        args.putInt(MainActivity.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(MainActivity.ARG_SECTION_NUMBER));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        TextView busyboxTV = (TextView) rootView.findViewById(R.id.busybox_status);
        TextView rootTV = (TextView) rootView.findViewById(R.id.root_status);
        LinearLayout aboutTV = (LinearLayout) rootView.findViewById(R.id.about);

        if (MainActivity.isBusybox)
            busyboxTV.setText("True");
        else
            busyboxTV.setText("False");

        if (MainActivity.isRoot)
            rootTV.setText("True");
        else
            rootTV.setText("False");

        aboutTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://plus.google.com/+ZhaoWeiLiew"));
                startActivity(i);
            }
        });

        return rootView;
    }

}
