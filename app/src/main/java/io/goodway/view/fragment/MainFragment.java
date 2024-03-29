package io.goodway.view.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.goodway.MainActivity;
import io.goodway.R;
import io.goodway.WayActivity;
import io.goodway.model.network.GoodwayHttpsClient;
import io.goodway.navitia_android.Action;
import io.goodway.navitia_android.Address;


/**
 * Created by sauray on 14/03/15.
 */
public class MainFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient googleApiClient;
    private View root;
    private boolean openedState;
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private FloatingActionButton floatingActionButton;
    private CoordinatorLayout coordinatorLayout;

    private LinearLayout bottom;

    private Location userLocation;
    private TextView fromText, toText;

    private Address from, to;
    private String mail, password;

    private static final int ACCESS_FINE_LOCATION=1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_map, container, false);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            ACCESS_FINE_LOCATION);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
            else{
                googleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
        }
        else{
            googleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        SharedPreferences shared_preferences = getActivity().getSharedPreferences("shared_preferences_test",
                getActivity().MODE_PRIVATE);
        mail = shared_preferences.getString("mail", null);
        password = shared_preferences.getString("password", null);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        bottom = (LinearLayout) root.findViewById(R.id.bottom);
        fromText = (TextView) root.findViewById(R.id.from);
        toText = (TextView) root.findViewById(R.id.to);
        coordinatorLayout = (CoordinatorLayout) root.findViewById(R.id.coordinatorLayout);

        floatingActionButton = (FloatingActionButton) root.findViewById(R.id.fab);
        floatingActionButton.setBaselineAlignBottom(true);
        floatingActionButton.setOnClickListener(this);

        Bundle extras = getArguments();
        LatLng latLng = null;
        if (extras.getParcelable("DEPARTURE") != null) {
            from = extras.getParcelable("DEPARTURE");

            fromText.setText(extras.getParcelable("DEPARTURE").toString());
            //latLng = new LatLng(a.getLatitude(), a.getLongitude());
        }

        if (extras.getParcelable("DESTINATION") != null) {
            to = extras.getParcelable("DESTINATION");
            toText.setText(to.toString());
            //latLng = new LatLng(a.getLatitude(), a.getLongitude());
        }
        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    googleApiClient = new GoogleApiClient.Builder(getActivity())
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public String toString() {
        return "Bottom Sheet";
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setPadding(0, 0, 0, bottom.getHeight());
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setOnMyLocationChangeListener(this);
        positionMap();
    }

    private void positionMap(){
        CameraPosition cameraPosition = null;
        if (from == null && to == null && userLocation != null) {
            cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()))      // Sets the center of the map to Mountain View
                    .zoom(13)                   // Sets the zoom
                    .build();                   // Creates a CameraPosition from the builder
        } else {
            if (from != null) {
                cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(from.getLatitude(), from.getLongitude()))      // Sets the center of the map to Mountain View
                        .zoom(13)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(from.getLatitude(), from.getLongitude()))
                                //.alpha(0.8f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        .title(getString(R.string.departure)));
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            if (to != null) {
                cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(to.getLatitude(), to.getLongitude()))      // Sets the center of the map to Mountain View
                        .zoom(13)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(to.getLatitude(), to.getLongitude()))
                                //.alpha(0.8f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title(getString(R.string.arrival)));
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
        if (cameraPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void setFrom(Address from) {
        this.from = from;
        Log.d("setfrom", "setfrom: " + from.toString());
        fromText.setText(from.toString());
        Log.d("setTo", "setTo : " + from.getLatitude() + ";" + from.getLongitude());
    }

    public void setTo(Address to) {
        this.to = to;
        Log.d("setfrom", "setfrom: " + to.toString());
        toText.setText(to.toString());
        Log.d("setTo", "setTo : " + to.getLatitude() + ";" + to.getLongitude());
        /*
*/
    }


    @Override
    public void onResume() {
        super.onResume();
        if(googleApiClient!=null){googleApiClient.connect();}
    }

    @Override
    public void onPause() {
        super.onPause();
        if (googleApiClient!=null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        userLocation = location;
    }


    @Override
    public void onConnected(Bundle bundle) {
        userLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
        List<android.location.Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(userLocation.getLatitude(), userLocation.getLongitude(), 1);
            if (addresses.size() > 0){
                GoodwayHttpsClient.updateUserCity(getActivity(), new Action<Boolean>() {
                    @Override
                    public void action(Boolean e) {

                    }
                }, null, mail, password, addresses.get(0).getLocality());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(googleMap!=null){
            positionMap();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(from != null){
            TextView from = (TextView) (root.findViewById(R.id.from));
            from.setText(getString(R.string.your_location) + " (" + getString(R.string.unavailable) + ")");
        }
        else if (to != null){
            TextView to = (TextView) (root.findViewById(R.id.to));
            to.setText(getString(R.string.your_location)+" ("+getString(R.string.unavailable)+")");
        }
    }

    @Override
    public void onClick(View v) {
        if(this.from == null && this.to==null){
            Snackbar.make(coordinatorLayout, R.string.no_to, Snackbar.LENGTH_LONG)
                    .setAction(R.string.select, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity)getActivity()).changeLocation(MainActivity.DESTINATION);
                        }
                    }).show();
        }
        else{
            if(this.from==null && userLocation!=null){
                Log.d("setting user location", "setting user location");
                this.from = new Address(getString(R.string.your_location), userLocation.getLatitude(), userLocation.getLongitude());
            }
            else if (this.to == null & userLocation!=null){
                Log.d("setting user location", "setting user location");
                this.to = new Address(getString(R.string.your_location), userLocation.getLatitude(), userLocation.getLongitude());
            }

            if(this.from==null){
                Snackbar.make(coordinatorLayout, R.string.your_location_not_available, Snackbar.LENGTH_LONG)
                        .setAction(R.string.select, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((MainActivity)getActivity()).changeLocation(MainActivity.DEPARTURE);
                            }
                        }).show();
            }
            else if(this.to==null){
                Snackbar.make(coordinatorLayout, R.string.your_location_not_available, Snackbar.LENGTH_LONG)
                        .setAction(R.string.select, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((MainActivity)getActivity()).changeLocation(MainActivity.DESTINATION);
                            }
                        }).show();
            }
            else{
                Intent intent = new Intent(getActivity(), WayActivity.class);
                intent.putExtra("FROM", this.from);
                intent.putExtra("TO", this.to);

                Log.d(from.toString(), "from");
                Log.d(to.toString(), "to");
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    //ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity());
                    startActivity(intent);
                }
                else{
                    startActivity(intent);
                }
            }

        }

    }
}
