package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText resetEmail;
    private Button resetPasswordBtn, backToLoginBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        resetEmail = findViewById(R.id.resetEmail);
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn);
        backToLoginBtn = findViewById(R.id.backToLoginBtn);

        resetPasswordBtn.setOnClickListener(v -> resetPassword());
        backToLoginBtn.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void resetPassword() {
        String email = resetEmail.getText().toString().trim();

        if (!isValidEmail(email)) {
            showToast("Type a valid email!");
            return;
        }

        // Attempt to send password reset email directly
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent! Check your inbox.");
                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        // Handle errors properly
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email!";
                        showToast("Error: " + errorMessage);
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}