package com.example.doriftotaxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class CustomerRegLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_log);

        Intent driverIntent = new Intent(CustomerRegLogActivity.this, DriversMapActivity.class);
        startActivity(driverIntent);
    }
}