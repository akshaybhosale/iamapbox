package com.example.mohdadil.visit21;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
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
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
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
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private View levelButtons;


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
    public void onMapReady(@NonNull MapboxMap mmapboxMap) {
        MainActivity.this.mapboxMap = mmapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/adil-khot/cjnivv3u029l02sk0f0pwwf19"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        mStyle=style;
                        levelButtons = findViewById(R.id.floor_level_buttons);
                        final List<Point> boundingBox = new ArrayList<>();

                        boundingBox.add(Point.fromLngLat(72.9914219677448, 19.0762151432401));
                        boundingBox.add(Point.fromLngLat(72.9920905083418, 19.0761612762958));
                        boundingBox.add(Point.fromLngLat(72.9913944751024, 19.075908418288));
                        boundingBox.add(Point.fromLngLat(72.9920636862516, 19.075853917514));
                        boundingBoxList = new ArrayList<>();
                        boundingBoxList.add(boundingBox);

                        mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
                            @Override
                            public void onCameraMove() {
                                if (mapboxMap.getCameraPosition().zoom > 16) {
                                    if (TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                                            mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))) {
                                        if (levelButtons.getVisibility() != View.VISIBLE) {
                                            showLevelButton();
                                        }
                                    } else {
                                        if (levelButtons.getVisibility() == View.VISIBLE) {
                                            hideLevelButton();
                                        }
                                    }
                                } else if (levelButtons.getVisibility() == View.VISIBLE) {
                                    hideLevelButton();
                                }
                            }
                        });

                        indoorBuildingSource = new GeoJsonSource(
                                "indoor-building", loadJsonFromAsset("fourth.geojson"));
                        style.addSource(indoorBuildingSource);

                        // Add the building layers since we know zoom levels in range
                        loadBuildingLayer(style);
                        enableLocationComponent(mStyle);
                    }
                });

        Button buttonFifthLevel = findViewById(R.id.fifth_level_button);
        buttonFifthLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                indoorBuildingSource.setGeoJson(loadJsonFromAsset("fifth.geojson"));
            }
        });

        Button buttonfourthLevel = findViewById(R.id.fourth_level_button);
        buttonfourthLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                indoorBuildingSource.setGeoJson(loadJsonFromAsset("fourth.geojson"));
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
            locationComponent.setCameraMode(CameraMode.TRACKING);

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

    private void hideLevelButton() {
        // When the user moves away from our bounding box region or zooms out far enough the floor level
        // buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }

    private void showLevelButton() {
        // When the user moves inside our bounding box region or zooms in to a high enough zoom level,
        // the floor level buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }

    private void loadBuildingLayer(@NonNull Style style) {
        // Method used to load the indoor layer on the map. First the fill layer is drawn and then the
        // line layer is added.

        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill", "indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")),
                // Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
                // necessary to show the indoor map at high zoom levels.
                fillOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));

        style.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line", "indoor-building").withProperties(
                lineColor(Color.parseColor("#50667f")),
                lineWidth(0.5f),
                lineOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));
        style.addLayer(indoorBuildingLineLayer);
    }

    private String loadJsonFromAsset(String filename) {
        // Using this method to load in GeoJSON files from the assets folder.

        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
