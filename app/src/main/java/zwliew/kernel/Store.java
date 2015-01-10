package zwliew.kernel;

import android.os.Build;

public class Store {
    public static final String PREFERENCES_FILE = "zwliew.kernel.my_app_settings";
    public static final String NEW_KERNEL = "zwliew.kernel.new_kernel";
    public static final String CUR_KERNEL = "zwliew.kernel.cur_kernel";
    public static final String AUTO_CHECK = "zwliew.kernel.auto_check";
    public static final String AUTO_FLASH = "zwliew.kernel.auto_flash";
    public static final String BACKUP_FLASH = "zwliew.kernel.backup_flash";

    public static final String base64EncodedPublicKey0 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu08Jbx7oDhkcjdY1fQFLlxos/TSMtBWTVk08rDbGVm+QDnYipe6RdiQFAkoNayHK/G1pb5OLyfUp4PqoxkM1d9t9HPy/96FCDXwdyqbpJ8L0tKCPz1oRa0AzAODPo8YyTSdtwMXOkOozPaPNnCAuXrxctnVtwQT";
    public static final String base64EncodedPublicKey1 = "Up+tkzxwxz/EsY/OBFtvlF4Do8aNFQYuHQ1KIR6BeI9MBy9wHcSr9gdxJ5N5mXA8WoBaOhnKKpTb6d+6KMi5WK9XNpWIXpTmX1y+hmwgIsLAMoLVJH3jbt6Wpz0ZNNUV+Zf6/8QGz1W8+S5Imz1yVDIpBqO3MXfZU/EvUlbgMZaBUvR0SSD+uUwIDAQAB";
    public static final String SKU_LOLLIPOP = "lollipop";
    public static final String SKU_COFFEE = "cup_of_coffee";
    public static final String SKU_BUS = "bus_fare";
    public static final String SKU_ELECTRICITY = "electricity_bill";
    public static final String SKU_MCDONALDS = "mcdonalds_lunch";
    public static final String PAYLOAD = "qwerty12345";
    public static final int RC_REQUEST = 12321;

    public static final String TAG = "zwliew_kernel";

    public static final String SYSTEM_INFO_CMD = "uname -r";
    public static final String REBOOT_RECOVERY_CMD = "reboot recovery";
    public static final String REBOOT_CMD = "reboot";

    public static final String DEVICE_MODEL = Build.DEVICE;
    public static final String ZIP_ENDING = DEVICE_MODEL.equals("ghost") ? ".zip" : ".img";
    public static final String IS_ZWLIEW_KERNEL = "-zwliew_Kernel-r";
    public static final String NOT_ZWLIEW_KERNEL = "Not zwliew_Kernel";
    public static final boolean IS_SUPPORTED = DEVICE_MODEL.equals("ghost") || DEVICE_MODEL.equals("flo");

    public static final String SERVER_URL = "http://128.199.239.125/";
    public static final String LATEST_RELEASE_URL = SERVER_URL + DEVICE_MODEL + "/latest";
    public static final String CHANGELOG_URL = SERVER_URL + DEVICE_MODEL + "/changelog.html";
    public static final String DOWNLOAD_URL = SERVER_URL + DEVICE_MODEL + "/releases/zwliew_Kernel-" + DEVICE_MODEL + "-";

    // TODO: Fix /storage/emulated/0 A.K.A. Environment.getExternalStorageDirectory() not working
    public static final String BACKUP_DIR = "/sdcard/zK_Updater/";

    public static long downloadReference;
}
