package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ForgotPasswordActivity allows users to reset their password by providing their registered email address.
 * It uses Firebase Authentication to send a password reset email to the user.
 *
 * @author
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutResetEmail; // Layout for the email input field
    private TextInputEditText resetEmail; // EditText for the email input
    private FirebaseAuth auth; // Firebase Authentication instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance();

        // Initialize views
        layoutResetEmail = findViewById(R.id.layoutResetEmail);
        resetEmail = findViewById(R.id.resetEmail);

        // Set click listener for the reset password button
        findViewById(R.id.resetPasswordBtn).setOnClickListener(v -> resetPassword());

        // Set click listener for the back to login button
        findViewById(R.id.backToLoginBtn).setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Handles the password reset process.
     * Validates the email input and sends a password reset email via Firebase Authentication.
     */
    private void resetPassword() {
        String email = resetEmail.getText().toString().trim();

        // Validate the email input
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutResetEmail.setError("Please enter a valid email");
            layoutResetEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutResetEmail.setEndIconDrawable(R.drawable.ic_error);
            return;
        } else {
            layoutResetEmail.setError(null);
            layoutResetEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        // Send password reset email
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Show success message
                        layoutResetEmail.setHelperText("✅ Password reset email sent! Check your inbox.");
                    } else {
                        // Show error message
                        layoutResetEmail.setError("Error: " + task.getException().getMessage());
                    }
                });
    }
}