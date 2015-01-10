package zwliew.kernel;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.androguide.cmdprocessor.CMDProcessor;

import java.io.File;
import java.util.List;

import zwliew.kernel.fragments.BackupFragment;

public class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
    private List<BackupItem> mData;

    public BackupListAdapter(List<BackupItem> data) {
        mData = data;
    }

    @Override
    public BackupListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View backupItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_item_row, null);
        return new ViewHolder(backupItemView);
    }

    @Override
    public void onBindViewHolder(BackupListAdapter.ViewHolder holder, int position) {
        try {
            holder.backupTitle.setText(mData.get(position).getTitle());
            holder.backupDesc.setText(mData.get(position).getDesc());
        } catch (IndexOutOfBoundsException e) {

        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView backupTitle;
        public TextView backupDesc;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            backupTitle = (TextView) itemView.findViewById(R.id.backup_item_title);
            backupDesc = (TextView) itemView.findViewById(R.id.backup_item_desc);
        }

        @Override
        public void onClick(View view) {
            if (backupTitle.getText().equals(
                    itemView.getContext().getString(R.string.no_backup_file)))
                return;

            new MaterialDialog.Builder(itemView.getContext())
                    .title(backupTitle.getText())
                    .titleColor(itemView.getResources().getColor(R.color.accent))
                    .items(R.array.kernel_backup_items)
                    .negativeText(android.R.string.cancel)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            switch (which) {
                                case 0:
                                    new MaterialDialog.Builder(itemView.getContext())
                                            .title(R.string.restore_title)
                                            .content(R.string.restore_desc)
                                            .positiveText(R.string.agree)
                                            .negativeText(R.string.disagree)
                                            .neutralText(android.R.string.cancel)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (Store.DEVICE_MODEL.equals("ghost")) {
                                                                // TODO: Add proper support for modules
                                                                CMDProcessor.runSuCommand("dd if=" + Store.BACKUP_DIR + backupTitle.getText() +
                                                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot" + "\n" +
                                                                        Store.REBOOT_CMD);
                                                            } else {
                                                                CMDProcessor.runSuCommand("dd if=" + Store.BACKUP_DIR + backupTitle.getText() +
                                                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot" + "\n" +
                                                                        Store.REBOOT_CMD);
                                                            }
                                                        }
                                                    }).start();
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    super.onNegative(dialog);

                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (Store.DEVICE_MODEL.equals("ghost")) {
                                                                // TODO: Add proper support for modules
                                                                CMDProcessor.runSuCommand("dd if=" + Store.BACKUP_DIR + backupTitle.getText() +
                                                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot");
                                                            } else {
                                                                CMDProcessor.runSuCommand("dd if=" + Store.BACKUP_DIR + backupTitle.getText() +
                                                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot");
                                                            }
                                                        }
                                                    }).start();
                                                }

                                                @Override
                                                public void onNeutral(MaterialDialog dialog) {
                                                    super.onNeutral(dialog);
                                                }
                                            })
                                            .show();
                                    break;
                                case 1:
                                    new MaterialDialog.Builder(itemView.getContext())
                                            .title(R.string.rename_title)
                                            .customView(R.layout.edittext_dialog, false)
                                            .positiveText(android.R.string.ok)
                                            .negativeText(android.R.string.cancel)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);

                                                    EditText backupName = (EditText) dialog.findViewById(R.id.backup_name);
                                                    File currentFile = new File(Store.BACKUP_DIR + backupTitle.getText().toString());
                                                    File newFile = new File(Store.BACKUP_DIR + backupName.getText().toString().replace(" ", "_") + ".img");
                                                    currentFile.renameTo(newFile);

                                                    new BackupFragment.getBackupCount().execute();
                                                }
                                            })
                                            .show();
                                    break;
                                case 2:
                                    File toDelete = new File(Store.BACKUP_DIR + backupTitle.getText().toString());
                                    toDelete.delete();

                                    new BackupFragment.getBackupCount().execute();
                                    break;
                                default:
                                    break;
                            }
                        }
                    })
                    .show();
        }
    }
}
