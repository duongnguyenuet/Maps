package binary.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import android.widget.ImageButton;
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

import java.io.IOException;
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
    private ImageButton btnClose;

    private PlaceAutocompleteFragment autocompleteFragment;
    private Place searchedPlace;
    private Geocoder geocoder;

    private BottomSheetBehavior bottomSheetDirection;

    private ArrayList<String> directionList = new ArrayList<>();
    private ArrayList<LatLng> startLatLngList = new ArrayList<>();
    private ArrayList<LatLng> passedLatLngList = new ArrayList<>();
    private ArrayList<Double> distanceList = new ArrayList<>();

    private TextToSpeech tts;

    private int i;
    private double distanceToNext;
    private double distanceToStart;
    private double distanceStartNext;
    private double angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tts = new TextToSpeech(this, this);
        final View btsDirections = findViewById(R.id.bottom_sheet_directions);
        bottomSheetDirection = BottomSheetBehavior.from(btsDirections);
        bottomSheetDirection.setPeekHeight(0);

        tvDurationDistance = findViewById(R.id.duration_distance);
        rvDirections = findViewById(R.id.rvDirections);

        fabDirection = findViewById(R.id.fab_direction);
        fabDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btsDirections.setVisibility(View.VISIBLE);
                bottomSheetDirection.setPeekHeight(100);
                bottomSheetDirection.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        fabMyLocation = findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(this);

        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btsDirections.setVisibility(View.GONE);
            }
        });
        geocoder = new Geocoder(this);

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
        if(startLatLngList.size() != 0 ){
            List<Address> matches = null;
            try {
                matches = geocoder.getFromLocation(startLatLngList.get(0).latitude, startLatLngList.get(0).longitude, 1);
                Address bestMatch = (matches.isEmpty() ? null : matches.get(0));

            } catch (IOException e) {
                e.printStackTrace();
            }

            distanceToStart = CalculateDistance.formatNumberDouble(CalculateDistance.calculateDistance(mLastLatLng, passedLatLngList.get(0)));
            distanceToNext = CalculateDistance.formatNumberDouble(CalculateDistance.calculateDistance(mLastLatLng, startLatLngList.get(0)));
            distanceStartNext = CalculateDistance.formatNumberDouble(CalculateDistance.calculateDistance(passedLatLngList.get(0), startLatLngList.get(0)));
            angle = CalculateDistance.computeAngle(distanceToStart, distanceToNext, distanceStartNext);
            Log.d("angle", String.valueOf(angle));
            Log.d("distance", String.valueOf(distanceToNext));
            if(angle > 150 || Double.isNaN(angle) || angle == 0.0){
                if(distanceToNext < 0.02){
                    Log.d("direction 1", String.valueOf(Html.fromHtml(directionList.get(0))));
                    passedLatLngList.remove(passedLatLngList.get(0));
                    passedLatLngList.add(startLatLngList.get(0));
                    directionList.remove(directionList.get(0));
                    startLatLngList.remove(startLatLngList.get(0));
                }
            } else {
                directionList.clear();
                startLatLngList.clear();
                passedLatLngList.clear();
                mMap.clear();
                GoogleDirection.withServerKey(Constants.API_KEY)
                        .from(mLastLatLng)
                        .to(searchedPlace.getLatLng())
                        .language("VI")
                        .alternativeRoute(true)
                        .transportMode(TransportMode.DRIVING)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction, String rawBody) {
                                passedLatLngList.add(mLastLatLng);
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
                                tvDurationDistance.setText(distance  + ", " + duration);
                                rvDirections.setHasFixedSize(true);
                                rvDirections.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                adapter = new StepAdapter(stepList);
                                rvDirections.setAdapter(adapter);

                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                                PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.RED);
                                mMap.addPolyline(polylineOptions);

                                mMap.addMarker(new MarkerOptions().position(searchedPlace.getLatLng()));
                            }

                            @Override
                            public void onDirectionFailure(Throwable t) {

                            }
                        });
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
                                passedLatLngList.add(mLastLatLng);
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
