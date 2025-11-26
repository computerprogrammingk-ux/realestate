package com.example.realestate.activities;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityLocationPickerBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import java.util.Arrays;
import java.util.List;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityLocationPickerBinding binding;
    private static final String TAG="LOCATION_PICKER_TAG";
    private static final int DEFAULT_ZOOM=15;
    private GoogleMap mMap=null;
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Double selectedLatiude=null;
    private Double selectedLongitude=null;
    private String selectedAddress="";
    private String selectedCity="";
    private String selectedStatus="";
    private String selectedCountry="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  EdgeToEdge.enable(this);
        binding=ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.doneLl.setVisibility(View.GONE);

        SupportMapFragment mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFrameLayout);
        mapFragment.getMapAsync(this);

        Places.initialize(this,getString(R.string.my_maps_api_key));
        mPlacesClient=Places.createClient(this);
        mFusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);

        AutocompleteSupportFragment autocompleteSupportFragment=(AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        Place.Field[] placeFields = new Place.Field[]{
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        };

        autocompleteSupportFragment.setPlaceFields(Arrays.asList(placeFields));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Log.d(TAG,"onError: status: "+ status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG,"onPlaceSelected: place: "+ place);
                String id=place.getId();
                LatLng latLng =place.getLatLng();
                addressFromLatLng(latLng);
            }
        });



         binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 finish();
             }
         });
         binding.toolbarGpsBtn.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 if(isGpsEnabled()){

                     requstLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

                 }
                 else{

                     MyUtils.toast(LocationPickerActivity.this,"Location is not on! Turn it on to show current location");
                 }
             }
         });
         binding.doneBtn.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent=new Intent();
                intent.putExtra("latiude",selectedLatiude);
                intent.putExtra("longitude",selectedLongitude);
                intent.putExtra("address",selectedAddress);
                intent.putExtra("city",selectedCity);
                intent.putExtra("country",selectedCountry);
                intent.putExtra("state",selectedStatus);
                setResult(RESULT_OK,intent);
                finish();

             }
         });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMapClickListener(latLng -> addressFromLatLng(latLng));

        requstLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private ActivityResultLauncher<String> requstLocationPermission=registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG,"onActivityResult: isGranted: "+ isGranted);
                    if(isGranted){
                        mMap.setMyLocationEnabled(true);
                        pickCurrentPlace();
                    }else {
                        MyUtils.toast(LocationPickerActivity.this,"Permission denied...!");
                    }
                }
            }
    );

    private void pickCurrentPlace(){
        Log.d(TAG,"pickCurrentPlace: ");

        if(mMap==null){
            return;
        }
        delectAndShowDeviceLocationMp();
    }
    private void delectAndShowDeviceLocationMp(){
        try {

         @SuppressLint("MissingPermission") Task<Location> locationResult=mFusedLocationProviderClient.getLastLocation();
         locationResult.addOnSuccessListener(new OnSuccessListener<Location>() {
             @Override
             public void onSuccess(Location location) {

                 if(location!=null){


                     LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());

                     addressFromLatLng(latLng);
                 }
             }
         }).addOnFailureListener(new OnFailureListener() {
             @Override
             public void onFailure(@NonNull Exception e) {
                 Log.e(TAG,"onFailure: ",e);
             }
         });

        }catch (Exception e){
            Log.e(TAG,"delectAndShowDeviceLocationMp: ",e);

        }
    }
    private boolean isGpsEnabled(){
        LocationManager lm=(LocationManager)  getSystemService(LOCATION_SERVICE);

        boolean gpsEnabled=false;
        boolean newtworkEnabled=false;
        try {
            gpsEnabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch (Exception e){
            Log.e(TAG,"isGpsEnabled: ",e);
        }
        try {
            newtworkEnabled=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        } catch (Exception e) {
            Log.e(TAG,"isGpsEnabled: ",e);
        }
      //  return !(gpsEnabled && !newtworkEnabled);
        return gpsEnabled || newtworkEnabled; // ✅

    }

    private void addressFromLatLng(LatLng latLng){

        try {
            Geocoder geocoder=new Geocoder(this);
            List<Address> addressList=geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            Address address=addressList.get(0);
            String addressLine=address.getAddressLine(0);
            selectedLatiude=latLng.latitude;
            selectedLongitude=latLng.longitude;
            String subLocality=address.getSubLocality();
            selectedCountry=address.getCountryName();
            selectedStatus=address.getSubAdminArea();
            selectedCity=address.getLocality();
            selectedAddress=addressLine;

            Log.d(TAG,"addressFromLatLng: selectedLatiude: "+ selectedLatiude);
            Log.d(TAG,"addressFromLatLng: selectedLongitude: "+ selectedLongitude);
            Log.d(TAG,"addressFromLatLng: selectedStatus: "+ selectedStatus);
            Log.d(TAG,"addressFromLatLng: selectedCountry: "+ selectedCountry);
            Log.d(TAG,"addressFromLatLng: selectedCity: "+ selectedCity);
            Log.d(TAG,"addressFromLatLng: selectedAddress: "+ selectedAddress);
            addMarker(latLng,subLocality,addressLine);
        }catch (Exception e){
            Log.e(TAG,"addressFromLatLng: ",e);
        }
    }

    private  void addMarker(LatLng latLng, String title, String address){

        mMap.clear();
        try {
            MarkerOptions markerOptions=new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(title);
            markerOptions.snippet(address);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_ZOOM));

            binding.doneLl.setVisibility(VISIBLE);
            binding.selectedPlaceTv.setText(address);



        }catch (Exception e){
            Log.e(TAG,"addMarker: ",e);
        }

    }
}