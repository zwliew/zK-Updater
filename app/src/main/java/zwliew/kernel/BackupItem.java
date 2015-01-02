package zwliew.kernel;

public class BackupItem {
    private String title;
    private String desc;

    public BackupItem(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }
}
