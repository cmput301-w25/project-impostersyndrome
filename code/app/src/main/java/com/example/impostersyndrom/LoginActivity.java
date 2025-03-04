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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        findViewById(R.id.loginBtn).setOnClickListener(v -> loginUser());
        findViewById(R.id.forgotPassword).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.newUserSignUp).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String emailText = loginEmail.getText().toString().trim();
        String passwordText = loginPassword.getText().toString().trim();

        if (emailText.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            layoutEmail.setError("Please enter a valid email");
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutEmail.setEndIconDrawable(R.drawable.ic_error);
            return;
        } else {
            layoutEmail.setError(null);
            layoutEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        if (passwordText.isEmpty()) {
            layoutPassword.setError("Password is required");
            return;
        } else {
            layoutPassword.setError(null);
        }

        // Show progress bar and disable login button
        loginProgressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.loginBtn).setEnabled(false);

        auth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                User.getInstance().setUserId(userId);
                showToast("Login Successful!");

                // Instead of immediately starting MainActivity, prefetch mood data
                prefetchMoodData(userId);
            } else {
                // Hide progress bar and re-enable login button on failure
                loginProgressBar.setVisibility(View.GONE);
                findViewById(R.id.loginBtn).setEnabled(true);
                showToast("Login Failed: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    private void prefetchMoodData(String userId) {
        // Fetch the mood data before transitioning to MainActivity
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // Hide progress bar
                    loginProgressBar.setVisibility(View.GONE);

                    // Get the mood data
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        List<DocumentSnapshot> moodDocs = snapshot.getDocuments();

                        // Store the mood data in a global application class or singleton
                        MoodDataCache.getInstance().setMoodDocs(moodDocs);

                        // Now transition to MainActivity with pre-fetched data
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("dataPreloaded", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Even if there's an error, still transition to MainActivity
                        // but let it know data wasn't pre-loaded
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("dataPreloaded", false);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
