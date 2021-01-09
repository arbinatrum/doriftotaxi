package com.example.doriftotaxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.hdodenhof.circleimageview.CircleImageView;

public class DriverSettingsActivity extends AppCompatActivity {

    private String getType;

    //private CircleImageView circleImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        //getType = getIntent().getStringExtra("type");


        //circleImageView = (CircleImageView)findViewById(R.id.profile_image);
        EditText nameET = findViewById(R.id.name_text);
        EditText phoneET = findViewById(R.id.phone_number_text);
        EditText carET = findViewById(R.id.car_model_text);
        EditText carNumberET = findViewById(R.id.car_number_text);
        Button saveBtn = findViewById(R.id.save_changes_btn);
        ImageView backBtn = findViewById(R.id.close_btn);
        TextView imageChangeBtn = findViewById(R.id.change_photo_btn);

        /*if (getType.equals("Register_Driver")){
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(DriverSettingsActivity.this, DriverRegLoginActivity.class));
                }
            });
        }*/

        //if (getType.equals(false)){ Toast.makeText(DriverSettingsActivity.this, "Неизвестная ошибка", Toast.LENGTH_SHORT).show(); }
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getType.equals("Drivers")){
                    startActivity(new Intent(DriverSettingsActivity.this, DriversMapActivity.class));
                }
                //else{ Toast.makeText(DriverSettingsActivity.this, "Неизвестная ошибка", Toast.LENGTH_SHORT).show();}
            }
        });

    }
}