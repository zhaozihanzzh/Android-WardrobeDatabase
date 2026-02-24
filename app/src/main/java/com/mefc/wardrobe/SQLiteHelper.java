package com.mefc.wardrobe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Wardrobe.db";
    private static final int DATABASE_VERSION = 1;

    // Table and columns
    private static final String TABLE_ITEMS = "items";
    private static final String COLUMN_ITEM_ID = "id";
    private static final String COLUMN_ITEM_NAME = "name";
    private static final String COLUMN_ITEM_DESCRIPTION = "description";
    private static final String COLUMN_ITEM_BOUGHT_TIME = "bought_time";
    private static final String COLUMN_ITEM_LAST_WEAR_TIME = "last_wear_time";
    private static final String COLUMN_ITEM_IMAGE = "image";

    // Tags table
    private static final String TABLE_TAGS = "tags";
    private static final String COLUMN_TAG_ID = "id";
    private static final String COLUMN_TAG_NAME = "name";

    // Item-Tags junction table (many-to-many)
    private static final String TABLE_ITEM_TAGS = "item_tags";
    private static final String COLUMN_ITEM_TAG_ITEM_ID = "item_id";
    private static final String COLUMN_ITEM_TAG_TAG_ID = "tag_id";
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ITEMS + " (" +
                COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                COLUMN_ITEM_BOUGHT_TIME + " INTEGER, " +
                COLUMN_ITEM_LAST_WEAR_TIME + " INTEGER, " +
                COLUMN_ITEM_DESCRIPTION + " TEXT, " +
                COLUMN_ITEM_IMAGE + " TEXT)";
        db.execSQL(createTable);

        String createTagsTable = "CREATE TABLE " + TABLE_TAGS + " (" +
                COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TAG_NAME + " TEXT NOT NULL UNIQUE)";
        db.execSQL(createTagsTable);

        // Create item-tags junction table
        String createItemTagsTable = "CREATE TABLE " + TABLE_ITEM_TAGS + " (" +
                COLUMN_ITEM_TAG_ITEM_ID + " INTEGER, " +
                COLUMN_ITEM_TAG_TAG_ID + " INTEGER, " +
                "PRIMARY KEY (" + COLUMN_ITEM_TAG_ITEM_ID + ", " + COLUMN_ITEM_TAG_TAG_ID + "), " +
                "FOREIGN KEY (" + COLUMN_ITEM_TAG_ITEM_ID + ") REFERENCES " + TABLE_ITEMS + "(" + COLUMN_ITEM_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY (" + COLUMN_ITEM_TAG_TAG_ID + ") REFERENCES " + TABLE_TAGS + "(" + COLUMN_TAG_ID + ") ON DELETE CASCADE)";
        db.execSQL(createItemTagsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEM_TAGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        onCreate(db);
    }
// ========== Tag Management Methods ==========

    // Get all tags with usage count
    public List<TagInfo> getAllTagsWithUsage() {
        List<TagInfo> tags = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT t." + COLUMN_TAG_ID + ", t." + COLUMN_TAG_NAME + ", " +
                "COUNT(it." + COLUMN_ITEM_TAG_ITEM_ID + ") as usage_count " +
                "FROM " + TABLE_TAGS + " t " +
                "LEFT JOIN " + TABLE_ITEM_TAGS + " it ON t." + COLUMN_TAG_ID + " = it." + COLUMN_ITEM_TAG_TAG_ID + " " +
                "GROUP BY t." + COLUMN_TAG_ID + ", t." + COLUMN_TAG_NAME + " " +
                "ORDER BY t." + COLUMN_TAG_NAME + " ASC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TagInfo tagInfo = new TagInfo(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow("usage_count"))
                );
                tags.add(tagInfo);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tags;
    }

    // Check if a tag is used by any item
    public boolean isTagUsed(long tagId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ITEM_TAGS,
                new String[]{"COUNT(*)"},
                COLUMN_ITEM_TAG_TAG_ID + " = ?",
                new String[]{String.valueOf(tagId)},
                null, null, null
        );

        boolean isUsed = false;
        if (cursor.moveToFirst()) {
            isUsed = cursor.getInt(0) > 0;
        }
        cursor.close();
        db.close();
        return isUsed;
    }

    // Delete a tag (only if not in use)
    public boolean deleteTag(long tagId) {
        if (isTagUsed(tagId)) {
            return false; // Cannot delete tag in use
        }

        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(
                TABLE_TAGS,
                COLUMN_TAG_ID + " = ?",
                new String[]{String.valueOf(tagId)}
        );
        db.close();
        return rowsDeleted > 0;
    }

    // Create a new tag
    public long createTag(String tagName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_NAME, tagName.trim());
        long tagId = db.insert(TABLE_TAGS, null, values);
        db.close();
        return tagId;
    }

    // Get or create tag
    public long getOrCreateTag(String tagName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TAGS,
                new String[]{COLUMN_TAG_ID},
                COLUMN_TAG_NAME + " = ?",
                new String[]{tagName.trim()},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            cursor.close();
            db.close();
            return tagId;
        }
        cursor.close();
        db.close();

        return createTag(tagName);
    }

    // Get all unique tag names for autocomplete
    public List<String> getAllTagNames() {
        List<String> tagNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TAGS,
                new String[]{COLUMN_TAG_NAME},
                null, null, null, null,
                COLUMN_TAG_NAME + " ASC"
        );

        if (cursor.moveToFirst()) {
            do {
                tagNames.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tagNames;
    }

    // ========== Image Storage Methods ==========

    private File getImageDir(Context context) {
        File imageDir = new File(context.getFilesDir(), "wardrobe_images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        return imageDir;
    }

    private String saveImageToInternalStorage(Context context, byte[] imageData, long itemId) {
        try {
            File imageDir = getImageDir(context);
            File imageFile = new File(imageDir, "item_" + itemId + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(imageData);
            fos.close();
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("SQLiteHelper", "Error saving image to internal storage", e);
            return null;
        }
    }

    public byte[] loadImageFromPath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        try {
            File imageFile = new File(imagePath);
            FileInputStream fis = new FileInputStream(imageFile);
            byte[] imageData = new byte[(int) imageFile.length()];
            fis.read(imageData);
            fis.close();
            return imageData;
        } catch (IOException e) {
            Log.e("SQLiteHelper", "Error loading image from internal storage", e);
            return null;
        }
    }

    private boolean deleteImageFromInternalStorage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return true;
        }
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            return imageFile.delete();
        }
        return true;
    }

    // ========== Item CRUD Operations ==========

    // Add item with tags and image
    public long addItem(Context context, String name, String description, long boughtTime, long lastWearTime, byte[] image, List<String> tags) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues itemValues = new ContentValues();
            itemValues.put(COLUMN_ITEM_NAME, name);
            itemValues.put(COLUMN_ITEM_DESCRIPTION, description);
            itemValues.put(COLUMN_ITEM_BOUGHT_TIME, boughtTime);
            itemValues.put(COLUMN_ITEM_LAST_WEAR_TIME, lastWearTime);

            long itemId = db.insert(TABLE_ITEMS, null, itemValues);

            String imagePath = null;
            if (image != null && image.length > 0) {
                imagePath = saveImageToInternalStorage(context, image, itemId);
                ContentValues imageUpdateValues = new ContentValues();
                imageUpdateValues.put(COLUMN_ITEM_IMAGE, imagePath);
                db.update(TABLE_ITEMS, imageUpdateValues, COLUMN_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)});
            }

            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        long tagId = getOrCreateTagInternal(db, tag);
                        linkItemWithTag(db, itemId, tagId);
                    }
                }
            }

            db.setTransactionSuccessful();
            return itemId;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Get all items with tags
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_ITEMS,
                null, null, null, null, null,
                COLUMN_ITEM_BOUGHT_TIME + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Item item = cursorToItem(cursor);
                item.setTags(getTagsForItem(db, item.getId()));
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return items;
    }

    // Get item by ID
    public Item getItemById(long itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ITEMS,
                null,
                COLUMN_ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)},
                null, null, null
        );

        Item item = null;
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor);
            item.setTags(getTagsForItem(db, item.getId()));
        }
        cursor.close();
        db.close();
        return item;
    }

    // Search items by name, description, or tags
    public List<Item> searchItems(String query) {
        List<Item> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String searchPattern = "%" + query + "%";

        // Search by name or description
        Cursor cursor = db.query(TABLE_ITEMS,
                null,
                COLUMN_ITEM_NAME + " LIKE ? OR " + COLUMN_ITEM_DESCRIPTION + " LIKE ?",
                new String[]{searchPattern, searchPattern},
                null, null, COLUMN_ITEM_BOUGHT_TIME + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Item item = cursorToItem(cursor);
                item.setTags(getTagsForItem(db, item.getId()));
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Also search by tags
        Cursor tagCursor = db.rawQuery(
                "SELECT DISTINCT i.* FROM " + TABLE_ITEMS + " i " +
                        "INNER JOIN " + TABLE_ITEM_TAGS + " it ON i." + COLUMN_ITEM_ID + " = it." + COLUMN_ITEM_TAG_ITEM_ID + " " +
                        "INNER JOIN " + TABLE_TAGS + " t ON it." + COLUMN_ITEM_TAG_TAG_ID + " = t." + COLUMN_TAG_ID + " " +
                        "WHERE t." + COLUMN_TAG_NAME + " LIKE ?",
                new String[]{searchPattern});

        if (tagCursor.moveToFirst()) {
            do {
                Item item = cursorToItem(tagCursor);
                item.setTags(getTagsForItem(db, item.getId()));
                if (!containsItem(items, item.getId())) {
                    items.add(item);
                }
            } while (tagCursor.moveToNext());
        }
        tagCursor.close();

        db.close();
        return items;
    }

    // Update item
    public boolean updateItem(Context context, long itemId, String name, String description, long boughtTime, long lastWearTime, byte[] image, List<String> tags) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            // Get old image path for cleanup
            Cursor cursor = db.query(TABLE_ITEMS, new String[]{COLUMN_ITEM_IMAGE},
                    COLUMN_ITEM_ID + " = ?", new String[]{String.valueOf(itemId)}, null, null, null);
            String oldImagePath = null;
            if (cursor.moveToFirst()) {
                oldImagePath = cursor.getString(0);
            }
            cursor.close();

            ContentValues itemValues = new ContentValues();
            itemValues.put(COLUMN_ITEM_NAME, name);
            itemValues.put(COLUMN_ITEM_DESCRIPTION, description);
            itemValues.put(COLUMN_ITEM_BOUGHT_TIME, boughtTime);
            itemValues.put(COLUMN_ITEM_LAST_WEAR_TIME, lastWearTime);

            // Handle image update
            if (image != null && image.length > 0) {
                // Delete old image if exists
                if (oldImagePath != null) {
                    deleteImageFromInternalStorage(oldImagePath);
                }
                // Save new image
                String imagePath = saveImageToInternalStorage(context, image, itemId);
                itemValues.put(COLUMN_ITEM_IMAGE, imagePath);
            } else {
                // No new image provided, keep existing image path
                itemValues.put(COLUMN_ITEM_IMAGE, oldImagePath);
            }

            int rowsAffected = db.update(TABLE_ITEMS, itemValues,
                    COLUMN_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)});

            // Delete existing tag associations
            db.delete(TABLE_ITEM_TAGS, COLUMN_ITEM_TAG_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)});

            // Add new tag associations
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        long tagId = getOrCreateTagInternal(db, tag);
                        linkItemWithTag(db, itemId, tagId);
                    }
                }
            }

            db.setTransactionSuccessful();
            return rowsAffected > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Delete item
    public boolean deleteItem(long itemId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Get image path for cleanup before deletion
        Cursor cursor = db.query(TABLE_ITEMS, new String[]{COLUMN_ITEM_IMAGE},
                COLUMN_ITEM_ID + " = ?", new String[]{String.valueOf(itemId)}, null, null, null);
        String imagePath = null;
        if (cursor.moveToFirst()) {
            imagePath = cursor.getString(0);
        }
        cursor.close();

        int rowsDeleted = db.delete(TABLE_ITEMS,
                COLUMN_ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)});

        // Delete image file if item was deleted and had an image
        if (rowsDeleted > 0 && imagePath != null) {
            deleteImageFromInternalStorage(imagePath);
        }

        db.close();
        return rowsDeleted > 0;
    }

    // ========== Helper Methods ==========

    private long getOrCreateTagInternal(SQLiteDatabase db, String tagName) {
        Cursor cursor = db.query(TABLE_TAGS,
                new String[]{COLUMN_TAG_ID},
                COLUMN_TAG_NAME + " = ?",
                new String[]{tagName.trim()},
                null, null, null);

        if (cursor.moveToFirst()) {
            long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            cursor.close();
            return tagId;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_NAME, tagName.trim());
        return db.insert(TABLE_TAGS, null, values);
    }

    private void linkItemWithTag(SQLiteDatabase db, long itemId, long tagId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_TAG_ITEM_ID, itemId);
        values.put(COLUMN_ITEM_TAG_TAG_ID, tagId);
        db.insert(TABLE_ITEM_TAGS, null, values);
    }

    private List<String> getTagsForItem(SQLiteDatabase db, long itemId) {
        List<String> tags = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT t." + COLUMN_TAG_NAME + " FROM " + TABLE_TAGS + " t " +
                        "INNER JOIN " + TABLE_ITEM_TAGS + " it ON t." + COLUMN_TAG_ID + " = it." + COLUMN_ITEM_TAG_TAG_ID + " " +
                        "WHERE it." + COLUMN_ITEM_TAG_ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)});

        if (cursor.moveToFirst()) {
            do {
                tags.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tags;
    }

    private Item cursorToItem(Cursor cursor) {
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_IMAGE));
        Item item = new Item(
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_DESCRIPTION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_BOUGHT_TIME)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LAST_WEAR_TIME)),
                imagePath
        );

        if (imagePath != null) {
            byte[] imageData = loadImageFromPath(imagePath);
            item.setImageData(imageData);
        }

        return item;
    }

    private boolean containsItem(List<Item> items, long itemId) {
        for (Item item : items) {
            if (item.getId() == itemId) {
                return true;
            }
        }
        return false;
    }}
