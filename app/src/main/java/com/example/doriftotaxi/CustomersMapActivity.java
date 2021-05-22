package com.example.doriftotaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.util.Strings;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193;

    //Для диалогового окна
    private Dialog dialog;
    private TextView driverName, driverPhone, driverCar;
    private CircleImageView driverImage;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;
    GeoFire geoFire1;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
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
            }
            if(status && lastLocation!= null){
                zoomToUserLocation();
                status = false;
            }
        }
    };


    //Кнопки интерфейса
    Button customerLogoutButton, settingsButton;

    //Кнопки и элементы Нижнего Листа
    private BottomSheetBehavior<CoordinatorLayout> bottomSheetBehavior; //сам нижний лист
    private Button callTaxiButton;
    private EditText searchBarA, searchBarB;
    private int beforeBottomSheetState;
    String searchA;
    String searchB;

    //Токены и буллеаны
    String customerID;
    LatLng CustomerPosition;
    int radius = 1;
    double section = 0;
    HashMap<String, String> driversMap = new HashMap<String, String>();
    int it = 1;
    Boolean driverFound = false, requestType = false, status = true;
    Boolean StateCustomer = true;
    String driverFoundID = "";

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
    private ValueEventListener BanListListener;
    private ChildEventListener CustomerRideListener;
    private DatabaseReference DriversRef1;
    private String orderID = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Инфо о такси в диалоговое окно
        driverName = (TextView) findViewById(R.id.driver_name_dialog);
        driverPhone = (TextView) findViewById(R.id.driver_phone_dialog);
        driverCar = (TextView) findViewById(R.id.driver_car_dialog);
        driverImage = (CircleImageView) findViewById(R.id.driver_photo_dialog);

        dialog = new Dialog(CustomersMapActivity.this);
        dialog.setContentView(R.layout.dialog_fragment);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

        Button okay = dialog.findViewById(R.id.customer_approve_button);

        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CustomersMapActivity.this, "Okay", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                bottomSheetBehavior.setDraggable(true);
                bottomSheetBehavior.setHideable(true);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                RelInfo.setVisibility(View.VISIBLE);
                RelInfo.setEnabled(true);
                infoButton.setVisibility(View.VISIBLE);
                infoButton.setEnabled(true);
                showLinearInfo();
            }
        });
        //Элементы нижнего листа
        callTaxiButton = findViewById(R.id.customer_order_button);
        searchBarA = findViewById(R.id.search_bar_A);
        searchBarB = findViewById(R.id.search_bar_B);
        CoordinatorLayout container = findViewById(R.id.bottomSheetContainer);

        bottomSheetBehavior = BottomSheetBehavior.from(container);
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.42f);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //Log.d("status Behavior",String.format("state is %d",bottomSheetBehavior.getState()));
                beforeBottomSheetState = bottomSheetBehavior.getState();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //Log.d("Status",String.format("Положения - %f",slideOffset));
                if (slideOffset > 0.251 && !searchBarA.hasFocus() && !searchBarB.hasFocus()) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
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
        customerID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        CustomerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Customers Requests"); //Обращаемся к запросам на заказ
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //Обращаемся к доступным водителям
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(orderID).child("DriverLocation").child("l"); //Обращаемся к месторасположению таксиста из заказа

        //Информационный блок о таксисте
        RelInfo.setVisibility(View.INVISIBLE);
        RelInfo.setEnabled(false);
        infoButton.setVisibility(View.INVISIBLE);
        infoButton.setEnabled(false);

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestType) {
                    Toast.makeText(CustomersMapActivity.this, "Нельзя выходить во время поиска", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.signOut();
                    LogoutCustomer();
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestType) {
                    Toast.makeText(CustomersMapActivity.this, "Нельзя заходить в настройки во время поиска", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(CustomersMapActivity.this, DriverSettingsActivity.class);
                    intent.putExtra("type", "Customers");
                    startActivity(intent);
                }
            }
        });

                /*
                Если есть запрос на поиск водителей, но заказчик решил отменить поиск,
                то удаляем всех видимых водителей, если они есть.
                Удаляем из БД привязку к водителю, если она не пустая.
                Отменяем отправку данных о пользователе в БД.
                Удаляем все маркеры с карты и меняем название кнопки на Вызвать такси.
                */

        searchBarA.setOnFocusChangeListener(new View.OnFocusChangeListener() {//Обработчик фокуса МП на первом поле ввода
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    bottomSheetBehavior.setDraggable(true);
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    bottomSheetBehavior.setDraggable(false);
                } else {
                    if (!searchBarB.hasFocus()) {
                        bottomSheetBehavior.setDraggable(true);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    }
                }
            }
        });

        searchBarB.setOnFocusChangeListener(new View.OnFocusChangeListener() {//Обработчик фокуса МП на втором поле ввода
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    bottomSheetBehavior.setDraggable(true);
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    bottomSheetBehavior.setDraggable(false);
                } else {
                    bottomSheetBehavior.setDraggable(true);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            }
        });

        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() { //Слушатель клавиатуры
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                if (!isOpen) {
                    searchBarA.clearFocus();
                    searchBarB.clearFocus();
                    bottomSheetBehavior.setDraggable(true);
                }
            }
        });

        //Заготовка под поиск
        //searchBarA.addTextChangedListener(new TextWatcher() {

        getStateCustomer(); //Метод для определения наличия информации о заказчике
        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (geoQuery != null) {
                    geoQuery.removeAllListeners();
                    if(DriverLocationRefListener != null) DriversLocationRef.removeEventListener(DriverLocationRefListener);
                    if(CustomerRideListener != null) DriversRef1.removeEventListener(CustomerRideListener);
                }
                if (driverFound) {
                    DriversRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID").child(customerID);
                    DriversRef.removeValue();
                    driverFoundID = "";
                    driverFound = false;
                }
                driverFound = false;
                radius = 1;
                section = 0;
                it = 1;

                if (PickUpMarker != null) {
                    PickUpMarker.remove();
                }

                if (driverMarker != null) {
                    driverMarker.remove();
                }
                if(!driversMap.isEmpty()){
                    driversMap.clear();
                }
                if (StateCustomer) { //Если есть информация о заказчике, то позволяем ему оформить заказ
                    if (searchBarA.length() > 0 && searchBarB.length() > 0 ) { //Если поля заполнены, то начинаем поиск
                            searchA = searchBarA.getText().toString();
                            searchB = searchBarB.getText().toString();
                            requestType = true;
                            new setLocationTask().execute(lastLocation);
                            if (PickUpMarker != null) PickUpMarker.remove();
                            CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Пункт подачи"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CustomerPosition, 20.0f));
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            getNearbyDrivers();
                    } else
                        if(requestType){
                            requestType = false;
                            if (geoQuery != null) {
                                geoQuery.removeAllListeners();
                                if(DriverLocationRefListener != null) DriversLocationRef.removeEventListener(DriverLocationRefListener);
                                if(CustomerRideListener != null) DriversRef1.removeEventListener(CustomerRideListener);
                            }
                            if (driverFound) {
                                DriversRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID").child(customerID);
                                DriversRef.removeValue();
                                driverFoundID = null;
                            }
                            if(!driversMap.isEmpty()){
                                driversMap.clear();
                            }
                            driverFound = false;
                            radius = 1;
                            section = 0;
                            it = 1;

                            if (PickUpMarker != null) {
                                PickUpMarker.remove();
                            }

                            if (driverMarker != null) {
                                driverMarker.remove();
                            }

                            geoFire1 = new GeoFire(CustomerDatabaseReference);
                            geoFire1.removeLocation(customerID);

                            cancelSearchStart();
                            zoomToUserLocation();
                    } else Toast.makeText(CustomersMapActivity.this, "Введите адрес", Toast.LENGTH_SHORT).show();

                } else
                    Toast.makeText(CustomersMapActivity.this, "Заполните информацию о себе", Toast.LENGTH_SHORT).show();
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000); //Интервал обновления геолокации
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10);
    }

    public class setLocationTask extends AsyncTask<Location, Void, GeoFire> {
        Location location;

        @Override
        protected GeoFire doInBackground(Location... locations) {
            mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
            customerID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

            if (locations.length > 0) {
                location = locations[0];
            }
            try {

                CustomerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Customers Requests"); //Обращаемся к запросам на заказ
                geoFire1 = new GeoFire(CustomerDatabaseReference);
                geoFire1.setLocation(customerID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            Log.d("TAG", "There was an error saving the location to GeoFire: " + error);
                        } else {
                            Log.d("TAG", "Location saved on server successfully!");
                        }
                    }
                });

                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("adressA", searchBarA.getText().toString());
                userMap.put("adressB", searchBarB.getText().toString());

                CustomerDatabaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            } catch (DatabaseException databaseException) {
                databaseException.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoFire geoFire) {
            super.onPostExecute(geoFire);

            searchStart();
        }
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
                        apiException.startResolutionForResult(CustomersMapActivity.this, 1001);
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
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("status", "Запустился onConnectionFailed()");
    }

    @Override
    public void onLocationChanged(Location location) {
        /*lastLocation = location;
        displayLocation();*/
        //Тут можно добавить метод вывода всех таксистов на карте
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("TAG", "Запустился onStart()");
        checkStatus();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            checkSettingsAndStartLocationUpdated();
        else askLocationPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("status", "Запустился onStop()");
        stopLocationUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("status", "Запустился onDestroy()");

        if (requestType && driverFoundID == "") {
            GeoFire geoFire = new GeoFire(CustomerDatabaseReference);
            geoFire.removeLocation(customerID);
            requestType = false;
        }
        stopLocationUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("status", "Запустился onResume()");
        checkStatus();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            checkSettingsAndStartLocationUpdated();
        else askLocationPermission();
    }

    private void checkStatus() {
        CustomerDatabaseReference.child(customerID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            requestType = true;
                            List<Object> customerLocationMap = (List<Object>) snapshot.child("l").getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            if (customerLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                                Log.d("status", String.format("LocationLat = %f", LocationLat));
                            }
                            if (customerLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                                Log.d("status", String.format("LocationLat = %f", LocationLng));
                            }

                            if (PickUpMarker != null) { //Удалим маркер если он есть
                                PickUpMarker.remove();
                            }
                            Location lastLocation = new Location("");
                            lastLocation.setLatitude(LocationLat);
                            lastLocation.setLongitude(LocationLng);

                            if (snapshot.getChildrenCount() > 2) {
                                String searchPointA = snapshot.child("adressA").getValue().toString();
                                String searchPointB = snapshot.child("adressB").getValue().toString();
                                searchA = searchPointA;
                                searchB = searchPointB;
                            }

                            CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                            //Анимация Камеры
                            PickUpMarker = mMap.addMarker(new MarkerOptions()
                                    .position(CustomerPosition)
                                    .title("Место подачи"));

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(PickUpMarker.getPosition(), 18.0f));

                            searchStart();

                            //displayLocation();
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

    }

    //Выходим на начальный экран и выполняем логаут для Заказчика
    private void LogoutCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    //Метод для поиска водителей поблизости
    private void getNearbyDrivers() {
        if(geoQuery != null) {
            geoQuery.removeAllListeners();
            Log.d("Status", "geoQuery.removeAllListeners(); ");
        }
        GeoFire geoFire = new GeoFire(DriversAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude + section, CustomerPosition.longitude), radius);
        //Начинаем поиск с позиции заказчика и ставим радиус, в последствии обновлеяем его

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(driversMap.isEmpty()){
                    driversMap.put("id1", key);
                    startListeners();
                }else
                    if (!driversMap.containsValue(key) & !driversMap.containsValue(String.format("minus%s", key)))  {
                        for (int i = 0; i < driversMap.size(); i++) {
                            if (driversMap.get(String.format("id%d", i + 1)) == " empty") {
                                driversMap.put(String.format("id%d", i + 1), key);
                                startListeners();
                                break;
                            }
                            if(i + 1 == driversMap.size()){
                                driversMap.put(String.format("id%d", driversMap.size() + 1), key);
                                startListeners();
                            }
                        }
                }
                Log.d("Status", String.format("KEY_ENTERED(%s)", driversMap));
            }

            @Override
            public void onKeyExited(String key) {
                Log.d("Status", String.format("KEY_Exited(%s)", key));
                    if(driversMap.containsValue(key)){
                        for(int i = 0; i < 15; i++){
                            if(driversMap.containsKey(String.format("id%d",i + 1))) {
                                if (driversMap.get(String.format("id%d", i + 1)) == key) {
                                    Log.d("Status", String.format("Удаляю ключ из БанЛиста и РайдID(%s)", key));
                                    driversMap.put(String.format("id%d", i + 1), " empty");
                                    CustomerDatabaseReference.child(customerID).child("CustomersBanList").child(key).removeValue(new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            Log.d("Status", "Удаляю ключ из CustomersBanList");
                                        }
                                    });
                                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key).child("CustomerRideID").child(customerID);
                                    DriversRef.removeValue(new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            Log.d("Status", "Удаляю ключ из CustomerRideID");
                                        }
                                    });
                                    Log.d("Status driversMap", String.format("вышел- %s", key));
                                    Log.d("Status DrivesrMap",String.format("Состояние DriversMap - %s", driversMap));
                                    if(driverFoundID != "" & key == driverFoundID) {
                                        driverFoundID = "";
                                        driverFound = false;
                                        getNearbyDrivers();
                                    }
                                    break;
                                }
                            }
                        }
                    }else if(driversMap.containsValue(String.format("minus%s", key))){
                        for(int i = 0; i < 15; i++){
                            if(driversMap.containsKey(String.format("id%d",i + 1))) {
                                if (driversMap.get(String.format("id%d", i + 1)) == String.format("minus%s", key)) {
                                    Log.d("Status", String.format("Удаляю ключ из БанЛиста и РайдID(%s)", key));
                                    driversMap.put(String.format("id%d", i + 1), " empty");
                                    CustomerDatabaseReference.child(customerID).child("CustomersBanList").child(key).removeValue(new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            Log.d("Status", "Удаляю ключ из CustomersBanList");
                                        }
                                    });
                                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key).child("CustomerRideID").child(customerID);
                                    DriversRef.removeValue(new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            Log.d("Status", "Удаляю ключ из CustomerRideID");
                                        }
                                    });
                                    Log.d("Status driversMap", String.format("вышел- %s", key));
                                    Log.d("Status DrivesrMap",String.format("Состояние DriversMap - %s", driversMap));
                                    if(driverFoundID != "" & key == driverFoundID) {
                                        driverFoundID = "";
                                        driverFound = false;
                                        getNearbyDrivers();
                                    }
                                    break;
                                }
                            }
                        }
                    }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("Status", String.format("KEY_Moved(%s,%f,%f)", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            sleep(3000);
                        }catch (Exception e){
                            e.printStackTrace();
                        } finally {
                            if (!driverFound && requestType) {
                                radius = radius + 1;
                                if(section >= 0 && section < 0.06 && section != 0.06)
                                    section = section + 0.01;
                                else section = 0;
                                getNearbyDrivers();
                                Log.d("Status Search", "Водитель не найден, ищу нового");
                                //Toast.makeText(CustomersMapActivity.this, "Водитель не найден, ищу нового", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                };
                thread.start();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
        //Если таксист не принял заказ, то ничего не делаем ни у заказчика ни у таксиста. см. логику на DriversMapActivity
    }

    private void startListeners(){
        if (!driverFound && requestType) {
            if(!driversMap.isEmpty()){
                if (geoQuery != null) geoQuery.removeAllListeners();
                    CustomerDatabaseReference.child(customerID).child("CustomersBanList").addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("DefaultLocale")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Object value = snapshot.getValue();
                            if(value instanceof Map){
                                Map<String, Boolean> driverIDs = (Map<String, Boolean>) value;
                                if (!driverIDs.containsKey(driversMap.get(String.format("id%d", it))) & it <= driversMap.size()) {
                                    if(!driversMap.get(String.format("id%d", it)).contains("minus")) {
                                        DatabaseReference DriverRefToRide = FirebaseDatabase.getInstance().getReference()
                                                .child("Users")
                                                .child("Drivers")
                                                .child(driversMap.get(String.format("id%d", it)))
                                                .child("CustomerRideID")
                                                .child(customerID);
                                        DriverRefToRide.setValue(true);
                                        driverFound = true;
                                        driverFoundID = driversMap.get(String.format("id%d", it));
                                        it = it + 1;
                                        startListeners();
                                    }
                                } else if(it > driversMap.size()) it = 1;
                                else if(it < driversMap.size()) it = it + 1;
                            } else {
                                if(!snapshot.exists() & it <= driversMap.size()) {
                                    if(!driversMap.get(String.format("id%d", it)).contains("minus")) {
                                        String path = driversMap.get(String.format("id%d", it));
                                        DatabaseReference DriverRefToRide = FirebaseDatabase.getInstance().getReference()
                                                .child("Users")
                                                .child("Drivers")
                                                .child(path)
                                                .child("CustomerRideID")
                                                .child(customerID);
                                        DriverRefToRide.setValue(true);
                                        driverFound = true;
                                        driverFoundID = path;
                                        it = it + 1;
                                        startListeners();
                                    }
                                } else if(it > driversMap.size()) it = 1;
                                else if(it < driversMap.size()) it = it + 1;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
            }
        }

        if(driverFound && requestType) {
            if(!driversMap.isEmpty()) {
                if (geoQuery != null) geoQuery.removeAllListeners();
                DriversRef1 = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                CustomerRideListener = DriversRef1.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull final DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                                Object value = snapshot.getValue();
                                if (value instanceof Map) {
                                    Map<String, Boolean> customerIDs = (Map<String, Boolean>) value;
                                    if (!customerIDs.containsKey(customerID)) {
                                        Log.d("Status Search","Снапшот в ремувед существует");
                                        DriversRef1.removeEventListener(CustomerRideListener);
                                        CustomerDatabaseReference.child(customerID).child("CustomersBanList").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                                Object value1 = snapshot2.getValue();
                                                if(value1 instanceof Map){
                                                    Map<String, Boolean> banList = (Map<String, Boolean>) value1;
                                                    if(banList.containsKey(driverFoundID)){
                                                        for (int i = 0; i < driversMap.size(); i++) {
                                                            if (driversMap.get(String.format("id%d", i + 1)) == driverFoundID) {
                                                                driversMap.put(String.format("id%d", i + 1), String.format("minus%s", driverFoundID));
                                                                driverFound = false;
                                                                driverFoundID = "";
                                                                getNearbyDrivers();
                                                                Log.d("Status Search", "В CustomersBanList обнаружен найденный такстист");
                                                            }
                                                        }

                                                    }
                                                } else if (!snapshot2.exists()) {
                                                    CustomerDatabaseReference.child(customerID).child("orderID").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot3) {
                                                            if(snapshot3.exists()){
                                                                orderID = (String) snapshot3.getValue();
                                                                showDialog();
                                                                orderApprove();
                                                            } else {
                                                                    Log.d("Status Search", "Пользователь не активен");
                                                                    driverFound = false;
                                                                    driverFoundID = "";
                                                                    getNearbyDrivers();
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                                }
                                                //тут функционал на формирование заказа, сюда попадаем если CustomerID и BanList пусты
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }

                                } else {
                                    DriversRef1.removeEventListener(CustomerRideListener);
                                    CustomerDatabaseReference.child(customerID).child("CustomersBanList").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                            Object value1 = snapshot2.getValue();
                                            if(value1 instanceof Map){
                                                Log.d("Status Search","Снапшота в ремувед не существует");
                                                Map<String, Boolean> banList = (Map<String, Boolean>) value1;
                                                if(banList.containsKey(driverFoundID)){
                                                    for (int i = 0; i < driversMap.size(); i++) {
                                                        if (driversMap.get(String.format("id%d", i + 1)) == driverFoundID) {
                                                            driversMap.put(String.format("id%d", i + 1), String.format("minus%s", driverFoundID));
                                                            driverFound = false;
                                                            driverFoundID = "";
                                                            getNearbyDrivers();
                                                            Log.d("Status Search", "В CustomersBanList обнаружен найденный такстист" + driversMap);
                                                        }
                                                    }
                                                }
                                            } else if (!snapshot2.exists()) {
                                                CustomerDatabaseReference.child(customerID).child("orderID").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot3) {
                                                        if(snapshot3.exists()){
                                                            orderID = (String) snapshot3.getValue();
                                                            showDialog();
                                                            orderApprove();
                                                        } else {
                                                            Log.d("Status Search", "Пользователь не активен");
                                                            driverFound = false;
                                                            driverFoundID = "";
                                                            getNearbyDrivers();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            }
                                            //тут функционал на формирование заказа, сюда попадаем если CustomerID и BanList пусты
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }

                            }
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    private void orderApprove() {
        geoFire1 = new GeoFire(CustomerDatabaseReference);
        geoFire1.removeLocation(customerID);
        requestType = false;
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(orderID);
        GeoFire geoFire = new GeoFire(orderRef);
        geoFire.setLocation("CustomerLocation", new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude));
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("tokenCustomer", customerID);

        orderRef.updateChildren(userMap);

        GetDriverLocation();
    }


    private void GetDriverLocation() {
        //Водитель найден. Обновляем данные геолокаци таксиста
        DriversLocationRef =  FirebaseDatabase.getInstance().getReference()
                .child("Orders")
                .child(orderID)
                .child("DriverLocation")
                .child("l");
        DriverLocationRefListener = DriversLocationRef.
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            if (driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());

                            }
                            if (driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                            if (driverMarker != null) { //Удалим лишних водителей
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

                            if (Distance < 50) {
                                infoButton.setText("Ваше такси подъезжает");
                            } else {
                                infoButton.setText("Такси в пути");
                            }

                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                    .title("Ваше такси тут"));
                        } else{
                            requestType = false;
                            if (geoQuery != null) {
                                geoQuery.removeAllListeners();
                                if(DriverLocationRefListener != null) DriversLocationRef.removeEventListener(DriverLocationRefListener);
                                if(CustomerRideListener != null) DriversRef1.removeEventListener(CustomerRideListener);
                            }
                            if (driverFound) {
                                DriversRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID").child(customerID);
                                DriversRef.removeValue();
                                driverFoundID = null;
                            }
                            if(!driversMap.isEmpty()){
                                driversMap.clear();
                            }
                            driverFound = false;
                            radius = 1;
                            section = 0;
                            it = 1;

                            if (PickUpMarker != null) {
                                PickUpMarker.remove();
                            }

                            if (driverMarker != null) {
                                driverMarker.remove();
                            }

                            geoFire1 = new GeoFire(CustomerDatabaseReference);
                            geoFire1.removeLocation(customerID);

                            cancelSearchStart();
                            zoomToUserLocation();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showLinearInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 1) {
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String carmodel = snapshot.child("carmodel").getValue().toString();
                    String carnumber = snapshot.child("carnumber").getValue().toString();
                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCar.setText(carmodel + "," + carnumber);

                    if (snapshot.hasChild("image")) {
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

    private void showDialog(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 1 && driverFoundID != "") {
                    dialog.show();
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String carmodel = snapshot.child("carmodel").getValue().toString();
                    String carnumber = snapshot.child("carnumber").getValue().toString();
                    driverName.setText(name);
                    driverPhone.setText(phone);
                    driverCar.setText(carmodel + "," + carnumber);
                    if (snapshot.hasChild("image")) {
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(driverImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getStateCustomer() {
        DatabaseReference CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        CustomerDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 1) {
                    StateCustomer = true;
                } else StateCustomer = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchStart(){
        searchBarA.setText("Поиск водителя...");
        searchBarB.setText("");
        searchBarA.setEnabled(false);
        searchBarB.setEnabled(false);
        searchBarB.setVisibility(View.INVISIBLE);
        callTaxiButton.setText("Отменить поиск");
        getNearbyDrivers();
    }

    private void cancelSearchStart(){
        searchBarA.setText("");
        searchBarA.setEnabled(true);
        searchBarB.setEnabled(true);
        searchBarB.setVisibility(View.VISIBLE);
        searchBarA.setText(searchA);
        searchBarB.setText(searchB);
        callTaxiButton.setText("Начать поиск");
    }

    private void driverFound(){

    }

}

