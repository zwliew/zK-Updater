package zwliew.kernel;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
    private List<BackupItem> mData;

    public BackupListAdapter(List<BackupItem> mData) {
        this.mData = mData;
    }

    @Override
    public BackupListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View backupItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_item_row, null);
        return new ViewHolder(backupItemView);
    }

    @Override
    public void onBindViewHolder(BackupListAdapter.ViewHolder holder, int position) {
        holder.backupTitle.setText(mData.get(position).getTitle());
        holder.backupDesc.setText(mData.get(position).getDesc());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView backupTitle;
        public TextView backupDesc;

        public ViewHolder(View itemView) {
            super(itemView);
            backupTitle = (TextView) itemView.findViewById(R.id.backup_item_title);
            backupDesc = (TextView) itemView.findViewById(R.id.backup_item_desc);
        }
    }
}
