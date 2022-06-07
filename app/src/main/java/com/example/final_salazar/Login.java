package com.example.final_salazar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class Login extends AppCompatActivity {

    TextView registerNowButton;
    Button loginButton;
    TextInputEditText emailText, passwordText;

    FirebaseAuth mAuth;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        registerNowButton = findViewById(R.id.registerNowButton);
        registerNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, Register.class));
            }
        });
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userLogin();
            }
        });

        emailText = findViewById(R.id.emailAddress);
        passwordText = findViewById(R.id.password);

        progressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(Login.this, MainActivity.class));
            finish();
        }
    }

    private void userLogin() {
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();

        if (email.isEmpty()) {
            emailText.setError("Email is required!");
            emailText.requestFocus();
            progressDialog.dismiss();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("Please provide valid Email!");
            emailText.requestFocus();
            progressDialog.dismiss();
            return;
        }
        if (password.isEmpty()) {
            passwordText.setError("Password is required!");
            passwordText.requestFocus();
            progressDialog.dismiss();
            return;
        }
        if (password.length() < 6) {
            passwordText.setError("Min password length should be 6 characters!");
            passwordText.requestFocus();
            progressDialog.dismiss();
            return;
        }

        progressDialog.setMessage("Logging in...Please Wait");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                            progressDialog.dismiss();

                        } else {
                            try {
                                throw task.getException();
                            }
                            // if user enters wrong email.
                            catch (FirebaseAuthInvalidUserException invalidEmail) {
                                Log.d("TAG", "onComplete: invalid_email");
                                emailText.setError("Invalid Email not registered");
                                emailText.requestFocus();
                                progressDialog.dismiss();
                            }
                            // if user enters wrong password.
                            catch (FirebaseAuthInvalidCredentialsException wrongPassword) {
                                Log.d("TAG", "onComplete: wrong_password");
                                passwordText.setError("Wrong Password");
                                passwordText.requestFocus();
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                Toast.makeText(Login.this, "Failed to login!", Toast.LENGTH_LONG).show();
                                Log.d("TAG", "onComplete: " + e.getMessage());
                                progressDialog.dismiss();
                            }
                        }
                    }
                });
    }
}