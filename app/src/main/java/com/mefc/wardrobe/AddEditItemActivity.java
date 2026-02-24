package com.mefc.wardrobe;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditItemActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 102;

    private SQLiteHelper dbHelper;
    private long itemId = -1;
    private byte[] imageData = null;
    private List<String> selectedTags = new ArrayList<>();
    private long boughtTime = System.currentTimeMillis();
    private long lastWearTime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        dbHelper = new SQLiteHelper(this);

        loadExistingItem();
        setupToolbar();
        initializeViews();
    }

    private void setupToolbar() {
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(itemId == -1 ? R.string.add_item : R.string.edit_item);
    }

    private void initializeViews() {
        Button selectImageButton = findViewById(R.id.buttonSelectImage);
        Button removeImageButton = findViewById(R.id.buttonRemoveImage);
        Button saveButton = findViewById(R.id.buttonSave);
        Button deleteButton = findViewById(R.id.buttonDelete);
        Button manageTagsButton = findViewById(R.id.buttonManageTags);
        Button boughtTimeButton = findViewById(R.id.buttonSelectBoughtTime);
        Button lastWearTimeButton = findViewById(R.id.buttonSelectLastWearTime);
        ImageView imageView = findViewById(R.id.imageViewItem);

        // Show delete button only when editing
        if (itemId != -1) {
            deleteButton.setVisibility(View.VISIBLE);
        }

        // Select image
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        });

        // Remove image
        removeImageButton.setOnClickListener(v -> {
            imageData = null;
            imageView.setImageBitmap(null);
            imageView.setBackgroundResource(android.R.drawable.ic_menu_gallery);
            removeImageButton.setVisibility(View.GONE);
        });

        // Select bought time
        boughtTimeButton.setOnClickListener(v -> showDateTimePicker(true));

        // Select last wear time
        lastWearTimeButton.setOnClickListener(v -> showDateTimePicker(false));

        // Manage tags
        manageTagsButton.setOnClickListener(v -> showTagSelectionDialog());

        // Delete item
        deleteButton.setOnClickListener(v -> showDeleteConfirmation());

        // Save
        saveButton.setOnClickListener(v -> saveItem());
    }

    private void showTagSelectionDialog() {
        TagSelectionDialog dialog = new TagSelectionDialog(
                this,
                dbHelper,
                selectedTags,
                this::handleTagsSelected
        );
        dialog.show();
    }

    private void handleTagsSelected(List<String> tags) {
        selectedTags.clear();
        selectedTags.addAll(tags);
        updateSelectedTagsDisplay();
    }

    private void updateSelectedTagsDisplay() {
        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chipGroupSelectedTags);
        TextView noTagsTextView = findViewById(R.id.textViewNoTags);

        chipGroup.removeAllViews();

        if (selectedTags.isEmpty()) {
            noTagsTextView.setVisibility(View.VISIBLE);
        } else {
            noTagsTextView.setVisibility(View.GONE);

            for (String tagName : selectedTags) {
                Chip chip = new Chip(this);
                chip.setText(tagName);
                chip.setCloseIconVisible(true);
                chip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                chip.setTextColor(getColor(android.R.color.white));

                chip.setOnCloseIconClickListener(v -> {
                    selectedTags.remove(tagName);
                    updateSelectedTagsDisplay();
                });

                chipGroup.addView(chip);
            }
        }
    }

    private void loadExistingItem() {
        itemId = getIntent().getLongExtra("item_id", -1);
        if (itemId != -1) {
            Item item = dbHelper.getItemById(itemId);
            if (item != null) {
                ((EditText) findViewById(R.id.editTextName)).setText(item.getName());
                ((EditText) findViewById(R.id.editTextDescription)).setText(item.getDescription());

                boughtTime = item.getBoughtTime();
                lastWearTime = item.getLastWearTime();
                updateTimeDisplay(true);
                updateTimeDisplay(false);

                if (item.getImagePath() != null) {
                    byte[] imageDataFromPath = dbHelper.loadImageFromPath(item.getImagePath());
                    if (imageDataFromPath != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageDataFromPath, 0, imageDataFromPath.length);
                        ImageView imageView = findViewById(R.id.imageViewItem);
                        imageView.setImageBitmap(bitmap);
                        imageView.setBackground(null);
                        findViewById(R.id.buttonRemoveImage).setVisibility(View.VISIBLE);
                        imageData = imageDataFromPath;
                        item.setImageData(imageDataFromPath);
                    }
                }

                if (item.getTags() != null && !item.getTags().isEmpty()) {
                    selectedTags.clear();
                    selectedTags.addAll(item.getTags());
                    updateSelectedTagsDisplay();
                }
            }
        } else {
            // New item: initialize with current time
            updateTimeDisplay(true);
            updateTimeDisplay(false);
        }
    }

    private void saveItem() {
        EditText nameEditText = findViewById(R.id.editTextName);
        EditText descriptionEditText = findViewById(R.id.editTextDescription);

        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError(getString(R.string.name_required));
            return;
        }

        if (itemId == -1) {
            dbHelper.addItem(getApplicationContext(), name, description, boughtTime, lastWearTime, imageData, selectedTags);
        } else {
            dbHelper.updateItem(getApplicationContext(), itemId, name, description, boughtTime, lastWearTime, imageData, selectedTags);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ImageView imageView = findViewById(R.id.imageViewItem);
                imageView.setImageBitmap(bitmap);
                imageView.setBackground(null);
                findViewById(R.id.buttonRemoveImage).setVisibility(View.VISIBLE);
                imageData = bitmapToByteArray(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        android.graphics.Bitmap.CompressFormat format = bitmap.hasAlpha() ?
                android.graphics.Bitmap.CompressFormat.PNG : android.graphics.Bitmap.CompressFormat.JPEG;
        int quality = bitmap.hasAlpha() ? 100 : 85;

        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(format, quality, stream);
        return stream.toByteArray();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showDateTimePicker(final boolean isBoughtTime) {
        Calendar calendar = Calendar.getInstance();
        long currentTime = isBoughtTime ? boughtTime : lastWearTime;
        calendar.setTimeInMillis(currentTime);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            AddEditItemActivity.this,
                            (timeView, selectedHour, selectedMinute) -> {
                                selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour);
                                selectedDate.set(Calendar.MINUTE, selectedMinute);
                                selectedDate.set(Calendar.SECOND, 0);
                                selectedDate.set(Calendar.MILLISECOND, 0);

                                if (isBoughtTime) {
                                    boughtTime = selectedDate.getTimeInMillis();
                                } else {
                                    lastWearTime = selectedDate.getTimeInMillis();
                                }
                                updateTimeDisplay(isBoughtTime);
                            },
                            hour,
                            minute,
                            false // 24-hour format
                    );
                    timePickerDialog.show();
                },
                year,
                month,
                day
        );
        datePickerDialog.show();
    }

    private void updateTimeDisplay(boolean isBoughtTime) {
        long time = isBoughtTime ? boughtTime : lastWearTime;
        int textViewId = isBoughtTime ? R.id.textViewBoughtTime : R.id.textViewLastWearTime;
        TextView textView = findViewById(textViewId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textView.setText(sdf.format(new java.util.Date(time)));
        textView.setTextColor(getColor(android.R.color.black));
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(R.string.confirm_delete_item)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    boolean deleted = dbHelper.deleteItem(itemId);
                    if (deleted) {
                        Toast.makeText(this, R.string.item_deleted, Toast.LENGTH_SHORT).show();
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
