package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;

    //Кнопки интерфейса
    Button customerLogoutButton, settingsButton;
    String customerID;
    LatLng CustomerPosition;
    int radius = 1;
    Boolean driverFound = false, requestType = false, status = true;
    String driverFoundID;
    /*private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout bottomSheet;*/

    private boolean close = true;

    //Нижний лист
    //Button callTaxiButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        String getType = getIntent().getStringExtra("type");

        if (getType != null) {
            if (getType.equals("Ready!")) {
                Toast.makeText(this, "Данные успешно сохранены!", Toast.LENGTH_SHORT).show();
            }
        }

        //Кнопки интерфейса
        customerLogoutButton = (Button) findViewById(R.id.customer_logout_button);
        settingsButton = (Button) findViewById(R.id.customer_settings_button);
        //callTaxiButton = (Button) findViewById(R.id.customer_order_button);

        //Нижний лист
        //bottomSheet = (LinearLayout) findViewById(R.id.bottomSheetContainer);
        //bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

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

        RelInfo.setVisibility(View.INVISIBLE);
        RelInfo.setEnabled(false);


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

        //Здесь также необходимо боавить новый метод на запрос включения GPS, если все таки пользователь включил GPS и предоставил доступы к приложению, то выполняем код ниже

                /*
                Если есть запрос на поиск водителей, но заказчик решил отменить поиск,
                то удаляем всех видимых водителей, если они есть.
                Удаляем из БД привязку к водителю, если она не пустая.
                Отменяем отправку данных о пользователе в БД.
                Удаляем все маркеры с карты и меняем название кнопки на Вызвать такси.
                */

        /*callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!requestType){
                    //Добавить код на определение количества дочерних элементов у заказчика: if countchildren > 1 {выполняем код ниже} else {Выдаем Toast на необходимость бобавления данных о себе}

                    checkBottomSheet();

                    infoButton.setText("Отменить поиск такси");
                    requestType = true;

                    //Устанавливаем новую полку в БД RequestType и записываем туда месторасположение заказчика и его ID
                    GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    //Установка геопозиции заказчика на карте
                    CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я нахожусь здесь"));

                    showLocation(lastLocation, 10); //Зум камеры при поиске

                    getNearbyDrivers(); //Начинаем поиск водителей
                }
            }
        });*/

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestType) {
                    //heckBottomSheet();
                    requestType = false;
                    if (geoQuery != null) { //Обнуляем позицию водителя
                        geoQuery.removeAllListeners();
                        DriversLocationRef.removeEventListener(DriverLocationRefListener); //Отменяем обновление данных о геолокации водителя
                    }
                    if (driverFound) {
                        DriversRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                        DriversRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
                    geoFire.removeLocation(customerID);

                    if (PickUpMarker != null) {
                        PickUpMarker.remove();
                    }

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    infoButton.setText("Начать поиск такси");
                } else {
                        //Добавить код на определение количества дочерних элементов у заказчика: if countchildren > 1 {выполняем код ниже} else {Выдаем Toast на необходимость бобавления данных о себе}
                        requestType = true;

                        //Устанавливаем новую полку в БД RequestType и записываем туда месторасположение заказчика и его ID
                        GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
                        geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                        //Установка геопозиции заказчика на карте
                        CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                        PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я нахожусь здесь"));

                        showLocation(lastLocation, 10); //Зум камеры при поиске
                        infoButton.setText("Отменить поиск такси");
                        getNearbyDrivers(); //Начинаем поиск водителей
                } //else Toast.makeText(CustomersMapActivity.this, "Нет доступных таксистов!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Отрисовка карты
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
        } //Если предоставлены доступы, то даем добро на получение локации
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
            showLocation(lastLocation, 18);
        }
        status = false;
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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(requestType) {
            GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
            geoFire.removeLocation(customerID);
            requestType = false;
        }
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

    private  boolean checkAvailableDrivers(){
        final boolean[] make = new boolean[1];
        DriversAvailableRefListener = DriversAvailableRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                make[0] = snapshot.exists();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        DriversAvailableRef.removeEventListener(DriversAvailableRefListener);

        return make[0];
    }

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
}