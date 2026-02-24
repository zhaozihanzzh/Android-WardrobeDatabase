package com.mefc.wardrobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> items;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView;
        private TextView boughtTimeTextView;
        private TextView lastWearTimeTextView;
        private TextView tagsTextView;
        private ImageView imageView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewName);
            descriptionTextView = itemView.findViewById(R.id.textViewDescription);
            boughtTimeTextView = itemView.findViewById(R.id.textViewBoughtTime);
            lastWearTimeTextView = itemView.findViewById(R.id.textViewLastWearTime);
            tagsTextView = itemView.findViewById(R.id.textViewTags);
            imageView = itemView.findViewById(R.id.imageViewItem);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(items.get(position));
                }
            });
        }

        public void bind(Item item) {
            nameTextView.setText(item.getName());
            descriptionTextView.setText(item.getDescription());
            boughtTimeTextView.setText(context.getString(R.string.bought) + item.getFormattedBoughtTime());
            lastWearTimeTextView.setText(context.getString(R.string.last_wear) + item.getFormattedLastWearTime());

            // Display tags
            if (item.getTags() != null && !item.getTags().isEmpty()) {
            } else {
                tagsTextView.setText(context.getString(R.string.tags) + ": " + String.join(", ", item.getTags()));
                tagsTextView.setText(context.getString(R.string.tags) + ": None");
            }

            // Display image
            if (item.getImageData() != null && item.getImageData().length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(item.getImageData(), 0, item.getImageData().length);
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }
    }
}
