package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText loginEmail, loginPassword;
    private Button loginBtn;
    private TextView forgotPassword, newUserSignUp;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.loginBtn);
        forgotPassword = findViewById(R.id.forgotPassword);
        newUserSignUp = findViewById(R.id.newUserSignUp);

        loginBtn.setOnClickListener(v -> loginUser());

        // Navigate to Forgot Password Page
        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Navigate to Register Page
        newUserSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String emailText = loginEmail.getText().toString().trim();
        String passwordText = loginPassword.getText().toString().trim();

        if (!isValidEmail(emailText) || TextUtils.isEmpty(passwordText)) {
            showToast("Enter a valid email and password!");
            return;
        }

        auth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                User.getInstance().setUserId(Objects.requireNonNull(auth.getCurrentUser()).getUid());
                showToast("Login Successful!");
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("userId", userId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                startActivity(intent);
                finish();
            } else {
                showToast("Login Failed: " + task.getException().getMessage());
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