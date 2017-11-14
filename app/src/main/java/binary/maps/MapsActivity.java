package binary.maps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import binary.maps.adapter.StepAdapter;
import binary.maps.utils.CalculateDistance;
import binary.maps.utils.Constants;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener, View.OnClickListener, TextToSpeech.OnInitListener {
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LatLng mLastLatLng;

    private Gson gson = new Gson();

    private StepAdapter adapter;

    private RecyclerView rvDirections;
    private TextView tvDurationDistance;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabDirection;

    private PlaceAutocompleteFragment autocompleteFragment;
    private Place searchedPlace;

    private BottomSheetBehavior bottomSheetDirection;

    private ArrayList<String> directionList = new ArrayList<>();
    private ArrayList<LatLng> startLatLngList = new ArrayList<>();
    private ArrayList<LatLng> passedLatLngList = new ArrayList<>();
    private ArrayList<Double> distanceList = new ArrayList<>();

    private TextToSpeech tts;

    private int i;
    private double distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tts = new TextToSpeech(this, this);
        View btsDirections = findViewById(R.id.bottom_sheet_directions);
        bottomSheetDirection = BottomSheetBehavior.from(btsDirections);
        bottomSheetDirection.setPeekHeight(0);

        tvDurationDistance = findViewById(R.id.duration_distance);
        rvDirections = findViewById(R.id.rvDirections);

        fabDirection = findViewById(R.id.fab_direction);
        fabDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDirection.setPeekHeight(100);
                bottomSheetDirection.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        fabMyLocation = findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.getView().setBackgroundColor(Color.WHITE);
        findWay();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10);

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
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mLastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        moveToLocation(mLastLatLng);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        distanceList.clear();

        mLastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        for(i = 0; i < startLatLngList.size(); i++){
            if(i == 0){
                distance = CalculateDistance.calculateDistance(mLastLatLng, startLatLngList.get(i));
            }else{
                distance += CalculateDistance.calculateDistance(startLatLngList.get(i-1), startLatLngList.get(i));
            }
            distanceList.add(distance);
            Log.d("distance", String.valueOf(CalculateDistance.calculateDistance(mLastLatLng, startLatLngList.get(1))));
        }
        for(i = 0; i < distanceList.size(); i++){
            if(distanceList.get(i) < 0.04){
                tts.speak(String.valueOf(Html.fromHtml(directionList.get(i))), TextToSpeech.QUEUE_FLUSH, null);
                passedLatLngList.add(startLatLngList.get(i));
                directionList.remove(directionList.get(i));
                startLatLngList.remove(startLatLngList.get(i));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void moveToLocation(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng).zoom(12).build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }

    @Override
    public void onClick(View v) {
        moveToLocation(mLastLatLng);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (bottomSheetDirection.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetDirection.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        if (bottomSheetDirection.getPeekHeight() == 100) {
            bottomSheetDirection.setPeekHeight(0);
            bottomSheetDirection.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetDirection.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    public void findWay(){
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                directionList.clear();
                startLatLngList.clear();
                mMap.clear();
                searchedPlace = place;
                GoogleDirection.withServerKey(Constants.API_KEY)
                        .from(mLastLatLng)
                        .to(place.getLatLng())
                        .language("VI")
                        .alternativeRoute(true)
                        .transportMode(TransportMode.DRIVING)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction, String rawBody) {
                                Route route = direction.getRouteList().get(0);
                                Leg leg = route.getLegList().get(0);
                                Info distanceInfo = leg.getDistance();
                                Info durationInfo = leg.getDuration();
                                String distance = distanceInfo.getText();
                                String duration = durationInfo.getText();

                                List<Step> stepList = leg.getStepList();
                                for(Step step:stepList){
                                    directionList.add(step.getHtmlInstruction());
                                    startLatLngList.add(step.getStartLocation().getCoordination());
                                }
                                Log.d("old directionList", gson.toJson(directionList));
                                Log.d("old startLatLngList", gson.toJson(startLatLngList));
                                tvDurationDistance.setText(distance  + ", " + duration);
                                rvDirections.setHasFixedSize(true);
                                rvDirections.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                adapter = new StepAdapter(stepList);
                                rvDirections.setAdapter(adapter);

                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.RED);
                                mMap.addPolyline(polylineOptions);

                                mMap.addMarker(new MarkerOptions().position(place.getLatLng()));
                                moveToLocation(place.getLatLng());
                            }

                            @Override
                            public void onDirectionFailure(Throwable t) {

                            }
                        });
            }

            @Override
            public void onError(Status status) {
                mMap.clear();
            }
        });

    }
}
