package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.regex.Pattern;

/**
 * RegisterActivity handles user registration functionality.
 * It allows users to create a new account by providing their first name, last name, email, password, and username.
 * The activity validates user input, checks for username availability, and registers the user using Firebase Authentication and Firestore.
 *
 * @author Bhuvan Veeravalli
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout layoutFirstName, layoutLastName, layoutEmail, layoutPassword, layoutUsername; // Layouts for input fields
    private TextInputEditText firstName, lastName, email, password, username; // EditTexts for user input
    private FirebaseAuth auth; // Firebase Authentication instance
    private FirebaseFirestore db; // Firestore database instance
    private TextView loginLink; // Link to navigate to the login screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        layoutFirstName = findViewById(R.id.layoutFirstName);
        layoutLastName = findViewById(R.id.layoutLastName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutUsername = findViewById(R.id.layoutUsername);

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        username = findViewById(R.id.username);
        loginLink = findViewById(R.id.loginLink);

        // Set click listener for the register button
        findViewById(R.id.registerBtn).setOnClickListener(v -> validateAndCheckUsername());

        // Set click listener for the login link
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Validates user input and checks if the username is already taken.
     * If the input is valid and the username is available, it proceeds to register the user.
     */
    private void validateAndCheckUsername() {
        String firstNameText = firstName.getText().toString().trim();
        String lastNameText = lastName.getText().toString().trim();
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String usernameText = username.getText().toString().trim();

        boolean isValid = true;

        // Validate first name
        if (firstNameText.isEmpty()) {
            layoutFirstName.setError("First name is required");
            isValid = false;
        } else {
            layoutFirstName.setError(null);
        }

        // Validate last name
        if (lastNameText.isEmpty()) {
            layoutLastName.setError("Last name is required");
            isValid = false;
        } else {
            layoutLastName.setError(null);
        }

        // Validate username
        if (!isUsernameValid(usernameText)) {
            layoutUsername.setError("Username must be 5-20 alphanumeric characters!");
            isValid = false;
        } else {
            layoutUsername.setError(null);
        }

        // Validate email
        if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            layoutEmail.setError("Please enter a valid email");
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutEmail.setEndIconDrawable(R.drawable.ic_error);
            isValid = false;
        } else {
            layoutEmail.setError(null);
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        // Validate password
        if (passwordText.isEmpty() || passwordText.length() < 6) {
            layoutPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            layoutPassword.setError(null);
        }

        // If input is invalid, stop further processing
        if (!isValid) return;

        // Check if the username is already taken
        db.collection("users").whereEqualTo("username", usernameText).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            layoutUsername.setError("Username is already taken");
                        } else {
                            // Register the user if the username is available
                            registerUser(emailText, passwordText, firstNameText, lastNameText, usernameText);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Database error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Validates the username format.
     *
     * @param username The username to validate.
     * @return True if the username is valid, false otherwise.
     */
    private boolean isUsernameValid(String username) {
        return Pattern.matches("^[a-zA-Z0-9]{5,20}$", username);
    }

    /**
     * Registers the user using Firebase Authentication and stores user data in Firestore.
     *
     * @param email     The user's email address.
     * @param password  The user's password.
     * @param firstName The user's first name.
     * @param lastName  The user's last name.
     * @param username  The user's username.
     */
    private void registerUser(String email, String password, String firstName, String lastName, String username) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();

                // Store user data in Firestore
                db.collection("users").document(userId)
                        .set(new UserModel(email, username, firstName, lastName))
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            layoutEmail.setError("Registration failed: " + e.getMessage());
                        });

            } else {
                layoutEmail.setError("Error: " + task.getException().getMessage());
            }
        });
    }

    /**
     * UserModel represents the user data stored in Firestore.
     */
    static class UserModel {
        public String email, username, firstName, lastName;

        /**
         * Constructor for UserModel.
         *
         * @param email     The user's email address.
         * @param username  The user's username.
         * @param firstName The user's first name.
         * @param lastName  The user's last name.
         */
        public UserModel(String email, String username, String firstName, String lastName) {
            this.email = email;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}