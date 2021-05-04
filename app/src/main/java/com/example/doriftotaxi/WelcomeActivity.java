package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class WelcomeActivity extends AppCompatActivity {

    Button driverBtn, customerBtn;

    private FirebaseAuth mAuth;
    private FirebaseUser CurrentUser;
    ProgressDialog progressDialog;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initialize();
        driverBtn = (Button) findViewById(R.id.driverBtn);
        customerBtn = (Button) findViewById(R.id.customerBtn);

        progressDialog = new ProgressDialog(WelcomeActivity.this);
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressDialog.setCancelable(false);
        if(CurrentUser != null){
            openMap();
        } else progressDialog.dismiss();


        driverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent driverIntent = new Intent(WelcomeActivity.this, DriverRegLoginActivity.class);
                startActivity(driverIntent);
            }
        });

        customerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent customerIntent = new Intent(WelcomeActivity.this, CustomerRegLogActivity.class);
                startActivity(customerIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(CurrentUser != null){
            openMap();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(CurrentUser != null){
            openMap();
        }
    }

    private void openMap() {
        /*final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Загрузка информации");
        progressDialog.setMessage("Пожалуйста, подождите");
        progressDialog.show();*/
        //progressDialog.dismiss();

        /*DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference();
        databaseReference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("Users").child("Drivers").child(CurrentUser.getUid()).exists()){
                    progressDialog.dismiss();
                    if(snapshot.child("Users").child("Drivers").child(CurrentUser.getUid()).getChildrenCount() == 0){
                        Intent driverIntent = new Intent(WelcomeActivity.this, DriverSettingsActivity.class);
                        driverIntent.putExtra("type", "Drivers");
                        startActivity(driverIntent);
                    } else {
                        startActivity(new Intent(WelcomeActivity.this, DriversMapActivity.class));
                    }
                }else if(snapshot.child("Users").child("Customers").child(CurrentUser.getUid()).exists()){
                    progressDialog.dismiss();
                    startActivity(new Intent(WelcomeActivity.this, CustomersMapActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });*/

        databaseReference.child("Customers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(mAuth.getCurrentUser().getUid()).exists())
                {
                    progressDialog.dismiss();
                    startActivity(new Intent(WelcomeActivity.this, CustomersMapActivity.class));
                }
                else
                {
                    progressDialog.dismiss();
                    startActivity(new Intent(WelcomeActivity.this, DriversMapActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initialize() {
        mAuth = FirebaseAuth.getInstance();
        CurrentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //progressDialog.dismiss();
    }
}