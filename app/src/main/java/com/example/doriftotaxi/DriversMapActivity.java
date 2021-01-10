package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private String driverID, customerID = "";
    private Button DriverApprovedButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference DriverDatabaseRef;
    private LatLng DriverPosition;

    private Boolean currentLogoutDriverStatus;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPosition;

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        settingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriversMapActivity.this, DriverSettingsActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });

        DriverApprovedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocationUser();
                DriverApprovedButton.setVisibility(View.INVISIBLE);
                DriverApprovedButton.setEnabled(false);
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        });

        logoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutDriverStatus = true;
                mAuth.signOut();//Выход из аутентификации

                LogoutDriver();//Переход обратно на экран выбора пользователя
                if(!DriverApprovedButton.isEnabled()) DisconnectDriver();//Передача данных в firebase
            }
        });


        getAssignedCustomerRequest();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); //Интервал обновления геолокации, 1 секунда
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } //Если выданы пермишны на геолокацию, то получаем данные о своем месторасположении
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(final Location location) {
        if (getApplicationContext() != null) {
            lastLocation = location;

            //Мое месторасположение
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            if (!DriverApprovedButton.isEnabled()) updateLocationUser();
        }
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
    }

    private void DisconnectDriver() {
        String userID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");

        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
    }

    private void LogoutDriver() {

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

    private void getAssignedCustomerRequest() {
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        //Если данные совпали
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    customerID = snapshot.getValue().toString();

                    getAssignedCustomerPosition();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerPosition() {
        AssignedCustomerPosition = FirebaseDatabase.getInstance().getReference().child("Customer Requests")
                .child(customerID).child("l");

        AssignedCustomerPosition.addValueEventListener(new ValueEventListener() {
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

                    mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Забрать заказчика тут"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}