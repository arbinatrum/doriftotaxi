package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;

    Button customerLogoutButton, settingsButton;
    Button callTaxiButton;
    String customerID;
    LatLng CustomerPosition;
    int radius = 1;
    Boolean driverFound = false, requestType, status = true;
    String driverFoundID;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    DatabaseReference CustomerDatabaseReference;
    DatabaseReference DriversAvailableRef;
    DatabaseReference DriversRef;
    DatabaseReference DriversLocationRef;

    private ValueEventListener DriverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);


        customerLogoutButton = (Button) findViewById(R.id.customer_logout_button);
        settingsButton = (Button) findViewById(R.id.customer_settings_button);
        callTaxiButton = (Button) findViewById(R.id.customer_order_button);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Customers Requests"); //Обращение напрямую к конкретной полочке в Базе данных
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //Создаем переменную для получения Локации всех водителей
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); //Водитель принял заказ и он уже в работе



        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Здесь необходимо боавить новый метод на запрос включения GPS

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 mAuth.signOut();

                 LogoutCustomer();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapActivity.this, DriverSettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Здесь также необходимо боавить новый метод на запрос включения GPS, если все таки пользователь включил GPS и предоставил доступы к приложению, то выполняем код ниже
                if(requestType){
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriversLocationRef.removeEventListener(DriverLocationRefListener);
                    if(driverFound != null){
                        DriversRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                        DriversRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
                    geoFire.removeLocation(customerID);

                    if(PickUpMarker != null){
                        PickUpMarker.remove();
                    }

                    if(driverMarker != null){
                        driverMarker.remove();
                    }

                    callTaxiButton.setText("Вызвать такси");
                }
                else {

                    GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    //Установка геопозиции заказчика
                    CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я нахожусь здесь"));

                    showLocation(lastLocation, 5);

                    callTaxiButton.setText("Поиск такси...");
                    getNearbyDrivers();
                }
            }
        });
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

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); //Интервал обновления геолокации
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
    public void onLocationChanged(Location location) {
        lastLocation = location;


        if(status){
            showLocation(lastLocation, 12);
        }
        status = false;
    }

    //Метод обновления камеры вынесен отдельно
    private void showLocation(Location location, int zoomSide) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomSide));
    }

    private void showLocation(Marker Marker, int zoomSide) {
        LatLng latLng = new LatLng(Marker.getPosition().latitude, Marker.getPosition().longitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomSide));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //Выходим на начальный экран и выполняем логаут для Заказчика
    private void LogoutCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    //Метод для поиска водителей поблизости
    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(DriversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude), radius);
        geoQuery.removeAllListeners();


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType){
                    driverFound = true;
                    driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);
                    //Привязка водителя к заказчику после нахождения

                    GetDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //Если водитель не найден, то увеличиваем радиус
                if(!driverFound){
                    radius = radius + 1;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GetDriverLocation() {
        //Получить данные о геолокаци водителя
        DriverLocationRefListener = DriversLocationRef.child(driverFoundID).child("l").
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && requestType){
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            callTaxiButton.setText("Водитель найден!");
                            //Тут необходимо добавить диалоговое окно с информацие о водителе.

                            if (driverLocationMap.get(0) != null){
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());

                            }
                            if (driverLocationMap.get(1) != null){
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                            if(driverMarker != null) { //Удалим лишних водителей
                                driverMarker.remove();

                            }

                            //Определим расстояние между таксистом и заказчиком
                            Location location1 = new Location(""); //Позиция водителя
                            location1.setLatitude(DriverLatLng.latitude);
                            location1.setLongitude(DriverLatLng.longitude);

                            Location location2 = new Location(""); //Позиция заказчика
                            location2.setLatitude(CustomerPosition.latitude);
                            location2.setLongitude(CustomerPosition.longitude);

                            float Distance = location1.distanceTo(location2); //Определение расстояния по гугл локации

                            if (Distance < 100){
                                callTaxiButton.setText("Ваше такси подъезжает");
                            }
                            else {
                                callTaxiButton.setText("Такси в пути");
                            }


                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                    .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                            showLocation(driverMarker, 12);

                         }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}