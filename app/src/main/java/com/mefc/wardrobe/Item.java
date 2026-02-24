package com.mefc.wardrobe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Item {
    private long id;
    private String name;
    private String description;
    private long boughtTime;
    private long lastWearTime;
    private String imagePath;
    private byte[] imageData;
    private List<String> tags;

    public Item(long id, String name, String description, long boughtTime, long lastWearTime, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.boughtTime = boughtTime;
        this.lastWearTime = lastWearTime;
        this.imagePath = imagePath;
    }

    // Getters
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getBoughtTime() { return boughtTime; }
    public long getLastWearTime() { return lastWearTime; }
    public String getImagePath() { return imagePath; }
    public List<String> getTags() { return tags; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    // Setters
    public void setTags(List<String> tags) { this.tags = tags; }

    // Formatted date strings
    public String getFormattedBoughtTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(boughtTime));
    }

    public String getFormattedLastWearTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastWearTime));
    }
}
