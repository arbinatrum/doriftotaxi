package com.example.doriftotaxi;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    double lat = 0, lon = 0;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                //Log.d("TAG", "onLocationResult равен нулю ");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                //displayLocation();
                //Log.d("TAG", "onLocationResult: " + location.toString());
                lastLocation = location;
                if(lastLocation != null && requestType && customerID == ""){
                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                    geoFire.setLocation(driverID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                }
                if(lastLocation != null && !requestType && customerID != "" && orderID != ""){
                    DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(orderID);
                    GeoFire geoFire = new GeoFire(orderRef);
                    geoFire.setLocation("DriverLocation", new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                }
            }
            if(status && lastLocation!= null){
                zoomToUserLocation();
                status = false;
            }
            if(mMap != null){
                setUserLocationMarker(locationResult.getLastLocation());
            }
            if(DistanceCheckStatus){
                Location location = new Location("");
                location.setLatitude(PickUpMarker.getPosition().latitude);
                location.setLongitude(PickUpMarker.getPosition().longitude);
                double distance = lastLocation.distanceTo(location);

                if(distance < 100){
                    DistanceCheckStatus = false;
                    DriverInfoBtn.setVisibility(View.VISIBLE);
                    DriverInfoBtn.setEnabled(true);
                }
            }
            if(RoadToLastPointStatus){
                Location location = new Location("");
                location.setLatitude(RoadMarker.getPosition().latitude);
                location.setLongitude(RoadMarker.getPosition().longitude);
                double distance = lastLocation.distanceTo(location);

                if(distance < 100) {
                    RoadToLastPointStatus = false;
                    DriverInfoBtn.setEnabled(true);
                    DriverInfoBtn.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private GoogleMap mMap;
    private Geocoder geocoder;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationManager locationManager;
    Marker PickUpMarker, RoadMarker;
    Marker userLocationMarker;

    private String driverID, customerID = "";
    private Button DriverApprovedButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference DriverDatabaseRef, CustomersRef;
    private LatLng DriverPosition;
    private boolean status = true, requestType = false;

    //Диалоговое окно
    private Dialog dialog;
    private TextView pointA, pointB, textOrderFound, finishPrice;
    private Button acceptBtn, rejectBtn;
    private LinearLayout linearLayout;

    //Информация о заказчике
    private TextView txtName, txtPhone, txtAdress;
    private Button DriverInfoBtn;
    private LinearLayout LinInfo;
    private String finalAdress;

    private Boolean currentLogoutDriverStatus = false;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPosition;

    private ValueEventListener DriverLocationRefListener;
    private ValueEventListener assignedCustomerRefListener;
    private int it = 0;
    private String orderID = "";
    private String pointBString = "", pointAString = "";
    private boolean DistanceCheckStatus = false;
    private boolean RoadToLastPointStatus = false;
    private String price = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);

        Button logoutDriverButton = (Button) findViewById(R.id.driver_logout_button);
        Button settingsDriverButton = (Button) findViewById(R.id.driver_settings_button);
        DriverApprovedButton = (Button)findViewById(R.id.driver_Approved_button);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();
        DriverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

        dialog = new Dialog(DriversMapActivity.this);
        dialog.setContentView(R.layout.dialog_fragment_driver);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

        textOrderFound = dialog.findViewById(R.id.text_order_found);
        acceptBtn = dialog.findViewById(R.id.driver_accept_button);
        rejectBtn = dialog.findViewById(R.id.driver_reject_button);
        pointA = dialog.findViewById(R.id.pointA);
        pointB = dialog.findViewById(R.id.pointB);
        linearLayout = dialog.findViewById(R.id.lin_layout);
        finishPrice = dialog.findViewById(R.id.finish_price);

        finishPrice.setVisibility(View.GONE);

        //Информация о такси
        txtName = (TextView) findViewById(R.id.customer_name);
        txtPhone = (TextView) findViewById(R.id.customer_phone);
        txtAdress = (TextView) findViewById(R.id.customer_address);
        LinInfo = findViewById(R.id.lin_info_for_drivers);
        DriverInfoBtn = findViewById(R.id.driver_status_button);

        LinInfo.setVisibility(View.GONE);
        LinInfo.setEnabled(false);
        DriverInfoBtn.setVisibility(View.GONE);
        DriverInfoBtn.setEnabled(false);
        DriverInfoBtn.setText("Клиент в машине");

        //Слушатель кнопки на прием заказа
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DriverApprovedButton.getText() == "Отменить заказ" & acceptBtn.getText().toString() != "Оплачено"){ //Отменить заказ когда он уже создан и таксист решил отменить заказ до того, как заказчик сел в машину
                    dialog.dismiss();
                    DisconnectDriver();
                    requestType = false;
                    customerID = "";
                    orderID = "";
                    it = 1;
                    LinInfo.setVisibility(View.GONE);
                    LinInfo.setEnabled(false);
                    DriverInfoBtn.setVisibility(View.GONE);
                    DriverInfoBtn.setEnabled(false);

                    if (PickUpMarker != null & RoadMarker != null) {
                        PickUpMarker.remove();
                        RoadMarker.remove();
                    }
                    DriverApprovedButton.setText("Начать поиск заказов");
                    zoomToUserLocation();
                } else if(textOrderFound.getText().toString() == "Новый заказ поблизости!" & acceptBtn.getText().toString() != "Оплачено"){ //Принять заказ при нахождении заказчика
                    dialog.dismiss();
                    it = 1;

                    assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverID).child("CustomerRideID");
                    assignedCustomerRef.child(customerID).removeValue();

                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                    geoFire.removeLocation(driverID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            requestType = false;
                            Toast.makeText(DriversMapActivity.this, "Заказ принят", Toast.LENGTH_SHORT).show();
                            createOrder();
                        }
                    });
                } else if(acceptBtn.getText().toString() == "Оплачено"){ //Окончание заказа, таксист привез заказчика на место назначения
                    acceptBtn.setEnabled(false);
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                            .child("Orders")
                            .child(orderID)
                            .child("status");
                    databaseReference.setValue("Закончен", new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
                                    .child("Orders")
                                    .child(orderID);
                            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("endTimeTrip", date);

                            orderRef.updateChildren(userMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    dialog.dismiss();
                                    if (RoadMarker != null) {
                                        RoadMarker.remove();
                                    }
                                    customerID = "";
                                    orderID = "";
                                    it = 1;
                                    LinInfo.setVisibility(View.GONE);
                                    LinInfo.setEnabled(false);
                                    DriverInfoBtn.setVisibility(View.GONE);
                                    DriverInfoBtn.setEnabled(false);
                                    linearLayout.setVisibility(View.VISIBLE);
                                    finishPrice.setVisibility(View.GONE);
                                    acceptBtn.setText("Принять");
                                    acceptBtn.setEnabled(true);
                                    rejectBtn.setText("Отклонить");
                                    textOrderFound.setText("Новый заказ поблизости!");
                                    rejectBtn.setEnabled(true);

                                    DriverApprovedButton.setVisibility(View.VISIBLE);
                                    DriverApprovedButton.setEnabled(true);
                                    DriverApprovedButton.setText("Отменить поиск заказов");

                                    requestType = true;
                                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                                    geoFire.setLocation(driverID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                        @Override
                                        public void onComplete(String key, DatabaseError error) {
                                            zoomToUserLocation();
                                            getAssignedCustomerRequest();
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });

        //Слушатель кнопки на отмену заказа
        rejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DriverApprovedButton.getText() == "Отменить заказ") { //Диалоговое окно "Отменить заказ?", в котором пользователь выбирает "нет"
                    dialog.dismiss();
                } else { //Отменить заказ при нахождении заказчика
                    dialog.dismiss();
                    final DatabaseReference CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().
                            child("Customers Requests").child(customerID);
                    CustomerDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                DatabaseReference reference = CustomerDatabaseRef.child("CustomersBanList").child(driverID);
                                reference.setValue(true);
                                DatabaseReference DriverRemoveRideId = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("CustomerRideID").child(customerID);
                                DriverRemoveRideId.removeValue(new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                        Toast.makeText(DriversMapActivity.this, "Заказ отклонен", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                DatabaseReference DriverRemoveRideId = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("CustomerRideID").child(customerID);
                                DriverRemoveRideId.removeValue(new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                        Toast.makeText(DriversMapActivity.this, "Заказ отклонен", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                sleep(3000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                customerID = "";
                                getAssignedCustomerRequest();
                            }
                        }
                    };
                    thread.start();

                }
            }
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        //Слушатель кнопки для перехода в настройки
        settingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!requestType && customerID == "") {
                    Intent intent = new Intent(DriversMapActivity.this, DriverSettingsActivity.class);
                    intent.putExtra("type", "Drivers");
                    startActivity(intent);
                } else {
                    Toast.makeText(DriversMapActivity.this, "Нельзя менять или просматривать настройки во время заказа!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Слушательн кнопки для начала и отмены поиска заказчиков
        DriverApprovedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType && customerID == "") { //Отменили поиск заказов
                    DisconnectDriver();
                    requestType = false;
                    DriverApprovedButton.setText("Начать поиск заказов");
                    zoomToUserLocation();
                } else if(customerID == "" && !requestType){ //Начали поиск заказов
                    zoomToUserLocation(); //Переводим камеру на таксиста
                    requestType = true;
                    DriverApprovedButton.setText("Отменить поиск заказов");
                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                    geoFire.setLocation(driverID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    getAssignedCustomerRequest();
                } else if(customerID != "" && !requestType && orderID != ""){ //Отменили заказ после нахождения заказчика
                    textOrderFound.setText("Отменить заказ?");
                    linearLayout.setVisibility(View.GONE);
                    acceptBtn.setText("Да");
                    rejectBtn.setText("Нет");
                    dialog.show();
                }

                //Тут дописать метод для отмены заказа уже в процессе поездки, но до принятия пассажира на борт
            }
        });

        //Слушатель на кнопку "Клиент в машине"
        DriverInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DriverInfoBtn.getText().toString() == "Клиент в машине") {
                    if(PickUpMarker != null) PickUpMarker.remove(); //Как прибыли на пункт нахождения заказчика удаляем данный пункт на карте
                    DriverInfoBtn.setText("Поездка завершена");
                    DriverInfoBtn.setEnabled(false);
                    DriverApprovedButton.setVisibility(View.GONE);
                    DriverApprovedButton.setEnabled(false);
                    DriverInfoBtn.setVisibility(View.GONE);
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                            .child("Orders")
                            .child(orderID)
                            .child("status");
                    databaseReference.setValue("Выполняется", new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            txtAdress.setText(pointBString);
                            startRoadToLastPoint();
                        }
                    });
                } else
                    if(DriverInfoBtn.getText().toString() == "Поездка завершена"){
                        linearLayout.setVisibility(View.GONE);
                        acceptBtn.setText("Оплачено");
                        rejectBtn.setText("Оставить отзыв");
                        rejectBtn.setEnabled(false);
                        textOrderFound.setText("Поездка окончена!");
                        finishPrice.setText("Сумма за поездку: " + price + "Р");
                        finishPrice.setVisibility(View.VISIBLE);
                        dialog.show();
                        //Для вывода диалога на конечный диалог
                    }
            }
        });

        //Слушатель кнопки для выхода из аккаунта
        logoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!requestType && customerID == "") {
                    currentLogoutDriverStatus = true;
                    mAuth.signOut();//Выход из аутентификации
                    LogoutDriver();//Переход обратно на экран выбора пользователя
                }else {
                    Toast.makeText(DriversMapActivity.this, "У вас запущен поиск заказов!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        geocoder = new Geocoder(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500); //Интервал обновления геолокации
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("TAG", "askLocationPermission: you should show an alert dialog...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //enableUserLocation();
                    checkSettingsAndStartLocationUpdated();
                    buildGoogleApiClient();
                }
                break;
        }
    }

    //Отрисовка карты
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("status", "Запустился onMapReady()");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.129057, 40.406635), 12.0f));
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //enableUserLocation();
            zoomToUserLocation();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCES_LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCES_LOCATION_REQUEST_CODE);
            }
        }
    }


    private void zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            }
        });
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    private void checkSettingsAndStartLocationUpdated() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("TAG", "Не удается начать обновление локации");
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(DriversMapActivity.this, 1001);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }
                }
            }
        });

    }

    private void setUserLocationMarker(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userLocationMarker == null){
            //Создаем новый маркер
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car));
            markerOptions.rotation(location.getBearing());
            markerOptions.anchor((float) 0.5, (float) 0.5);
            userLocationMarker = mMap.addMarker(markerOptions);
        } else {
            //Используем предыдущий маркер
            userLocationMarker.setPosition(latLng);
            userLocationMarker.setRotation(location.getBearing());
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdate() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(final Location location) {

    }


    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!currentLogoutDriverStatus) {
            DisconnectDriver();
        }
        Log.d("status", "Запустился onStop()");
        //checkStatus(); //Тут надо будет дописать проверку активного заказа, чтобы при перезаходе он работал правильно

        if(customerID == ""){
            stopLocationUpdate();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("TAG", "Запустился onStart()");
        //checkStatus(); //Тут надо будет дописать проверку активного заказа, чтобы при перезаходе он работал правильно
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            checkSettingsAndStartLocationUpdated();
        else askLocationPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("status", "Запустился onDestroy()");

        DisconnectDriver();
        stopLocationUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("status", "Запустился onResume()");
        //checkStatus(); //Тут надо будет дописать проверку активного заказа, чтобы при перезаходе он работал правильно
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            checkSettingsAndStartLocationUpdated();
        else askLocationPermission();
    }

    //Метод для удаления Driver Available.
    private void DisconnectDriver() {
        if(customerID == "" && requestType) {
            DriverApprovedButton.setText("Готов принимать заказы");
            String userID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

            GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
            geoFire.removeLocation(userID);
            requestType = false;
        } else if(customerID != "" && !requestType && orderID != ""){
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("Orders")
                    .child(orderID);
            databaseReference.removeValue();
        }
    }

    private void LogoutDriver() {
        DisconnectDriver();
        stopLocationUpdate();
        Intent welcomeIntent = new Intent(DriversMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }


    private void getAssignedCustomerRequest(){
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomerRefListener = assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0 && customerID == "") {
                    assignedCustomerRef.removeEventListener(assignedCustomerRefListener);
                    Object value = snapshot.getValue();
                    if(value instanceof Map){
                        Map<String, Boolean> customerIDs = (Map<String, Boolean>) value;
                        String[] myArray = {};
                        myArray = customerIDs.keySet().toArray(new String[customerIDs.size()]);
                        customerID = myArray[0];
                        DialogShow();
                    }
                } else {
                    it = 1;
                    customerID = "";

                    if (PickUpMarker != null & RoadMarker != null) {
                        PickUpMarker.remove();
                        RoadMarker.remove();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void DialogShow() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Customers Requests")
                .child(customerID);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 1 && customerID != "") {
                    pointAString = snapshot.child("adressA").getValue().toString();
                    pointBString = snapshot.child("adressB").getValue().toString();
                    pointA.setText(pointAString);
                    pointB.setText(pointBString);
                    textOrderFound.setText("Новый заказ поблизости!");
                    linearLayout.setVisibility(View.VISIBLE);
                    acceptBtn.setText("Принять");
                    rejectBtn.setText("Отклонить");
                    dialog.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void getAssignedCustomerPosition() {
        AssignedCustomerPosition = FirebaseDatabase.getInstance().getReference().child("Orders").child(orderID);

        AssignedCustomerPosition.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("CustomerLocation").exists()) {
                    //Место самовывоза
                    List<Object> customerPosition = (List<Object>) snapshot.child("CustomerLocation").child("l").getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if (customerPosition.get(0) != null) { LocationLat = Double.parseDouble(customerPosition.get(0).toString()); }
                    if (customerPosition.get(1) != null) { LocationLng = Double.parseDouble(customerPosition.get(1).toString()); }

                    LatLng CustomerLatLng = new LatLng(LocationLat, LocationLng);


                    //Пункт доставки
                    List<Object> lastPosition = (List<Object>) snapshot.child("LastPoint").child("l").getValue();
                    double LocationLat1 = 0;
                    double LocationLng1 = 0;

                    if (lastPosition.get(0) != null) { LocationLat1 = Double.parseDouble(lastPosition.get(0).toString()); }
                    if (lastPosition.get(1) != null) { LocationLng1 = Double.parseDouble(lastPosition.get(1).toString()); }

                    LatLng LastLatLng = new LatLng(LocationLat1, LocationLng1);

                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerLatLng).title("Забрать заказчика тут")); //Создаем маркер пункта места нахождения заказчика
                    RoadMarker = mMap.addMarker(new MarkerOptions().position(LastLatLng).title("Место назначения")); //Создаем маркер места назначения заказчика

                    price = snapshot.child("startPrice").getValue().toString();

                    startListenersDistance();
                } else getAssignedCustomerPosition();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void startListenersDistance() {
        DistanceCheckStatus = true;
    }

    private void startRoadToLastPoint() {
        RoadToLastPointStatus = true;
    }

    private void CustomerApprove(){
        showLinInfo();
    }

    private void showLinInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Customers")
                .child(customerID);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DriverApprovedButton.setText("Отменить заказ");
                    LinInfo.setVisibility(View.VISIBLE);
                    LinInfo.setEnabled(true);
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    txtAdress.setText(pointAString);
                    txtName.setText(name);
                    txtPhone.setText(phone);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.129057, 40.406635), 12.0f));
                    getAssignedCustomerPosition();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createOrder() {
        orderID = driverID.toLowerCase() + String.valueOf(1000 + (int) (Math.random() * 2000));

        DatabaseReference orderToCustomer = FirebaseDatabase.getInstance().getReference()
                .child("Customers Requests")
                .child(customerID)
                .child("orderID");
        orderToCustomer.setValue(orderID, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                requestType = false;
                DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
                        .child("Orders")
                        .child(orderID);
                GeoFire geoFire = new GeoFire(orderRef);
                geoFire.setLocation("DriverLocation", new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("startTimeTrip", date);
                userMap.put("tokenDriver", driverID);

                orderRef.updateChildren(userMap);

                CustomerApprove();
            }
        });
    }
}