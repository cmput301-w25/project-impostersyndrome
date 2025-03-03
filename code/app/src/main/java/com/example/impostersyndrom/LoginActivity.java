package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail, layoutPassword;
    private TextInputEditText loginEmail, loginPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);

        findViewById(R.id.loginBtn).setOnClickListener(v -> loginUser());
        findViewById(R.id.forgotPassword).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.newUserSignUp).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String emailText = loginEmail.getText().toString().trim();
        String passwordText = loginPassword.getText().toString().trim();

        if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
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

        auth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                layoutEmail.setError("Login failed: " + task.getException().getMessage());
            }
        });
    }
}
