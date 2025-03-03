package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutResetEmail;
    private TextInputEditText resetEmail;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();

        layoutResetEmail = findViewById(R.id.layoutResetEmail);
        resetEmail = findViewById(R.id.resetEmail);

        findViewById(R.id.resetPasswordBtn).setOnClickListener(v -> resetPassword());
        findViewById(R.id.backToLoginBtn).setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void resetPassword() {
        String email = resetEmail.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutResetEmail.setError("Please enter a valid email");
            layoutResetEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            layoutResetEmail.setEndIconDrawable(R.drawable.ic_error);
            return;
        } else {
            layoutResetEmail.setError(null);
            layoutResetEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        layoutResetEmail.setHelperText("✅ Password reset email sent! Check your inbox.");
                    } else {
                        layoutResetEmail.setError("Error: " + task.getException().getMessage());
                    }
                });
    }
}
