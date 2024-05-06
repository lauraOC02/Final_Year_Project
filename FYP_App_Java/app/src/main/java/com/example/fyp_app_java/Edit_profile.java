package com.example.fyp_app_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Edit_profile extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText usernameEditText, emailEditText, passwordEditText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            // Hide the title
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#A7CA95")));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameEditText = findViewById(R.id.editUsername);
        emailEditText = findViewById(R.id.editEmail);
        passwordEditText = findViewById(R.id.editPassword);
        Button saveChangesButton = findViewById(R.id.saveChanges);
        Button cancelButton = findViewById(R.id.cancelBtn);

        //**************Save Changes******************
        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prompt the user to re-enter their password for re-authentication
                AlertDialog.Builder builder = new AlertDialog.Builder(Edit_profile.this);
                builder.setTitle("Re-enter your password");

                final EditText input = new EditText(Edit_profile.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String enteredPassword = input.getText().toString();
                        reAuthenticateUser(enteredPassword);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        // Cancel button click listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close the activity without saving changes
                finish();
            }
        });

        fetchUserData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch user data whenever the activity resumes
        fetchUserData();
    }

    // Get the current user
    private void fetchUserData() {
    FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
        // Fetch the latest user data from Firestore
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Update UI with the latest user data
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    // Set username and email in EditText fields
                    usernameEditText.setText(username);
                    emailEditText.setText(email);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle failure to fetch user data
                Toast.makeText(Edit_profile.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

    // Method to update user information
    private void updateUserInformation() {
        final String newUsername = usernameEditText.getText().toString().trim();
        final String newEmail = emailEditText.getText().toString().trim();
        final String newPassword = passwordEditText.getText().toString().trim();

        // Get the current user
        final FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), newPassword);

            // Re-authenticate the user
            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
                            userRef.update("username", newUsername)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Username updated successfully in Firestore
                                            Toast.makeText(Edit_profile.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Failed to update username in Firestore
                                            Toast.makeText(Edit_profile.this, "Failed to update username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            // Update email if changed
                            if (!newEmail.equals(currentUser.getEmail())) {
                                currentUser.updateEmail(newEmail)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Email updated successfully
                                                Toast.makeText(Edit_profile.this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Email update failed
                                                Toast.makeText(Edit_profile.this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }

                            // Update password if changed
                            if (!newPassword.isEmpty()) {
                                currentUser.updatePassword(newPassword)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Password updated successfully
                                                Toast.makeText(Edit_profile.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Password update failed
                                                Toast.makeText(Edit_profile.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    private void reAuthenticateUser(String enteredPassword) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), enteredPassword);

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Re-authentication successful, proceed to update user information
                            updateUserInformation();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Re-authentication failed, display an error message
                            Toast.makeText(Edit_profile.this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
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