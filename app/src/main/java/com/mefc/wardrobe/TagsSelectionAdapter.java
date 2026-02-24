package com.mefc.wardrobe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TagsSelectionAdapter extends RecyclerView.Adapter<TagsSelectionAdapter.TagViewHolder> {

    private List<TagInfo> tags;
    private OnTagInteractionListener listener;

    public interface OnTagInteractionListener {
        void onTagSelectionChanged(TagInfo tag, boolean selected);
        void onDeleteTag(TagInfo tag);
    }

    public TagsSelectionAdapter(OnTagInteractionListener listener) {
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
                .inflate(R.layout.item_tag_selection, parent, false);
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
        private Button deleteButton;

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
            checkBox.setTag(tag);
            checkBox.setChecked(tag.isSelected());
            nameTextView.setText(tag.getName());
            usageTextView.setText("Used by " + tag.getUsageCount() + " item(s)");

            // Disable delete button if tag is in use
            deleteButton.setEnabled(tag.getUsageCount() == 0);
            deleteButton.setAlpha(tag.getUsageCount() == 0 ? 1f : 0.5f);
        }
    }
}
