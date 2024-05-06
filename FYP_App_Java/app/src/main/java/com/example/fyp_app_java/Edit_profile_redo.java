package com.example.fyp_app_java;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Edit_profile_redo extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText usernameEditText, emailEditText, passwordEditText;

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

        fetchUserData();


        // Save Changes button click listener
        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newUsername = usernameEditText.getText().toString().trim();
                final String newEmail = emailEditText.getText().toString().trim();
                final String newPassword = passwordEditText.getText().toString().trim();

                fetchCurrentUsername(newUsername, new Callback<String>() {
                    public void onSuccess(String currentUsername) {
                        // Compare newUsername with currentUsername fetched from Firestore
                        if (!newUsername.equals(currentUsername)) {
                            Log.d(TAG, "Username Changed from original: " + currentUsername);
                            // Prompt user to re-authenticate before updating username
                            getCurrentUserPassword(new Callback<String>() {
                                @Override
                                public void onSuccess(String currentPassword) {
                                    AuthCredential credential = EmailAuthProvider.getCredential(auth.getCurrentUser().getEmail(), currentPassword);

                                    auth.getCurrentUser().reauthenticate(credential)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        // Re-authentication successful, proceed with username update
                                                        updateUserUsernameInFirestore(newUsername);
                                                    } else {
                                                        // Failed to re-authenticate user
                                                        Toast.makeText(Edit_profile_redo.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                                                        Log.d(TAG, "Failed to re-authenticate user: " + task.getException().getMessage());
                                                    }
                                                }
                                            });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // Handle failure to get current password
                                    Log.d(TAG, "Failed to get current password: " + e.getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle failure to fetch current username
                        Log.d(TAG, "Failed to fetch current username: " + e.getMessage());
                    }
                });


                // Prompt user to re-authenticate before updating email/password
                getCurrentUserPassword(new Callback<String>() {
                    @Override
                    public void onSuccess(String currentPassword) {
                        AuthCredential credential = EmailAuthProvider.getCredential(auth.getCurrentUser().getEmail(), currentPassword);

                        auth.getCurrentUser().reauthenticate(credential)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            // Re-authentication successful, proceed with updates
                                            // Update email if it has changed
                                            if (!newEmail.equals(auth.getCurrentUser().getEmail())) {
                                                auth.getCurrentUser().updateEmail(newEmail)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Toast.makeText(Edit_profile_redo.this, "Email Successfully Changed", Toast.LENGTH_SHORT).show();
                                                                    Log.d(TAG, "User email address updated.");
                                                                    // Update email in Firestore
                                                                    updateUserEmailInFirestore(newEmail);
                                                                    // Update EditText field with new email
                                                                    emailEditText.setText(newEmail);
                                                                } else {
                                                                    Toast.makeText(Edit_profile_redo.this, "Failed to Change Email", Toast.LENGTH_SHORT).show();
                                                                    // Failed to update email
                                                                    Log.d(TAG, "Failed to update email: " + task.getException().getMessage());
                                                                }
                                                            }
                                                        });
                                            }

                                            // Update password if it's not empty
                                            if (!newPassword.isEmpty()) {
                                                auth.getCurrentUser().updatePassword(newPassword)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Toast.makeText(Edit_profile_redo.this, "Password Successfully Changed", Toast.LENGTH_SHORT).show();
                                                                    Log.d(TAG, "User password updated.");
                                                                } else {
                                                                    // Failed to update password
                                                                    Toast.makeText(Edit_profile_redo.this, "Password Failed to Change", Toast.LENGTH_SHORT).show();
                                                                    Log.d(TAG, "Failed to update password: " + task.getException().getMessage());
                                                                }
                                                            }
                                                        });
                                            }

//                                            // Update username in Firestore only if it has changed
//                                            if (!newUsername.equals(usernameEditText.getText().toString().trim())) {
//                                                Log.d(TAG, "Username Changed from original " + task.getException().getMessage());
//                                                updateUserUsernameInFirestore(newUsername);
//                                            }
                                        } else {
                                            // Failed to re-authenticate user
                                            Toast.makeText(Edit_profile_redo.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Failed to re-authenticate user: " + task.getException().getMessage());
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle failure to get current password
                        Log.d(TAG, "Failed to get current password: " + e.getMessage());
                    }
                });
            }
        });
    }

    // Helper method to prompt user for their current password
    private void getCurrentUserPassword(final Callback<String> callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Current Password");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                callback.onSuccess(password);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                callback.onFailure(new Exception("User canceled password entry"));
            }
        });

        builder.show();
    }

    // Helper method to update user email in Firestore
    private void updateUserEmailInFirestore(String newEmail) {
        db.collection("users").document(auth.getCurrentUser().getUid())
                .update("email", newEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Email updated in Firestore.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure to update email in Firestore
                        Log.d(TAG, "Failed to update email in Firestore: " + e.getMessage());
                    }
                });
    }

    // Helper method to update user username in Firestore
    private void updateUserUsernameInFirestore(String newUsername) {
        final FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && newUsername != null && !newUsername.isEmpty()) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.update("username", newUsername)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Update EditText field with new username
                            usernameEditText.setText(newUsername);
                            Toast.makeText(Edit_profile_redo.this, "Username Successfully Changed", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Username updated in Firestore.");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle failure to update username in Firestore
                            Toast.makeText(Edit_profile_redo.this, "Username Failed to Change", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Failed to update username in Firestore: " + e.getMessage());
                        }
                    });
        } else {
            // Handle null or empty newUsername
            Toast.makeText(Edit_profile_redo.this, "Invalid Username", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Invalid Username: " + newUsername);
        }
    }

    // Helper method to fetch user data from Firestore
    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Fetch the latest user data from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
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
                            Toast.makeText(Edit_profile_redo.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void fetchCurrentUsername(final String newUsername, final Callback<String> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Fetch current username from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                String currentUsername = documentSnapshot.getString("username");
                                callback.onSuccess(currentUsername);
                            } else {
                                callback.onFailure(new Exception("Cancelled"));

                            }
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

    // Define a simple callback interface
    interface Callback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
