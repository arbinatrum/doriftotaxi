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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
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
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int ACCES_LOCATION_REQUEST_CODE = 10001;
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193;

    Dialog dialog;

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
                Log.d("TAG", "onLocationResult равен нулю ");
                return;
            }
            for (Location location : locationResult.getLocations()) {
                //displayLocation();
                Log.d("TAG", "onLocationResult: " + location.toString());
                lastLocation = location;
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

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
                Log.d("status Behavior",String.format("state is %d",bottomSheetBehavior.getState()));
                beforeBottomSheetState = bottomSheetBehavior.getState();
                if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) dialog.show();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d("Status",String.format("Положения - %f",slideOffset));
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
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); //Обращаемся к водителям в работе

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
                if (StateCustomer) { //Если есть информация о заказчике, то позволяем ему оформить заказ
                    if (searchBarA.length() > 0 && searchBarB.length() > 0 ) { //Если поля заполнены, то начинаем поиск
                        searchA = searchBarA.getText().toString();
                        searchB = searchBarB.getText().toString();
                        if (requestType) { //Если еще раз нажали на эту кнопку, то отменяем поиск заказа
                            requestType = false;
                            if (geoQuery != null) {
                                geoQuery.removeAllListeners();
                                DriversLocationRef.removeEventListener(DriverLocationRefListener);
                            }
                            if (driverFound) {
                                DriversRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                                DriversRef.removeValue();
                                driverFoundID = null;
                            }
                            driverFound = false;
                            radius = 1;

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
                        } else {
                            requestType = true;
                            new setLocationTask().execute(lastLocation);
                            if (PickUpMarker != null) PickUpMarker.remove();
                            CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Пункт подачи"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CustomerPosition, 20.0f));
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            //getNearbyDrivers();
                        }
                    } else if(requestType){
                        requestType = false;
                        if (geoQuery != null) {
                            geoQuery.removeAllListeners();
                            DriversLocationRef.removeEventListener(DriverLocationRefListener);
                        }
                        if (driverFound) {
                            DriversRef = FirebaseDatabase.getInstance().getReference()
                                    .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                            DriversRef.removeValue();
                            driverFoundID = null;
                        }
                        driverFound = false;
                        radius = 1;

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
        //checkStatus();
        stopLocationUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("status", "Запустился onDestroy()");

        if (requestType) {
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
        CustomerLocationRefListener = CustomerDatabaseReference.child(customerID)
                .addValueEventListener(new ValueEventListener() {
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
                                searchB = searchPointA;
                            }

                            CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                            //Анимация Камеры
                            PickUpMarker = mMap.addMarker(new MarkerOptions()
                                    .position(CustomerPosition)
                                    .title("Я нахожусь здесь"));

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
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);
                    //Привязка водителя к заказчику после нахождения


                    //GetDriverLocation();
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
                if (!driverFound) {
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
                        if (snapshot.exists() && requestType) {
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
                                    .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
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
        reference.addValueEventListener(new ValueEventListener() {
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