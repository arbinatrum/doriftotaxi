package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverSettingsActivity extends AppCompatActivity {

    private String getType;

    private CircleImageView circleImageView;
    private EditText nameET, phoneET, carET, carNumberET;
    private ImageView backBtn;
    private Button saveBtn;
    private TextView imageChangeBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private Uri imageUri;
    private String myUrl = "";
    private StorageTask uploadTask;
    private StorageReference storageProfileImageRef;

    private String checker = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        getType = getIntent().getStringExtra("type");


        circleImageView = (CircleImageView)findViewById(R.id.profile_image_change);
        nameET = findViewById(R.id.name_text);
        phoneET = findViewById(R.id.phone_number_text);
        carET = findViewById(R.id.car_model_text);
        carNumberET = findViewById(R.id.car_number_text);
        saveBtn = findViewById(R.id.save_changes_btn);
        backBtn = findViewById(R.id.close_btn);
        imageChangeBtn = findViewById(R.id.change_photo_btn);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");

        storageProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        if(getType.equals("Customers")){
            nameET.setHint("Имя");
            carET.setVisibility(View.INVISIBLE);
            carET.setEnabled(false);
            carNumberET.setVisibility(View.INVISIBLE);
            carNumberET.setEnabled(false);
            imageChangeBtn.setVisibility(View.INVISIBLE);
            imageChangeBtn.setEnabled(false);
            Picasso.get().load(R.drawable.icon).into(circleImageView);
        }

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getType.equals("Drivers")) {
                    startActivity(new Intent(DriverSettingsActivity.this, DriversMapActivity.class));
                }
                else {
                    startActivity(new Intent(DriverSettingsActivity.this, CustomersMapActivity.class));
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checker.equals("clicked")){
                    ValidateControllers();
                }
                else {
                    ValidateAndSaveOnlyInformation();
                }
            }
        });

        imageChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";

                CropImage.activity().setAspectRatio(1,1).start(DriverSettingsActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            circleImageView.setImageURI(imageUri);
        }else {
            if(getType.equals("Drivers")){
                startActivity(new Intent(DriverSettingsActivity.this, DriversMapActivity.class));
            } else{
                startActivity(new Intent(DriverSettingsActivity.this, CustomersMapActivity.class));
            }
            Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
        }

        getUserInformation();
    }

    private void ValidateControllers(){
        if(getType.equals("Drivers")) {
            if (TextUtils.isEmpty(nameET.getText().toString())) {
                Toast.makeText(this, "Заполните поле ФИО", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(phoneET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер телефона", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(carET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Модель машины", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(carNumberET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер машины", Toast.LENGTH_SHORT).show();
            }
        }else {
            if (TextUtils.isEmpty(nameET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Имя", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(phoneET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер телефона", Toast.LENGTH_SHORT).show();
            }
            else if(checker.equals("clicked")){
                uploadProfileImage();
            }

        }

    }

    private void uploadProfileImage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Загрузка информации");
        progressDialog.setMessage("Пожалуйста, подождите");
        progressDialog.show();
        if(imageUri != null){
            final StorageReference fileRef = storageProfileImageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");

            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task <Uri> task) {
                 if(task.isSuccessful()){
                     Uri downloadUrl = task.getResult();
                     myUrl = downloadUrl.toString();

                     HashMap<String, Object> userMap = new HashMap<>();
                     userMap.put("uid", mAuth.getCurrentUser().getUid());
                     userMap.put("name", nameET.getText().toString());
                     userMap.put("phone", phoneET.getText().toString());


                     if(getType.equals("Drivers")){
                         userMap.put("car model", carET.getText().toString());
                         userMap.put("car number", carNumberET.getText().toString());
                         userMap.put("image", myUrl);
                     }

                     databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
                     //Отправляем все данные, включая картинку в FirebaseDatabase

                     progressDialog.dismiss();
                     if(getType.equals("Drivers")) {
                         startActivity(new Intent(DriverSettingsActivity.this, DriversMapActivity.class));
                     }
                     else {
                         startActivity(new Intent(DriverSettingsActivity.this, CustomersMapActivity.class));
                     }
                 }
                }
            });
        }
        else {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
        }
    }

    private void ValidateAndSaveOnlyInformation() {
        if(getType.equals("Drivers")) {
            if (TextUtils.isEmpty(nameET.getText().toString())) {
                Toast.makeText(this, "Заполните поле ФИО", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(phoneET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер телефона", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(carET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Модель машины", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(carNumberET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер машины", Toast.LENGTH_SHORT).show();
            }
        }else {
            if (TextUtils.isEmpty(nameET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Имя", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(phoneET.getText().toString())) {
                Toast.makeText(this, "Заполните поле Номер телефона", Toast.LENGTH_SHORT).show();
            } else {
                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("uid", mAuth.getCurrentUser().getUid());
                userMap.put("name", nameET.getText().toString());
                userMap.put("phone", phoneET.getText().toString());


                if (getType.equals("Drivers")) {
                    userMap.put("car model", carET.getText().toString());
                    userMap.put("car number", carNumberET.getText().toString());
                }

                databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
                //Отправляем все данные, включая картинку в FirebaseDatabase

                if (getType.equals("Drivers")) {
                    startActivity(new Intent(DriverSettingsActivity.this, DriversMapActivity.class));
                } else {
                    startActivity(new Intent(DriverSettingsActivity.this, CustomersMapActivity.class));
                }
            }
        }
    }

    private void getUserInformation() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount()>0){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    nameET.setText(name);
                    phoneET.setText(phone);
                    if (getType.equals("Drivers")) {
                        String carmodel = snapshot.child("car model").getValue().toString();
                        String carnumber = snapshot.child("car number").getValue().toString();
                        carET.setText(carmodel);
                        carNumberET.setText(carnumber);

                        if(snapshot.hasChild("image")) {
                            String image = snapshot.child("image").getValue().toString();
                            Picasso.get().load(image).into(circleImageView);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}