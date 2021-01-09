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

    private CircleImageView circleImageView;
    private EditText nameET, phoneET, carET, carNumberET;
    private ImageView backBtn;
    private TextView imageChangeBtn;
    private Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        getType = getIntent().getStringExtra("type");


        //circleImageView = (CircleImageView)findViewById(R.id.profile_image);
        nameET = (EditText) findViewById(R.id.name_text);
        phoneET = (EditText)findViewById(R.id.phone_number_text);
        carET = (EditText)findViewById(R.id.car_model_text);
        carNumberET = (EditText)findViewById(R.id.car_number_text);
        saveBtn = (Button) findViewById(R.id.save_changes_btn);
        backBtn = (ImageView)findViewById(R.id.close_btn);
        imageChangeBtn = (TextView)findViewById(R.id.change_photo_btn);

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