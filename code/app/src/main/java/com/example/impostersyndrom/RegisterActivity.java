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

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout layoutFirstName, layoutLastName, layoutEmail, layoutPassword, layoutUsername;
    private TextInputEditText firstName, lastName, email, password, username;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        findViewById(R.id.registerBtn).setOnClickListener(v -> validateAndCheckUsername());

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void validateAndCheckUsername() {
        String firstNameText = firstName.getText().toString().trim();
        String lastNameText = lastName.getText().toString().trim();
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String usernameText = username.getText().toString().trim();

        boolean isValid = true;

        if (firstNameText.isEmpty()) {
            layoutFirstName.setError("First name is required");
            isValid = false;
        } else layoutFirstName.setError(null);

        if (lastNameText.isEmpty()) {
            layoutLastName.setError("Last name is required");
            isValid = false;
        } else layoutLastName.setError(null);

        if (!isUsernameValid(usernameText)) {
            layoutUsername.setError("Username must be 5-20 alphanumeric characters!");
            isValid = false;
        } else layoutUsername.setError(null);

        if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            layoutEmail.setError("Please enter a valid email");
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutEmail.setEndIconDrawable(R.drawable.ic_error);
            isValid = false;
        } else {
            layoutEmail.setError(null);
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        if (passwordText.isEmpty() || passwordText.length() < 6) {
            layoutPassword.setError("Password must be at least 6 characters");
            isValid = false;
        } else layoutPassword.setError(null);

        if (!isValid) return;

        db.collection("users").whereEqualTo("username", usernameText).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            layoutUsername.setError("Username is already taken");
                        } else {
                            registerUser(emailText, passwordText, firstNameText, lastNameText, usernameText);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Database error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isUsernameValid(String username) {
        return Pattern.matches("^[a-zA-Z0-9]{5,20}$", username);
    }
    private void registerUser(String email, String password, String firstName, String lastName, String username) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();

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

    static class UserModel {
        public String email, username, firstName, lastName;

        public UserModel(String email, String username, String firstName, String lastName) {
            this.email = email;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
