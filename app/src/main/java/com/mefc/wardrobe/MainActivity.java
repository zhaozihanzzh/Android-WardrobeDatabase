package com.mefc.wardrobe;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_ITEM = 100;
    private static final int REQUEST_EDIT_ITEM = 101;
    private static final int REQUEST_PICK_IMAGE = 102;

    private SQLiteHelper dbHelper;
    private ItemAdapter itemAdapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new SQLiteHelper(this);

        setupRecyclerView();
        loadItems();

        findViewById(R.id.fabAddItem).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditItemActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ITEM);
        });
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewItems);
        itemAdapter = new ItemAdapter(this, item -> {
            // Handle item click - open for editing
            Intent intent = new Intent(this, AddEditItemActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivityForResult(intent, REQUEST_EDIT_ITEM);
        });
        recyclerView.setAdapter(itemAdapter);
    }

    private void loadItems() {
        List<Item> items = dbHelper.getAllItems();
        itemAdapter.setItems(items);
    }

    private void searchItems(String query) {
        if (query.isEmpty()) {
            loadItems();
        } else {
            List<Item> items = dbHelper.searchItems(query);
            itemAdapter.setItems(items);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchItems(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchItems(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_ADD_ITEM || requestCode == REQUEST_EDIT_ITEM) {
                loadItems();
            }
        }
    }

    // Helper method to convert Bitmap to byte array
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
