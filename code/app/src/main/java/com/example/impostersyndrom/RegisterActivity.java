package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText email, password, username, firstName, lastName;
    private Button registerBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        username = findViewById(R.id.username);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        registerBtn = findViewById(R.id.registerBtn);

        registerBtn.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String usernameText = username.getText().toString().trim();
        String firstNameText = firstName.getText().toString().trim();
        String lastNameText = lastName.getText().toString().trim();

        // Ensure no field is empty
        if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText) || TextUtils.isEmpty(usernameText)) {
            showToast("Registration failed: All fields are required!");
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            showToast("Registration failed: Invalid email format!");
            return;
        }

        // Validate password length (Firebase requires at least 6 characters)
        if (passwordText.length() < 6) {
            showToast("Registration failed: Password must be at least 6 characters!");
            return;
        }

        // Ensure username is unique
        checkUsernameUnique(emailText, passwordText, usernameText, firstNameText, lastNameText);
    }

    private void checkUsernameUnique(String email, String password, String username, String firstName, String lastName) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        showToast("Registration failed: Username already taken!");
                    } else {
                        registerUser(email, password, username, firstName, lastName);
                    }
                })
                .addOnFailureListener(e -> showToast("Registration failed: Database error - " + e.getMessage()));
    }

    private void registerUser(String email, String password, String username, String firstName, String lastName) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();
                saveUserToFirestore(userId, email, username, firstName, lastName);
            } else {
                // Handle Firebase authentication errors
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    showToast("Registration failed: Email is already registered!");
                } else {
                    showToast("Registration failed: " + task.getException().getMessage());
                }
            }
        });
    }

    private void saveUserToFirestore(String userId, String email, String username, String firstName, String lastName) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("username", username);
        user.put("firstName", firstName);
        user.put("lastName", lastName);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(unused -> {
                    showToast("Registration Successful!");
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Registration failed: Firestore error - " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}