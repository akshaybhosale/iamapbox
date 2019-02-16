package com.example.mohdadil.visit21;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

import java.util.List;
//import com.mapbox.android.core.location.LocationEngineListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private LocationLayerPlugin locationLayerPlugin;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    //private LocationEngine locationEngine;
    private Location ialastLocation;
    private float iaBearing;
    private int iaFloor;
   // private Location gpslastLocation;
    private LocationComponent locationComponent;
    private Style mStyle;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private IALocationManager mIALocationManager;


    private IALocationListener mListener = new IALocationListenerSupport() {

        @Override
        public void onLocationChanged(IALocation location) {


            ialastLocation = new Location("");
            ialastLocation.setLatitude(location.getLatitude());
            ialastLocation.setLongitude(location.getLongitude());
            iaBearing=location.getBearing();
            iaFloor=location.getFloorLevel();
            Log.d("loc tag", "new location received with coordinates from ia: " + ialastLocation.getLatitude()
                    + "," + ialastLocation.getLongitude());
            Log.d("locationDeatails",String.valueOf(location));


            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());

            enableLocationComponent(mStyle);


            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }


    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        findViewById(android.R.id.content).setKeepScreenOn(true);

        mIALocationManager = IALocationManager.create(this);
        mapView=findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/adil-khot/cjnivv3u029l02sk0f0pwwf19"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        mStyle=style;
                        enableLocationComponent(mStyle);
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            if (ialastLocation != null) {
                    //Log.d("latlng from", String.valueOf(ialastLocation));
                    locationComponent.forceLocationUpdate(ialastLocation);
            }
            else {
                Toast.makeText(this, "lastLocation empty",Toast.LENGTH_SHORT).show();
            }


            // Activate with options
            locationComponent.activateLocationComponent(this, loadedMapStyle);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            //tilt the camera
            //locationComponent.tiltWhileTracking(iaBearing,2000);


        } else {
            permissionsManager = new PermissionsManager((PermissionsListener) this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "give permission", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, "plz grant", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        //mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
        mIALocationManager.removeLocationUpdates(mListener);
    }
    @Override
    protected void onStop(){
        super.onStop();
        mapView.onStop();
        mIALocationManager.removeLocationUpdates(mListener);
    }
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mIALocationManager.destroy();
        mapView.onDestroy();

    }
}
