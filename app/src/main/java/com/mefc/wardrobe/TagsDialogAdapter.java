package com.mefc.wardrobe;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TagsDialogAdapter extends RecyclerView.Adapter<TagsDialogAdapter.TagViewHolder> {

    private List<TagInfo> tags;
    private OnTagInteractionListener listener;

    public interface OnTagInteractionListener {
        void onTagSelectionChanged(TagInfo tag, boolean selected);
        void onDeleteTag(TagInfo tag);
    }

    public TagsDialogAdapter(OnTagInteractionListener listener) {
        this.tags = new ArrayList<>();
        this.listener = listener;
    }

    public void setTags(List<TagInfo> tags) {
        this.tags = tags;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_dialog, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        TagInfo tag = tags.get(position);
        holder.bind(tag);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView nameTextView;
        private TextView usageTextView;
        private ImageButton deleteButton;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBoxTag);
            nameTextView = itemView.findViewById(R.id.textViewTagName);
            usageTextView = itemView.findViewById(R.id.textViewTagUsage);
            deleteButton = itemView.findViewById(R.id.buttonDeleteTag);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTagSelectionChanged(tags.get(position), isChecked);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteTag(tags.get(position));
                }
            });
        }

        public void bind(TagInfo tag) {
            checkBox.setChecked(tag.isSelected());
            nameTextView.setText(tag.getName());
            usageTextView.setText(
                    itemView.getContext().getString(R.string.tag_usage_format, tag.getUsageCount()));

            updateDeleteButtonState(tag.getUsageCount() == 0);
        }

        public void updateDeleteButtonState(boolean enabled) {
            deleteButton.setEnabled(enabled);
            if (enabled) {
                deleteButton.setColorFilter(Color.parseColor("#D32F2F"), PorterDuff.Mode.SRC_IN);
                deleteButton.setAlpha(1.0f);
            } else {
                deleteButton.setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.SRC_IN);
                deleteButton.setAlpha(0.5f);
            }
        }
    }
}
