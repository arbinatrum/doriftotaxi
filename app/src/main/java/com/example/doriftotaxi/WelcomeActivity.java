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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initialize();
        driverBtn = (Button) findViewById(R.id.driverBtn);
        customerBtn = (Button) findViewById(R.id.customerBtn);

        if(CurrentUser != null){
            openMap();
        }

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

        checkPermissions();
    }

    private void openMap() {
        /*final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Загрузка информации");
        progressDialog.setMessage("Пожалуйста, подождите");
        progressDialog.show();*/
        //progressDialog.dismiss();

        DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference();

        databaseReference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("Users").child("Drivers").child(CurrentUser.getUid()).exists()){
                    if(snapshot.child("Users").child("Drivers").child(CurrentUser.getUid()).getChildrenCount() == 0){
                        Intent driverIntent = new Intent(WelcomeActivity.this, DriverSettingsActivity.class);
                        driverIntent.putExtra("type", "Drivers");
                        startActivity(driverIntent);
                    } else {
                        startActivity(new Intent(WelcomeActivity.this, DriversMapActivity.class));
                    }
                }else if(snapshot.child("Users").child("Customers").child(CurrentUser.getUid()).exists()){
                    startActivity(new Intent(WelcomeActivity.this, CustomersMapActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initialize() {
        mAuth = FirebaseAuth.getInstance();
        CurrentUser = mAuth.getCurrentUser();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults[0] == RESULT_CANCELED && requestCode == 100){
            Toast.makeText(this, "Выданы пермишены, не забудьте включить GPS!", Toast.LENGTH_SHORT).show();
        } else {
            checkPermissions();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }

        /*AccessibilityService mContext = null;
        LocationManager mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        boolean mIsGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean mIsNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean mIsGeoDisabled = !mIsGPSEnabled && !mIsNetworkEnabled;

        if(mIsGeoDisabled) startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));*/
    }
}