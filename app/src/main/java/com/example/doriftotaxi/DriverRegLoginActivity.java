package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverRegLoginActivity extends AppCompatActivity {

    TextView driverStatus, question;
    Button signInBtn, signUpBtn;
    EditText emailET, passwordET;


    FirebaseAuth mAuth;
    FirebaseUser CurrentUser;
    DatabaseReference DriverDatabaseRef;
    String OnlineDriverID;


    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_login);



        driverStatus = (TextView)findViewById(R.id.statusDriver);
        question = (TextView)findViewById(R.id.accountCreate);
        signInBtn = (Button)findViewById(R.id.signInDriver);
        signUpBtn = (Button)findViewById(R.id.signUpDriver);
        emailET = (EditText)findViewById(R.id.driverEmail);
        passwordET = (EditText)findViewById(R.id.driverPassword);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        CurrentUser = mAuth.getCurrentUser();

        if(CurrentUser != null){
            openMap();
        }

        signUpBtn.setVisibility(View.INVISIBLE);
        signUpBtn.setEnabled(false);

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInBtn.setVisibility(View.INVISIBLE);
                question.setVisibility(View.INVISIBLE);
                signUpBtn.setVisibility(View.VISIBLE);
                signUpBtn.setEnabled(true);
                driverStatus.setText("Регистрация");
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                if(email.isEmpty() | password.isEmpty()){
                    Toast.makeText(DriverRegLoginActivity.this, "Введите корректные данные!", Toast.LENGTH_SHORT).show();
                }else {
                    RegisterDriver(email, password);
                }
            }
        });

        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();

                if(email.isEmpty() | password.isEmpty()){
                    Toast.makeText(DriverRegLoginActivity.this, "Введите корректные данные!", Toast.LENGTH_SHORT).show();
                }else {
                    SignInDriver(email, password);
                }
            }
        });
    }

    private void openMap() {
        DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference();

        databaseReference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("Users").child("Drivers").child(CurrentUser.getUid()).exists()){
                    startActivity(new Intent(DriverRegLoginActivity.this, DriversMapActivity.class));
                }else if(snapshot.child("Users").child("Customers").child(CurrentUser.getUid()).exists()){
                    startActivity(new Intent(DriverRegLoginActivity.this, CustomersMapActivity.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void SignInDriver(String email, String password) {
        loadingBar.setTitle("Вход");
        loadingBar.setMessage("Загрузка");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(DriverRegLoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent driverIntent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                    startActivity(driverIntent);
                }
                else{
                    Toast.makeText(DriverRegLoginActivity.this, "Ошибка Авторизации", Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void RegisterDriver(String email, String password) {
        loadingBar.setTitle("Регистрация");
        loadingBar.setMessage("Загрузка");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if(task.isSuccessful()){

                OnlineDriverID = mAuth.getCurrentUser().getUid();
                DriverDatabaseRef = FirebaseDatabase.getInstance().getReference().
                        child("Users").child("Drivers").child(OnlineDriverID);
                DriverDatabaseRef.setValue(true); //Помещение информации о заказчике в отдельную папку Users->Drivers со своими данными


                Intent driverIntent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                startActivity(driverIntent);

                Toast.makeText(DriverRegLoginActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
            else{
                Toast.makeText(DriverRegLoginActivity.this, "Ошибка Регистрации", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    });
    }
}