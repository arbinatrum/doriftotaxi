package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CustomerRegLogActivity extends AppCompatActivity {

    TextView customerStatus, question;
    Button signInBtn, signUpBtn;
    EditText emailET, passwordET;
    TextView driverStatus;

    FirebaseUser currentUser;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_log);

        customerStatus = (TextView)findViewById(R.id.statusCustomer);
        question = (TextView)findViewById(R.id.accountCreateCustomer);
        signInBtn = (Button)findViewById(R.id.signInCustomer);
        signUpBtn = (Button)findViewById(R.id.signUpCustomer);
        emailET = (EditText)findViewById(R.id.customerEmail);
        passwordET = (EditText)findViewById(R.id.customerPassword);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        signUpBtn.setVisibility(View.INVISIBLE);
        signUpBtn.setEnabled(false);

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInBtn.setVisibility(View.INVISIBLE);
                question.setVisibility(View.INVISIBLE);
                signUpBtn.setVisibility(View.VISIBLE);
                signUpBtn.setEnabled(true);
                customerStatus.setText("Регистрация");
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                RegisterCustomer(email, password);
//                Intent intent = new Intent(CustomerRegLogActivity.this, DriverSettingsActivity.class);
//                intent.putExtra("type", "Register_Customer");
//                startActivity(intent);
                //Переход на страницу настроек сразу после регистрации
            }
        });

        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                SignInCustomer(email, password);
            }
        });
    }

    private void SignInCustomer(String email, String password) {
        loadingBar.setTitle("Вход");
        loadingBar.setMessage("Загрузка");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(CustomerRegLogActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent customerIntent = new Intent(CustomerRegLogActivity.this, CustomersMapActivity.class);
                    startActivity(customerIntent);
                }
                else{
                    Toast.makeText(CustomerRegLogActivity.this, "Ошибка Авторизации", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void RegisterCustomer(String email, String password) {
        loadingBar.setTitle("Регистрация");
        loadingBar.setMessage("Загрузка");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(CustomerRegLogActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent customerIntent = new Intent(CustomerRegLogActivity.this, CustomersMapActivity.class);
                    startActivity(customerIntent);
                }
                else{
                    Toast.makeText(CustomerRegLogActivity.this, "Ошибка Регистрации", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
}