package com.example.mohdadil.visit21;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARoute;
import com.indooratlas.android.sdk.IAWayfindingListener;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
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
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
//import com.mapbox.android.core.location.LocationEngineListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {

    private LocationLayerPlugin locationLayerPlugin;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private Location ialastLocation;
    private float iaBearing;
    private int iaFloor;
    private LocationComponent locationComponent;
    private Style mStyle;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private IALocationManager mIALocationManager;
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private View levelButtons;
    private Button currButton;
    private Button buttonFifthLevel;
    private Button buttonfourthLevel;
    private FeatureCollection featureCollection;
    private AnimatorSet animatorSet;
    private TextView nameTextView;
    private  TextView descriptionTextView;
    private LinearLayout poiCard;
    private boolean cardVisible = false;

    private List<Polyline> mPolylines = new ArrayList<>();
    private IARoute mCurrentRoute;

    private int mFloor;
    private IAWayfindingRequest mWayfindingDestination;

    private IAWayfindingListener mWayfindingListener = new IAWayfindingListener() {
        @Override
        public void onWayfindingUpdate(IARoute route) {
            mCurrentRoute = route;
            if (hasArrivedToDestination(route)) {
                mCurrentRoute = null;
                mWayfindingDestination = null;
                mIALocationManager.removeWayfindingUpdates();
            }
            updateRouteVisualization();
        }
    };


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rr, menu);
        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {


            case R.id.hod: {

                mWayfindingDestination = new IAWayfindingRequest.Builder()
                        .withFloor(4)
                        .withLatitude(19.07606178)
                        .withLongitude(72.99151853)
                        .build();

                mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);

                break;
            }


            case R.id.toilet: {

                mWayfindingDestination = new IAWayfindingRequest.Builder()
                        .withFloor(4)
                        .withLatitude(19.07600723)
                        .withLongitude(72.99196862)
                        .build();

                mIALocationManager.requestWayfindingUpdates(mWayfindingDestination, mWayfindingListener);

                break;
            }

        }

        return super.onOptionsItemSelected(item);
    }


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
            final int newFloor = location.getFloorLevel();
            if (mFloor != newFloor) {
                updateRouteVisualization();
            }
            mFloor = newFloor;

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

        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/adil-khot/cjrs2yradf2g42tocp5czguq5"),
                style -> {
                    mStyle=style;
                    levelButtons = findViewById(R.id.floor_level_buttons);
                    final List<Point> boundingBox = new ArrayList<>();

                    boundingBox.add(Point.fromLngLat(72.9914219677448, 19.0762151432401));
                    boundingBox.add(Point.fromLngLat(72.9920905083418, 19.0761612762958));
                    boundingBox.add(Point.fromLngLat(72.9913944751024, 19.075908418288));
                    boundingBox.add(Point.fromLngLat(72.9920636862516, 19.075853917514));
                    boundingBoxList = new ArrayList<>();
                    boundingBoxList.add(boundingBox);

                    mapboxMap.addOnCameraMoveListener(() -> {
                        if(mapboxMap.getCameraPosition().zoom < 21){
                            if(cardVisible){
                                poiCard = (LinearLayout)findViewById(R.id.poicard);
                                poiCard.setVisibility(View.GONE);
                            }
                        }
                        if (mapboxMap.getCameraPosition().zoom > 18) {
                            if (TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                                    mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))) {
                                if (levelButtons.getVisibility() != View.VISIBLE) {
                                    showLevelButton();
                                }
                            }
                        } else if (levelButtons.getVisibility() == View.VISIBLE) {
                            hideLevelButton();
                        }
                    });

                    indoorBuildingSource = new GeoJsonSource(
                            "indoor-building", loadJsonFromAsset("fourth.geojson"));
                    style.addSource(indoorBuildingSource);
                    featureCollection = FeatureCollection.fromJson(loadJsonFromAsset("fourth.geojson"));
                    setupLayer();
                    currButton = buttonfourthLevel;
                    ActivateButton(currButton);

                    // Add the building layers since we know zoom levels in range
                    //loadBuildingLayer(style);
                    enableLocationComponent(mStyle);
                    mapboxMap.addOnMapClickListener(this);
                });

        buttonFifthLevel = findViewById(R.id.fifth_level_button);
        buttonFifthLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                indoorBuildingSource.setGeoJson(loadJsonFromAsset("fifth.geojson"));
                featureCollection = FeatureCollection.fromJson("fifth.geojson");
                setupLayer();
                    DeactiveButton(currButton);
                    currButton=buttonFifthLevel;
                    ActivateButton(currButton);
            }
        });

        buttonfourthLevel = findViewById(R.id.fourth_level_button);
        buttonfourthLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                indoorBuildingSource.setGeoJson(loadJsonFromAsset("fourth.geojson"));
                featureCollection = FeatureCollection.fromJson("fourth.geojson");
                setupLayer();
                    DeactiveButton(currButton);
                    currButton=buttonfourthLevel;
                    ActivateButton(currButton);
            }
        });
    }
    private void removeLayermine(){
            Layer extrusionlayer=mapboxMap.getStyle().getLayer("extrusion-layer");
            Layer poilayer=mapboxMap.getStyle().getLayer("poi-layer");
            if(extrusionlayer!=null && poilayer!=null)
            {
                mStyle.removeLayer(extrusionlayer);
                mStyle.removeLayer(poilayer);
            }
    }
    public void setupLayer(){
        removeLayermine();
        mStyle.addLayer(new FillExtrusionLayer("extrusion-layer", "indoor-building").withProperties(
                fillExtrusionColor(Color.rgb(129, 236, 236)),
                fillExtrusionOpacity(0.7f),
                fillExtrusionHeight((float)3)));
        mStyle.addLayer(new SymbolLayer("poi-layer","indoor-building").withProperties(
                iconImage("{poi}-15"),
                iconAllowOverlap(true),
                iconSize(1f))
        );

    }

    private void ActivateButton(Button b){
        b.setTextColor(Color.BLUE);
        b.setBackgroundColor(Color.WHITE);
    }

    private void DeactiveButton(Button b){
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.BLUE);
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

    private boolean hasArrivedToDestination(IARoute route) {
        // empty routes are only returned when there is a problem, for example,
        // missing or disconnected routing graph
        if (route.getLegs().size() == 0) {
            return false;
        }

        final double FINISH_THRESHOLD_METERS = 8.0;
        double routeLength = 0;
        for (IARoute.Leg leg : route.getLegs()) routeLength += leg.getLength();
        return routeLength < FINISH_THRESHOLD_METERS;
    }

    /**
     * Clear the visualizations for the wayfinding paths
     */
    private void clearRouteVisualization() {
        for (Polyline pl : mPolylines) {
            pl.remove();
        }
        mPolylines.clear();
    }

    /**
     * Visualize the IndoorAtlas Wayfinding route on top of the Google Maps.
     */
    private void updateRouteVisualization() {

        clearRouteVisualization();

        if (mCurrentRoute == null) {
            return;
        }

        for (IARoute.Leg leg : mCurrentRoute.getLegs()) {

            if (leg.getEdgeIndex() == null) {
                // Legs without an edge index are, in practice, the last and first legs of the
                // route. They connect the destination or current location to the routing graph.
                // All other legs travel along the edges of the routing graph.

                // Omitting these "artificial edges" in visualization can improve the aesthetics
                // of the route. Alternatively, they could be visualized with dashed lines.
                continue;
            }

            PolylineOptions opt = new PolylineOptions();
            opt.add(new LatLng(leg.getBegin().getLatitude(), leg.getBegin().getLongitude()));
            opt.add(new LatLng(leg.getEnd().getLatitude(), leg.getEnd().getLongitude()));

            // Here wayfinding path in different floor than current location is visualized in
            // a semi-transparent color
            if (leg.getBegin().getFloor() == mFloor && leg.getEnd().getFloor() == mFloor) {
                opt.color(0xFF0000FF);
            } else {
                opt.color(0x300000FF);
            }

            mPolylines.add(mapboxMap.addPolyline(opt));
        }
    }


    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(pixel, "poi-layer");
        if(!features.isEmpty()){
            String name = features.get(0).getStringProperty("name");
            List<Feature> featureList = featureCollection.features();
            for (int i = 0; i < featureList.size(); i++) {
                if(featureList.get(i).hasProperty("name")){
                    if (featureList.get(i).getStringProperty("name").equals(name)) {
                        setSelected(i, true);
                    }
                }
            }
        }
        return false;
    }

    private void setSelected(int index, boolean flag){
        deselectAll();
        Feature feature = featureCollection.features().get(index);
        selectFeature(feature);
        animateCameraToSelection(feature,21.0);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                poiCard= (LinearLayout)findViewById(R.id.poicard);
                nameTextView =(TextView)findViewById(R.id.name);
                descriptionTextView=(TextView)findViewById(R.id.description);
                nameTextView.setText(feature.getStringProperty("name"));
                descriptionTextView.setText(feature.getStringProperty("description"));
                Log.d("name: ",(String) nameTextView.getText());
                Log.d("description: ",(String) descriptionTextView.getText());
                cardVisible=true;
                poiCard.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void deselectAll() {
        for (Feature feature : featureCollection.features()) {
            feature.properties().addProperty("selected", false);
            if(cardVisible){
                poiCard= (LinearLayout)findViewById(R.id.poicard);
                poiCard.setVisibility(View.GONE);
                cardVisible=false;
            }
        }
    }
    private void selectFeature(Feature feature) {
        feature.properties().addProperty("selected", true);
    }
    private void animateCameraToSelection(Feature feature, double newZoom) {
        CameraPosition cameraPosition = mapboxMap.getCameraPosition();

        if (animatorSet != null) {
            animatorSet.cancel();
        }

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                createLatLngAnimator(cameraPosition.target, convertToLatLng(feature)),
                createZoomAnimator(cameraPosition.zoom, newZoom)
        );
        animatorSet.start();
    }

    private Animator createLatLngAnimator(LatLng currentPosition, LatLng targetPosition) {
        ValueAnimator latLngAnimator = ValueAnimator.ofObject(new LatLngEvaluator(), currentPosition, targetPosition);
        latLngAnimator.setDuration(1950);
        latLngAnimator.setInterpolator(new FastOutSlowInInterpolator());
        latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLng((LatLng) animation.getAnimatedValue()));
            }
        });
        return latLngAnimator;
    }

    private Animator createZoomAnimator(double currentZoom, double targetZoom) {
        ValueAnimator zoomAnimator = ValueAnimator.ofFloat((float) currentZoom, (float) targetZoom);
        zoomAnimator.setDuration(1950);
        zoomAnimator.setInterpolator(new FastOutSlowInInterpolator());
        zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mapboxMap.moveCamera(CameraUpdateFactory.zoomTo((Float) animation.getAnimatedValue()));
            }
        });
        return zoomAnimator;
    }
    private LatLng convertToLatLng(Feature feature) {
        Point symbolPoint = (Point) feature.geometry();
        return new LatLng(symbolPoint.latitude(), symbolPoint.longitude());
    }

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

        private final LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }
}

