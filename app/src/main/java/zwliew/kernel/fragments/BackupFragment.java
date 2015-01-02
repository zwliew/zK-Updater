package zwliew.kernel.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.androguide.cmdprocessor.CMDProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import zwliew.kernel.BackupItem;
import zwliew.kernel.BackupListAdapter;
import zwliew.kernel.R;
import zwliew.kernel.Store;

public class BackupFragment extends Fragment {
    private static Toolbar toolbar;
    private static RecyclerView backupList;
    private static List<BackupItem> items = new ArrayList<>();
    private static List<BackupItem> backupItemList;
    private static BackupListAdapter adapter;

    @InjectView(R.id.backup_swipe_container)
    SwipeRefreshLayout swipeLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_backup, container, false);
        ButterKnife.inject(this, rootView);

        backupList = (RecyclerView) rootView.findViewById(R.id.backup_list);

        items.add(new BackupItem(getString(R.string.no_backup_file), Store.BACKUP_DIR));
        backupItemList = items;
        adapter = new BackupListAdapter(backupItemList);
        backupList.setAdapter(adapter);
        backupList.setLayoutManager(new LinearLayoutManager(getActivity()));
        backupList.setItemAnimator(new DefaultItemAnimator());
        backupList.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                swipeLayout.setEnabled((recyclerView.getChildCount() == 0 ? 0 : recyclerView.getChildAt(0).getTop()) >= 0);
            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        swipeLayout.setColorSchemeResources(R.color.green,
                R.color.red,
                R.color.blue,
                R.color.orange);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                    }
                }, 200);

                new getBackupCount().execute();
            }
        });

        new getBackupCount().execute();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @OnClick(R.id.backup_button)
    void backupKernel() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.backup_dialog_title)
                .customView(R.layout.edittext_dialog, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        EditText backupName = (EditText) dialog.findViewById(R.id.backup_name);
                        if (backupName.getText().toString().contains(" ")) {
                            Toast.makeText(getActivity(), R.string.invalid_character, Toast.LENGTH_SHORT).show();
                        } else {
                            new Handler().post(new Runnable() {
                                @Override
                                public void run() {
                                    swipeLayout.setRefreshing(true);
                                }
                            });

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    swipeLayout.setRefreshing(false);
                                }
                            }, 2350);

                            new backupKernelTask(getActivity()).execute(backupName.getText().toString());
                            new getBackupCount().execute();
                        }

                    }
                })
                .show();
    }

    public static class backupKernelTask extends AsyncTask<String, Void, Void> {

        public Context context;

        public backupKernelTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... strings) {
            if ("".equals(strings[0])) {
                String kernel = "zwliew_Kernel-r" + String.valueOf(context.getSharedPreferences(
                        Store.PREFERENCES_FILE, Context.MODE_PRIVATE).getInt(Store.CUR_KERNEL, 0));

                CMDProcessor.runSuCommand("dd if=/dev/block/platform/msm_sdcc.1/by-name/boot" +
                        " of=" + Store.BACKUP_DIR + kernel + ".img");
            } else {
                CMDProcessor.runSuCommand("dd if=/dev/block/platform/msm_sdcc.1/by-name/boot" +
                        " of=" + Store.BACKUP_DIR + strings[0] + ".img");
            }

            return null;
        }
    }

    public static class getBackupCount extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            int backupCount = 0;
            File file;
            File folder = new File(Store.BACKUP_DIR);
            String[] fileNames = folder.list();

            items.clear();

            if (!folder.exists() || fileNames.length == 0) {
                folder.mkdir();
                items.add(new BackupItem("No backups found", String.valueOf(folder)));

                return backupCount;
            }

            for (String fileName : fileNames) {
                if (fileName.substring(fileName.length() - 4).equals(".img")) {
                    backupCount++;

                    file = new File(folder, fileName);
                    items.add(new BackupItem(fileName, String.valueOf(file.length() / 1048576) + " MB"));
                }
            }

            backupItemList = items;

            return backupCount;
        }

        protected void onPostExecute(Integer backupCount) {
            if (backupCount == 1)
                toolbar.setSubtitle(String.valueOf(backupCount) + " backup");
            else
                toolbar.setSubtitle(String.valueOf(backupCount) + " backups");

            if (backupList != null)
                backupList.setAdapter(adapter);
        }
    }
}
