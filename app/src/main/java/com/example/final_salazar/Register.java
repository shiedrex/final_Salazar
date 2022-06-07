package com.example.final_salazar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class Register extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView loginNowButton;
    private Button registerButton;
    private TextInputEditText emailText, passwordText, confirmPasswordText;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        loginNowButton = findViewById(R.id.loginNowButton);
        loginNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        emailText = findViewById(R.id.emailAddress);
        passwordText = findViewById(R.id.password);
        confirmPasswordText = findViewById(R.id.confirmPassword);
    }

    private void submit() {
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();
        String confirmPassword = confirmPasswordText.getText().toString().trim();

        if (email.isEmpty()) {
            emailText.setError("Email is required!");
            emailText.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("Please provide valid Email!");
            emailText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordText.setError("Password is required!");
            passwordText.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordText.setError("Min password length should be 6 characters!");
            passwordText.requestFocus();
            return;
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordText.setError("Confirm your password!");
            confirmPasswordText.requestFocus();
            return;
        }
        if (!confirmPassword.matches(password)) {
            confirmPasswordText.setError("Password doesn't match!");
            confirmPasswordText.requestFocus();
            return;
        }

        progressDialog.setMessage("Registering...Please Wait");
        progressDialog.show();


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            User user = new User(email);

                            FirebaseDatabase.getInstance().getReference("players")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                progressDialog.dismiss();
                                                Toast.makeText(Register.this, "User has been registered successfully!", Toast.LENGTH_LONG).show();
                                                finish();
                                            } else {
                                                progressDialog.dismiss();
                                                Toast.makeText(Register.this, "Failed to register! Try Again!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthUserCollisionException existEmail) {
                                Log.d("TAG", "onComplete: exist_email");
                                emailText.setError("Email already exists");
                                emailText.requestFocus();
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(Register.this, "Failed to register! Try Again!", Toast.LENGTH_LONG).show();
                                Log.d("TAG", "onComplete: " + e.getMessage());
                            }
                        }
                    }
                });
    }
}