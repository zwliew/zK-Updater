package zwliew.kernel.services;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.androguide.cmdprocessor.CMDProcessor;

import zwliew.kernel.Store;
import zwliew.kernel.fragments.BackupFragment;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                Store.PREFERENCES_FILE, Context.MODE_PRIVATE);
        long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        if (Store.downloadReference == reference &&
                sharedPrefs.getBoolean(Store.AUTO_FLASH, true)) {
            Cursor cursor = ((DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE)).query(new DownloadManager.Query()
                    .setFilterById(reference));
            if (cursor.moveToFirst() &&
                    DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(cursor.getColumnIndex(
                            DownloadManager.COLUMN_STATUS))) {
                final String savedFilePath = cursor.getString(cursor.getColumnIndex(
                        DownloadManager.COLUMN_LOCAL_FILENAME));

                if (sharedPrefs.getBoolean(Store.BACKUP_FLASH, true)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            new BackupFragment.backupKernelTask(context).execute("");

                            if (Store.DEVICE_MODEL.equals("ghost")) {
                                CMDProcessor.runSuCommand("echo '--update_package=" + savedFilePath +
                                        "' > /cache/recovery/command" + "\n" + Store.REBOOT_RECOVERY_CMD);
                            } else {
                                CMDProcessor.runSuCommand("dd if=" + savedFilePath +
                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot" + "\n" +
                                        Store.REBOOT_CMD);
                            }
                        }
                    }).start();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (Store.DEVICE_MODEL.equals("ghost")) {
                                CMDProcessor.runSuCommand("echo '--update_package=" + savedFilePath +
                                        "' > /cache/recovery/command" + "\n" + Store.REBOOT_RECOVERY_CMD);
                            } else {
                                CMDProcessor.runSuCommand("dd if=" + savedFilePath +
                                        " of=/dev/block/platform/msm_sdcc.1/by-name/boot" + "\n" +
                                        Store.REBOOT_CMD);
                            }
                        }
                    }).start();
                }
            }
        }
    }
}
