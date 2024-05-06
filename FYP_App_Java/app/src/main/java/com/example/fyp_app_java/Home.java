package com.example.fyp_app_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.collection.BuildConfig;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Home extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private String currentPhotoPath;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {

            getSupportActionBar().hide();

        }

        //***************Get User Name*********************
        db = FirebaseFirestore.getInstance();

        String username = getIntent().getStringExtra("USERNAME_EXTRA");

        TextView usernameViewText = findViewById(R.id.user);
        usernameViewText.setText(username);

        //****************Edit Profile Button**********************
        Button editProfileBtn = findViewById(R.id.edit_profile_btn);
        editProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Pass Username to edit profile page
                Intent intent = new Intent(Home.this, Edit_profile_redo.class);
                intent.putExtra("USERNAME_EXTRA", username);
                startActivity(intent);
            }
        });

        //*********Photo_Gallery Button*************
        Button viewPhotosBtn = findViewById(R.id.view_photos);
        viewPhotosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Home.this, Photo_Gallary.class));
            }
        });


        //***************Sign Out*******************
        Button signOutButton = findViewById(R.id.sign_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();

                startActivity(new Intent(Home.this, MainActivity.class));
                finish();
            }
        });

        //*********Stories Button*************
        Button btn_stories = findViewById(R.id.stories_btn);
        btn_stories.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Home.this, Stories.class));
            }
        });

        //*********RUN AR*************
        Button btn_view_3D = findViewById(R.id.view_3d);
        btn_view_3D.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClassName("com.DefaultCompany.FYP_AR_2", "com.unity3d.player.UnityPlayerActivity");
                startActivity(intent);
            }
        });

        //*********TAKE PHOTO*************
        storage = FirebaseStorage.getInstance();

        Button btn_Take_Photo = findViewById(R.id.take_photo);
        btn_Take_Photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check camera permission
                if (checkCameraPermission()) {
                    openCamera();
                } else {
                    // Request camera permission
                    requestCameraPermission();
                }

            }
        });
    }

    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        } else {

            return true;
        }
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(Home.this, "Unable to open camera due to permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }
        //@SuppressLint("QueryPermissionsNeeded")
        private void openCamera() {
            //System.out.println("Camera Open");
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    Uri photoUri = FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".provider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }

        private File createImageFile() {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            try {
                File imageFile = File.createTempFile(
                        imageFileName,
                        ".jpg",
                        storageDir
                );
                currentPhotoPath = imageFile.getAbsolutePath();
                return imageFile;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                // The image was captured successfully
                //upload the image to Firebase Cloud Storage
                uploadImageToFirebase();
            }
        }

        private void uploadImageToFirebase() {
            if (currentPhotoPath != null) {
                Uri fileUri = Uri.fromFile(new File(currentPhotoPath));
                StorageReference storageRef = storage.getReference().child("images/" + fileUri.getLastPathSegment());

                storageRef.putFile(fileUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // Image uploaded successfully
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                // save it to a database
                                saveImageUrlToFirestore(downloadUrl);
                                Toast.makeText(Home.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Handle the failure
                            e.printStackTrace();
                            Toast.makeText(Home.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        });
            }
        }

    private void saveImageUrlToFirestore(String imageUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Construct the document reference for the current user's "images" collection
            DocumentReference imagesRef = db.collection("users").document(currentUser.getUid()).collection("images").document();

            // Create a new document in the "images" collection
            imagesRef.set(new HashMap<String, Object>() {{
                        put("imageUrl", imageUrl);
                    }})
                    .addOnSuccessListener(aVoid -> {
                        // Image URL saved successfully in Firestore
                        Toast.makeText(Home.this, "Profile image uploaded and saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Failed to save image URL in Firestore
                        Toast.makeText(Home.this, "Failed to save profile image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    }