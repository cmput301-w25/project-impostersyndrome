package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.MoodDataCache;
import com.example.impostersyndrom.model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutPassword;
    private TextInputEditText loginEmail, loginPassword;
    private View loginProgressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // Ensure password toggle is set up initially and disable error icon
        layoutPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        layoutPassword.setErrorIconDrawable(null); // Prevent the red "!" from appearing

        // Set click listeners for buttons
        findViewById(R.id.loginBtn).setOnClickListener(v -> loginUser());
        findViewById(R.id.forgotPassword).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.newUserSignUp).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    /**
     * Handles the user login process.
     * Validates the email and password inputs, and authenticates the user using Firebase Authentication.
     */
    private void loginUser() {
        String emailText = loginEmail.getText().toString().trim();
        String passwordText = loginPassword.getText().toString().trim();

        // Validate email input
        if (emailText.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            layoutEmail.setError("Please enter a valid email");
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutEmail.setEndIconDrawable(R.drawable.ic_error);
            return;
        } else {
            layoutEmail.setError(null);
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        // Validate password input
        if (passwordText.isEmpty()) {
            layoutPassword.setError("Password is required");
            layoutPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE); // Ensure toggle remains
            layoutPassword.setErrorIconDrawable(null); // Prevent red "!" mark
            return;
        } else {
            layoutPassword.setError(null);
            layoutPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE); // Ensure toggle remains
            layoutPassword.setErrorIconDrawable(null); // Prevent red "!" mark
        }

        // Show progress bar and disable login button during login process
        loginProgressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.loginBtn).setEnabled(false);

        // Authenticate user with Firebase
        auth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                User.getInstance().setUserId(userId);

                // Pre-fetch mood data before navigating to MainActivity
                prefetchMoodData(userId);
            } else {
                // Hide progress bar and re-enable login button on failure
                loginProgressBar.setVisibility(View.GONE);
                findViewById(R.id.loginBtn).setEnabled(true);
                layoutPassword.setError("Wrong password. Try again or click Forgot password to reset it.");
                layoutPassword.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE); // Keep the eye icon
                layoutPassword.setErrorIconDrawable(null); // Prevent red "!" mark
            }
        });
    }

    /**
     * Pre-fetches mood data from Firestore for the logged-in user.
     */
    private void prefetchMoodData(String userId) {
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // Hide progress bar
                    loginProgressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Get the mood documents from the query result
                        QuerySnapshot snapshot = task.getResult();
                        List<DocumentSnapshot> moodDocs = snapshot.getDocuments();

                        // Cache the mood data in a global application class or singleton
                        MoodDataCache.getInstance().setMoodDocs(moodDocs);

                        // Navigate to MainActivity with pre-fetched data
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("dataPreloaded", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Navigate to MainActivity without pre-fetched data
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("dataPreloaded", false);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }
}