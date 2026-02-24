package com.mefc.wardrobe;

public class TagInfo {
    private long id;
    private String name;
    private int usageCount;
    private boolean selected;

    public TagInfo(long id, String name, int usageCount) {
        this.id = id;
        this.name = name;
        this.usageCount = usageCount;
        this.selected = false;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getUsageCount() { return usageCount; }
    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) { this.selected = selected; }
    public void toggleSelected() { this.selected = !this.selected; }
}
