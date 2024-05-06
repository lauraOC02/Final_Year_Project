package com.example.fyp_app_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Photo_Gallary extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<String> imageUrls;
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallary);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            // Hide the title
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#A7CA95")));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Adjust span count as needed
        imageUrls = new ArrayList<>();
        adapter = new GalleryAdapter(imageUrls);
        recyclerView.setAdapter(adapter);

        // Fetch images from Firestore
        fetchImages();
    }

    private void fetchImages() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Get reference to the "images" collection for the current user
            CollectionReference imagesRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .collection("images");

            // Query all documents in the "images" collection
            imagesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    // Get image URL from each document and add it to the list
                    String imageUrl = documentSnapshot.getString("imageUrl");
                    if (imageUrl != null) {
                        imageUrls.add(imageUrl);
                    }
                }
                // Notify adapter of data change
                adapter.notifyDataSetChanged();
            }).addOnFailureListener(e -> {
                // Handle failure to fetch images
                Toast.makeText(Photo_Gallary.this, "Failed to fetch images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }


    private static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ImageViewHolder> {
        private List<String> imageUrls;

        public GalleryAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, @SuppressLint("RecyclerView") int position) {
            // Load image into ImageView using Picasso or Glide
            String imageUrl = imageUrls.get(position);
            Picasso.get().load(imageUrl)
                    .resize(400,400)
                    .centerCrop()
                    .rotate(getRotationDegrees(imageUrl))
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Pass imageUrls and position to FullscreenGalleryActivity
                    Intent intent = new Intent(v.getContext(), Fullscreen_Gallery.class);
                    intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
                    intent.putExtra("position", position);
                    v.getContext().startActivity(intent);
                }
            });
        }

        // Method to get the rotation degrees based on Exif orientation
        private int getRotationDegrees(String imageUrl) {
            try {
                ExifInterface exifInterface = new ExifInterface(imageUrl);
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return 90;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return 180;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return 270;
                    default:
                        return 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        public static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}