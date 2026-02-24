package com.mefc.wardrobe;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagSelectionDialog extends Dialog {

    public interface OnTagsSelectedListener {
        void onTagsSelected(List<String> selectedTags);
    }

    private SQLiteHelper dbHelper;
    private OnTagsSelectedListener listener;
    private List<TagInfo> allTags;
    private Set<String> initiallySelectedTags;
    private TagsDialogAdapter tagsAdapter;

    private RecyclerView recyclerViewTags;
    private EditText editTextNewTag;
    private Button buttonCreateTag;
    private LinearLayout layoutEmptyTags;
    private TextView textViewSelectedCount;
    private Button buttonCancel;
    private Button buttonConfirm;

    public TagSelectionDialog(@NonNull Context context,
                              SQLiteHelper dbHelper,
                              List<String> initiallySelectedTags,
                              OnTagsSelectedListener listener) {
        super(context);
        this.dbHelper = dbHelper;
        this.initiallySelectedTags = new HashSet<>(initiallySelectedTags);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tag_selection, null);
        setContentView(view);

        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadTags();
        updateEmptyState();
        updateSelectedCount();
    }

    private void initializeViews(View view) {
        recyclerViewTags = view.findViewById(R.id.recyclerViewTags);
        editTextNewTag = view.findViewById(R.id.editTextNewTag);
        buttonCreateTag = view.findViewById(R.id.buttonCreateTag);
        layoutEmptyTags = view.findViewById(R.id.layoutEmptyTags);
        textViewSelectedCount = view.findViewById(R.id.textViewSelectedCount);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);

        // Set max height for RecyclerView
//        recyclerViewTags.setMaxHeight(300);

    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerViewTags.setLayoutManager(layoutManager);

        tagsAdapter = new TagsDialogAdapter(new TagsDialogAdapter.OnTagInteractionListener() {
            @Override
            public void onTagSelectionChanged(TagInfo tag, boolean selected) {
                tag.setSelected(selected);
                updateSelectedCount();
            }

            @Override
            public void onDeleteTag(TagInfo tag) {
                confirmDeleteTag(tag);
            }
        });
        recyclerViewTags.setAdapter(tagsAdapter);
    }

    private void setupListeners() {
        // Create new tag
        editTextNewTag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                buttonCreateTag.setEnabled(!s.toString().trim().isEmpty());
            }
        });

        editTextNewTag.setOnEditorActionListener((v, actionId, event) -> {
            createNewTag();
            return true;
        });

        buttonCreateTag.setOnClickListener(v -> createNewTag());

        // Cancel button
        buttonCancel.setOnClickListener(v -> dismiss());

        // Confirm button
        buttonConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void loadTags() {
        allTags = dbHelper.getAllTagsWithUsage();

        // Set initial selection state
        for (TagInfo tagInfo : allTags) {
            tagInfo.setSelected(initiallySelectedTags.contains(tagInfo.getName()));
        }

        tagsAdapter.setTags(allTags);
    }

    private void createNewTag() {
        String newTagName = editTextNewTag.getText().toString().trim();

        if (newTagName.isEmpty()) {
            editTextNewTag.setError(getContext().getString(R.string.tag_name_required));
            return;
        }

        // Check if tag already exists
        boolean tagExists = false;
        for (TagInfo tagInfo : allTags) {
            if (tagInfo.getName().equalsIgnoreCase(newTagName)) {
                tagExists = true;
                // Select the existing tag
                tagInfo.setSelected(true);
                break;
            }
        }

        if (!tagExists) {
            // Create new tag
            long tagId = dbHelper.createTag(newTagName);

            // Add to list and select it
            TagInfo newTag = new TagInfo(tagId, newTagName, 0);
            newTag.setSelected(true);
            allTags.add(newTag);
        }

        editTextNewTag.setText("");
        tagsAdapter.setTags(allTags);
        updateEmptyState();
        updateSelectedCount();

        // Scroll to bottom to show new tag
        recyclerViewTags.smoothScrollToPosition(allTags.size() - 1);

        Toast.makeText(getContext(), R.string.tag_created, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteTag(TagInfo tag) {
        if (tag.getUsageCount() > 0) {
            Toast.makeText(getContext(), R.string.cannot_delete_used_tag, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_tag)
                .setMessage(getContext().getString(R.string.confirm_delete_tag, tag.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (dbHelper.deleteTag(tag.getId())) {
                        allTags.remove(tag);
                        tagsAdapter.setTags(allTags);
                        updateEmptyState();
                        updateSelectedCount();
                        Toast.makeText(getContext(), R.string.tag_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateEmptyState() {
        boolean isEmpty = allTags.isEmpty();
        layoutEmptyTags.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewTags.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateSelectedCount() {
        int selectedCount = 0;
        for (TagInfo tagInfo : allTags) {
            if (tagInfo.isSelected()) {
                selectedCount++;
            }
        }

        textViewSelectedCount.setText(
                getContext().getString(R.string.tags_selected_count, selectedCount));

        // Dim delete button for used tags
        for (int i = 0; i < recyclerViewTags.getChildCount(); i++) {
            View child = recyclerViewTags.getChildAt(i);
            TagsDialogAdapter.TagViewHolder holder =
                    (TagsDialogAdapter.TagViewHolder) recyclerViewTags.getChildViewHolder(child);
            if (holder != null && i < allTags.size()) {
                TagInfo tag = allTags.get(i);
                holder.updateDeleteButtonState(tag.getUsageCount() == 0);
            }
        }
    }

    private void confirmSelection() {
        List<String> selectedTags = new ArrayList<>();
        for (TagInfo tagInfo : allTags) {
            if (tagInfo.isSelected()) {
                selectedTags.add(tagInfo.getName());
            }
        }

        if (listener != null) {
            listener.onTagsSelected(selectedTags);
        }

        dismiss();
    }
}
