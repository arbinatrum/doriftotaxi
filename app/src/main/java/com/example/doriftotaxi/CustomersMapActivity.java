package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;
    GeoFire geoFire1;

    //Кнопки интерфейса
    Button customerLogoutButton, settingsButton;

    //Кнопки и элементы Нижнего Листа
    private BottomSheetBehavior<CoordinatorLayout> bottomSheetBehavior; //сам нижний лист
    private Button callTaxiButton;

    //Токены и буллеаны
    String customerID;
    LatLng CustomerPosition;
    int radius = 1;
    Boolean driverFound = false, requestType = false, status = true;
    Boolean StateCustomer = true;
    String driverFoundID;

    //Информация о такси
    private TextView txtName, txtPhone, txtCar;
    private CircleImageView DriverImageView;
    private Button infoButton;
    private RelativeLayout RelInfo;

    //Для обращения к БД
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference CustomerDatabaseReference;
    private DatabaseReference DriversAvailableRef;
    private DatabaseReference DriversRef;
    private DatabaseReference DriversLocationRef;
    //

    private ValueEventListener DriverLocationRefListener;
    private ValueEventListener DriversAvailableRefListener;
    private ValueEventListener CustomerLocationRefListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        /*String getType = getIntent().getStringExtra("type");

        if (getType != null) {
            if (getType.equals("Ready!")) {
                Toast.makeText(this, "Данные успешно сохранены!", Toast.LENGTH_SHORT).show();
            }
        }*/

        //Элементы нижнего листа
        callTaxiButton = findViewById(R.id.customer_order_button);
        CoordinatorLayout container = findViewById(R.id.bottomSheetContainer);

        bottomSheetBehavior = BottomSheetBehavior.from(container);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        //Кнопки интерфейса
        customerLogoutButton = (Button) findViewById(R.id.customer_logout_button);
        settingsButton = (Button) findViewById(R.id.customer_settings_button);


        //Информация о такси
        txtName = (TextView) findViewById(R.id.driver_name);
        txtPhone = (TextView) findViewById(R.id.driver_phone);
        txtCar = (TextView) findViewById(R.id.driver_car);
        RelInfo = findViewById(R.id.rel_info);
        DriverImageView = (CircleImageView) findViewById(R.id.driver_photo);
        infoButton = findViewById(R.id.infoBtn);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Customers Requests"); //Обращаемся к запросам на заказ
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //Обращаемся к доступным водителям
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); //Обращаемся к водителям в работе

        //Информационный блок о таксисте
        RelInfo.setVisibility(View.INVISIBLE);
        RelInfo.setEnabled(false);
        infoButton.setVisibility(View.INVISIBLE);
        infoButton.setEnabled(false);

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType) {
                    Toast.makeText(CustomersMapActivity.this,"Нельзя выходить во время поиска", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.signOut();
                    LogoutCustomer();
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType) {
                    Toast.makeText(CustomersMapActivity.this,"Нельзя заходить в настройки во время поиска", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(CustomersMapActivity.this, DriverSettingsActivity.class);
                    intent.putExtra("type", "Customers");
                    startActivity(intent);
                }
            }
        });

        //Здесь также необходимо боавить новый метод на запрос включения GPS, если все таки пользователь включил GPS и предоставил доступы к приложению, то выполняем код ниже

                /*
                Если есть запрос на поиск водителей, но заказчик решил отменить поиск,
                то удаляем всех видимых водителей, если они есть.
                Удаляем из БД привязку к водителю, если она не пустая.
                Отменяем отправку данных о пользователе в БД.
                Удаляем все маркеры с карты и меняем название кнопки на Вызвать такси.
                */

        //bottomSheetClose();
        getStateCustomer();
        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(StateCustomer) {
                    if (requestType) {
                        Log.d("status", "Запускаем отмену по кнопке");
                        requestType = false;
                        if (geoQuery != null) { //Обнуляем позицию водителя
                            geoQuery.removeAllListeners();
                            DriversLocationRef.removeEventListener(DriverLocationRefListener); //Отменяем обновление данных о геолокации водителя
                            Log.d("status", "очищаем geoQuery");
                        }
                        if (driverFound) {
                            DriversRef = FirebaseDatabase.getInstance().getReference()
                                    .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                            DriversRef.removeValue();
                            driverFoundID = null;
                            Log.d("status", "очищаем driverFound");
                        }
                        driverFound = false;
                        radius = 1;

                        if (PickUpMarker != null) {
                            PickUpMarker.remove();
                            Log.d("status", "очищаем PickUpMarker в callTaxi");
                        }

                        if (driverMarker != null) {
                            driverMarker.remove();
                            Log.d("status", "очищаем driverMarker");
                        }

                        //CustomerDatabaseReference.child(customerID).removeValue();
                        if (lastLocation != null) {
                            geoFire1.removeLocation(customerID, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    Log.d("status", "Удаляем локацию в firebase, CustomerDatabaseReference.child(customerID).removeValue()");
                                }
                            });
                        }

                        callTaxiButton.setText("Начать поиск");
                        Log.d("status", "меняем название кнопки на Начать поиск");

                    } else {
                        Log.d("status", "Запускаем поиск по кнопке");
                        //Добавить код на определение количества дочерних элементов у заказчика: if countchildren > 1 {выполняем код ниже} else {Выдаем Toast на необходимость бобавления данных о себе}
                        //else Toast.makeText(CustomersMapActivity.this, "Нет информации о вас", Toast.LENGTH_SHORT).show();
                        requestType = true;
                        callTaxiButton.setText("Отменить поиск такси");
                        if (lastLocation != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 18.0f));
                        }
                        displayLocation();
                        //getNearbyDrivers(); //Начинаем поиск водителей
                    }
                } else Toast.makeText(CustomersMapActivity.this,"Необходимо заполнить данные о себе в настройках",Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setUpLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void setUpLocation(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        } else {
            if(checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(CustomerLocationRefListener != null){
            CustomerDatabaseReference.removeEventListener(CustomerLocationRefListener);
        }
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if(lastLocation != null){
            Log.d("Status", String.format("Моя локация тут: %f / %f", lastLocation.getLatitude(),lastLocation.getLongitude()));
            if(status){
                status = false;
                showLocation(lastLocation, 18);
            }
            //Тут можно сделать подачу данных о пользователе в firebase, можно сделать через флаг
            //Необходимо создать новый geoFire экземпляр, он будет для заказчика и будет обновляться в этом методе

            CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

            geoFire1 = new GeoFire(CustomerDatabaseReference);
            if(requestType){
                //allTaxiButton.setText("Отменить поиск такси");
                geoFire1.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                            //Установка геопозиции заказчика на карте
                            if (PickUpMarker != null) PickUpMarker.remove();
                            Log.d("status", "Отдаем локацию в firebase, setLocation");

                            //Анимация Камеры
                            PickUpMarker = mMap.addMarker(new MarkerOptions()
                                    .position(CustomerPosition)
                                    .title("Я нахожусь здесь"));
                            //showLocation(lastLocation, 10); //Зум камеры при поиске
                            Log.d("status", "Поставили маркер PickUpMarker в displayLocation");
                        }
                    }
                });
            }
        } else {
            Log.d("Status","Нет вашей локации");
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); //Интервал обновления геолокации
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
            else {
                Toast.makeText(this,"Данный девайс не поддерживается",Toast.LENGTH_SHORT).show();
                finish();
            } return false;
        }return true;
    }

    //Отрисовка карты
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("status","Запустился onMapReady()");
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(55.755,37.61), 12.0f));

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
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("status","Запустился onConnectionFailed()");
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        displayLocation();
        //Тут можно добавить метод вывода всех таксистов на карте
    }

    //Метод обновления камеры вынесен отдельно
    private void showLocation(Location location, int zoomSide) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomSide));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("status","Запустился onStop()");
        checkStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("status","Запустился onDestroy()");

        if(requestType) {
            GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
            geoFire.removeLocation(customerID);
            requestType = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("status","Запустился onResume()");
        checkStatus();
    }

    private void checkStatus() {
        CustomerLocationRefListener = CustomerDatabaseReference.child(customerID).child("l")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    requestType = true;
                    List<Object> customerLocationMap = (List<Object>) snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if (customerLocationMap.get(0) != null){
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                        Log.d("status", String.format("LocationLat = %f", LocationLat));
                    }
                    if (customerLocationMap.get(1) != null){
                        LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                        Log.d("status", String.format("LocationLat = %f", LocationLng));
                    }

                    if(PickUpMarker != null) { //Удалим маркер если он есть
                        PickUpMarker.remove();
                    }
                    Location lastLocation = new Location("");
                    lastLocation.setLatitude(LocationLat);
                    lastLocation.setLongitude(LocationLng);

                    CustomerPosition = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());

                    Log.d("status", "Отдаем локацию в firebase, setLocation");

                    //Анимация Камеры
                    PickUpMarker = mMap.addMarker(new MarkerOptions()
                            .position(CustomerPosition)
                            .title("Я нахожусь здесь"));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()),18.0f));

                    callTaxiButton.setText("Отменить поиск такси");

                    displayLocation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.d("status","Запустился onRestart()");
        displayLocation();
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
        //Начинаем поиск с позиции заказчика и ставим радиус, в последствии обновлеяем его
        geoQuery.removeAllListeners();

        //Здесь нужно добавить код определяющий принял ли таксист у себя заказ или нет.

        /*Если таксист у себя принял данный заказ, то выполняем следующиее:
        Если водитель не занят и статус запроса True, то берем id заказчика и в Drivers/driverFoundID/CustomerRideID
        создаем новый элемент с customerID.*/
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
                    //showLocation(driverMarker, 15); //Показываем заказчику расположение водителя
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

        //Если таксист не принял заказ, то ничего не делаем ни у заказчика ни у таксиста. см. логику на DriversMapActivity
    }

    private void GetDriverLocation() {
        //Водитель найден. Обновляем данные геолокаци таксиста
        DriverLocationRefListener = DriversLocationRef.child(driverFoundID).child("l").
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && requestType){
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            Toast.makeText(CustomersMapActivity.this, "Водитель найден!", Toast.LENGTH_SHORT).show();
                            //bottomSheetClose();
                            RelInfo.setVisibility(View.VISIBLE);
                            RelInfo.setEnabled(true);
                            infoButton.setVisibility(View.VISIBLE);
                            infoButton.setEnabled(true);
                            showLinearInfo();

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

                            if (Distance < 50){
                                infoButton.setText("Ваше такси подъезжает");
                            }
                            else {
                                infoButton.setText("Такси в пути");
                            }

                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                    .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                         }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    /*private void checkBottomSheet(){
        if(close){
            close = false;
            infoButton.setVisibility(View.INVISIBLE);
            infoButton.setEnabled(false);
            bottomSheetOpen();
        } else {
            bottomSheetClose();
            close = true;
            infoButton.setVisibility(View.VISIBLE);
            infoButton.setEnabled(true);
        }
    }

    private void bottomSheetClose() {
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED | bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED){
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void bottomSheetOpen(){
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetBehavior.setHideable(false);
        }
    }*/

    private void showLinearInfo(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 1){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String carmodel = snapshot.child("carmodel").getValue().toString();
                    String carnumber = snapshot.child("carnumber").getValue().toString();
                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCar.setText(carmodel + "," + carnumber);

                    if(snapshot.hasChild("image")) {
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(DriverImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getStateCustomer(){
        DatabaseReference CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        CustomerDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 1){
                    StateCustomer = true;
                } else StateCustomer = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}