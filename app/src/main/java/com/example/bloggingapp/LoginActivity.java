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

import com.example.bloggingapp.MainActivityPackage.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText txtLogInPass, txtLogInEmail;
    private ProgressBar loading;
    private FirebaseAuth firebaseAuth;
    private Button btnLogin, btnRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtLogInPass = findViewById(R.id.txtLogInPass);
        txtLogInEmail = findViewById(R.id.txtLogInEmail);
        loading = findViewById(R.id.loading);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
                btnRegister.setEnabled(false);
                login();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        Button btnGenerate = findViewById(R.id.btnGenerate);

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtLogInEmail.setText("mae06@gmail.com");
                txtLogInPass.setText("password");
            }
        });
    }

    private void login() {
        String email = txtLogInEmail.getText().toString();
        String pass = txtLogInPass.getText().toString();

        if (!TextUtils.isEmpty(email) || !TextUtils.isEmpty(pass)) {
            firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                        openMainApp();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    btnLogin.setEnabled(true);
                    btnRegister.setEnabled(true);
                    loading.setVisibility(View.GONE);
                }
            });
        } else Toast.makeText(LoginActivity.this, "Empty Fields", Toast.LENGTH_SHORT).show();
    }

    private void openMainApp() {
        startActivity(new Intent(LoginActivity.this, MainPage.class));
        finish();
    }

}