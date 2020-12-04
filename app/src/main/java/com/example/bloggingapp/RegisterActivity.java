package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText txtRegPass, txtRegConfirmPass, txtRegEmail;
    private ProgressBar loading;
    private FirebaseAuth firebaseAuth;
    private Button btnRegCreate, btnRegLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtRegPass = findViewById(R.id.txtRegPass);
        txtRegEmail = findViewById(R.id.txtRegEmail);
        txtRegConfirmPass = findViewById(R.id.txtRegConfirm);
        loading = findViewById(R.id.loading);
        firebaseAuth = FirebaseAuth.getInstance();

        btnRegCreate = findViewById(R.id.btnRegCreate);
        btnRegLogin = findViewById(R.id.btnRegLogin);

        btnRegCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPassword();
            }
        });

        btnRegLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openLogin();
            }
        });
    }

    private void checkPassword() {
        String email = txtRegEmail.getText().toString();
        String pass = txtRegPass.getText().toString();
        String confirmPass = txtRegConfirmPass.getText().toString();

        if (!TextUtils.isEmpty(email) || !TextUtils.isEmpty(pass) || !TextUtils.isEmpty(confirmPass)) {
            if (pass.equals(confirmPass)) {
                createUser(email, pass);
            } else {
                Toast.makeText(this, "Password Does not Match!", Toast.LENGTH_SHORT).show();
            }
        } else Toast.makeText(this, "Empty Fields", Toast.LENGTH_SHORT).show();
    }

    private void createUser(String email, String pass) {
        loading.setVisibility(View.VISIBLE);
        btnRegCreate.setEnabled(false);
        btnRegLogin.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "User Created Successfully!", Toast.LENGTH_SHORT).show();
                    openLogin();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
                btnRegCreate.setEnabled(true);
                btnRegLogin.setEnabled(true);
                loading.setVisibility(View.GONE);
            }
        });
    }

    private void openLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }
}