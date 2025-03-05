package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Objects;

/**
 * LoginActivity handles user authentication and login functionality.
 * It allows users to log in using their email and password, and pre-fetches mood data
 * from Firestore before navigating to the MainActivity.
 *
 * @author
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutPassword; // Layouts for email and password input fields
    private TextInputEditText loginEmail, loginPassword; // EditTexts for email and password input
    private View loginProgressBar; // Progress bar to show during login process
    private FirebaseAuth auth; // Firebase Authentication instance
    private FirebaseFirestore db; // Firestore database instance

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
            return;
        } else {
            layoutPassword.setError(null);
        }

        // Show progress bar and disable login button during login process
        loginProgressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.loginBtn).setEnabled(false);

        // Authenticate user with Firebase
        auth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                User.getInstance().setUserId(userId);
                showToast("Login Successful!");

                // Pre-fetch mood data before navigating to MainActivity
                prefetchMoodData(userId);
            } else {
                // Hide progress bar and re-enable login button on failure
                loginProgressBar.setVisibility(View.GONE);
                findViewById(R.id.loginBtn).setEnabled(true);
                showToast("Login Failed: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    /**
     * Pre-fetches mood data from Firestore for the logged-in user.
     * Once the data is fetched, it is cached and the user is navigated to MainActivity.
     *
     * @param userId The ID of the logged-in user.
     */
    private void prefetchMoodData(String userId) {
        // Fetch mood data from Firestore
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

    /**
     * Validates an email address.
     *
     * @param email The email address to validate.
     * @return True if the email is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Displays a toast message.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}