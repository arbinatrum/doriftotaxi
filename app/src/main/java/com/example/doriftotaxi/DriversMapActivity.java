package com.example.doriftotaxi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import java.util.List;
import java.util.Objects;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                Log.d("TAG", "onLocationResult равен нулю ");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                //displayLocation();
                Log.d("TAG", "onLocationResult: " + location.toString());
                lastLocation = location;
                if(lastLocation != null && requestType == true && customerID == ""){
                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                    geoFire.setLocation(driverID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                }
            }
        }
    };

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation, FirstLocation;
    LocationManager locationManager;
    Marker PickUpMarker;

    private String driverID, customerID = "";
    private Button DriverApprovedButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference DriverDatabaseRef;
    private LatLng DriverPosition;
    private boolean status = true, requestType = false;

    private Boolean currentLogoutDriverStatus = false;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPosition;

    private ValueEventListener AssignedCustomerPositionListener;
    private ValueEventListener DriverLocationRefListener;

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


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        settingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!requestType) {
                    Intent intent = new Intent(DriversMapActivity.this, DriverSettingsActivity.class);
                    intent.putExtra("type", "Drivers");
                    startActivity(intent);
                } else {
                    Toast.makeText(DriversMapActivity.this, "Нельзя менять или просматривать настройки во время заказа!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        DriverApprovedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType && customerID == "") {
                    DisconnectDriver();
                    requestType = false;
                    DriverApprovedButton.setText("Начать поиск заказов");
                    zoomToUserLocation();
                } else if(customerID == "" && !requestType){
                    zoomToUserLocation(); //Переводим камеру на таксиста
                    requestType = true;
                    DriverApprovedButton.setText("Отменить поиск заказов");
                    GeoFire geoFire = new GeoFire(DriverDatabaseRef);
                    geoFire.setLocation(driverID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    getAssignedCustomerRequest();
                }
            }
        });

        logoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!requestType) {
                    currentLogoutDriverStatus = true;
                    mAuth.signOut();//Выход из аутентификации
                    LogoutDriver();//Переход обратно на экран выбора пользователя
                }else {
                    Toast.makeText(DriversMapActivity.this, "У вас запущен поиск заказов!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000); //Интервал обновления геолокации
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10);
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
                    enableUserLocation();
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
            zoomToUserLocation();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCES_LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCES_LOCATION_REQUEST_CODE);
            }
        }
    }

    private void enableUserLocation() {
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
        mMap.setMyLocationEnabled(true);
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
        if(status && lastLocation!= null){
            zoomToUserLocation();
            status = false;
        }
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
        stopLocationUpdate();
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
        }
    }

    private void LogoutDriver() {
        String userID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
        Intent welcomeIntent = new Intent(DriversMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    private void updateLocationUser() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference DriverAvailablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
        GeoFire geoFireAvailablity = new GeoFire(DriverAvailablityRef);
        //Водитель готов к заказу


        DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");
        GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);
        //Водитель принял заказ

        switch (customerID){
            case "":
                geoFireWorking.removeLocation(userID);
                geoFireAvailablity.setLocation(userID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                break;
            default:
                geoFireAvailablity.removeLocation(userID);
                geoFireWorking.setLocation(userID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                break;
        }
    }

    private void getAssignedCustomerRequest(){
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /*private void getAssignedCustomerRequest() {
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        //Если данные совпали
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Если заказчик привязан к водителю, то Получаем данные о месторасположении заказчика
                if(snapshot.exists()){
                    customerID = snapshot.getValue().toString();

                    //getAssignedCustomerPosition();
                }
                else {
                    //Если заказчик не привязан, то удаляем слушателей позиии заказчика, очищаем маркер позиции заказчика и говорим, что id заказчика нет
                    customerID = "";

                    if(PickUpMarker != null){
                        PickUpMarker.remove();
                    }

                    if(AssignedCustomerPositionListener != null){
                        AssignedCustomerPosition.removeEventListener(AssignedCustomerPositionListener);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }*/

    private void getAssignedCustomerPosition() {
        AssignedCustomerPosition = FirebaseDatabase.getInstance().getReference().child("Customer Requests")
                .child(customerID).child("l");

        AssignedCustomerPositionListener = AssignedCustomerPosition.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Object> customerPosition = (List<Object>) snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;
                    //Тут необходимо добавить диалоговое окно с информацие о заказчике.

                    if (customerPosition.get(0) != null) {
                        LocationLat = Double.parseDouble(customerPosition.get(0).toString());
                    }
                    if (customerPosition.get(1) != null) {
                        LocationLng = Double.parseDouble(customerPosition.get(1).toString());
                    }

                    LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Забрать заказчика тут"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}