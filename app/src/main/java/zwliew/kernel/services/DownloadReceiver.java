package zwliew.kernel.services;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import zwliew.kernel.R;
import zwliew.kernel.Store;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Store.downloadReference ==
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1))
            Toast.makeText(context,
                    context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show();
    }
}
