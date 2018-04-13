package org.eoin.route.routetesting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.testfairy.TestFairy;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import static android.content.ContentValues.TAG;

/*
This class handles grabbing and updating the users location
 */
public class LocationController implements LocationListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    Activity activity;
    Context context;
    MapView mMapView;
    GeoPoint currentLocation;
    LocationManager mgr;
    Marker myLocation;
    boolean added = false;
    boolean followme = false;

    //Constructor for MainActivity
    public LocationController(Activity activity, Context ctx, MapView m) {
        this.activity = activity;
        this.context = ctx;
        this.mMapView = m;
        //this.currentLocation = new GeoPoint(0.0, 0.0);
        setCurrentLocation();
    }//end LocationController

    //Constructor for Map Activity
    public LocationController(Activity activity, Context ctx, MapView m, GeoPoint cl, boolean fm) {
        this.activity = activity;
        this.context = ctx;
        this.mMapView = m;
        this.currentLocation = cl;
        this.followme = fm;
        mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setCurrentLocation();
    }//End LocationController

    //If it has the user location, it will display it on the map
    public void addOverlays() {
        mMapView.getOverlays().remove(myLocation);
        myLocation = new Marker(mMapView);
        myLocation.setIcon(context.getResources().getDrawable(R.drawable.marker_node));
        mMapView.getOverlays().add(myLocation);
    }//end addOverlays()

    //Called on creation to set up the Location services and ensure permissions
    public void setCurrentLocation() {
        mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        checkLocationPermission();
        checkLocationOn();
        try {
            Location gpsLocation = null;
            Location networkLocation = null;
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            mgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,  0, 0, this);

            gpsLocation = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            networkLocation = mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location = betterLocation(gpsLocation, networkLocation);
            if( location != null ) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.i("currentLocation1", String.valueOf(currentLocation));
            } else {
                Log.i("currentLocation", "In the Ivory Coast somewhere I guess: " + String.valueOf(currentLocation));
            }//end if else
        } catch (Exception ex) {
            ex.printStackTrace();
        }//end try catch
    }//end setCurrentLocation

    //Compares location providers and determines the best one
    private Location betterLocation(Location location1, Location location2) {
        System.out.println(location1 + " / " + location2);
        if (location1 == null && location2 == null) {
            return null;
        } else if (location1 != null && location2 == null) {
            return location1;
        } else if (location1 == null && location2 != null) {
            return location2;
        } else if (location1.getAccuracy() < location2.getAccuracy()) {
            return location1;
        } else {
            return location2;
        }//end long ass if else
    }//End betterLocation

    //Ensures that the user has given that application permission to access their device location
    //This will generally only happen the first time the application is launched
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously*
                //Simple AlertBox to ask the user to enable their location.
                final AlertDialog.Builder enableLocation = new AlertDialog.Builder(activity);
                enableLocation.setTitle("This application requires permission to access your location.");
                enableLocation.setMessage("Would you like to give this application location permissions?");

                //If user wishes to continue with their action
                enableLocation.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                    }//End onClick
                });// End Positive Button
                //If user does not wish to continue with their action
                enableLocation.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        activity.setResult(Activity.RESULT_CANCELED);
                    }//End onClick
                });//End Negative Button
                enableLocation.show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this.activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }//end if else
            return;
        }//end if
    }//End checkLocationPermission

    //Checks if the user has their location enabled, and if not will request they enable it
    private void checkLocationOn() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = mgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }//end try catch
        try {
            networkEnabled = mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }//end try catch

        if (!gpsEnabled && !networkEnabled) {
            final AlertDialog.Builder turnOnLocation = new AlertDialog.Builder(activity);
            //final AlertDialog el = enableLocation.create();
            turnOnLocation.setTitle("This application requires your location.");
            turnOnLocation.setMessage("Would you like to enable your location?");
            turnOnLocation.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(settings);
                    dialog.dismiss();
                }//End onClick
            });// End Positive Button
            turnOnLocation.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }//End onClick
            });//End Negative Button
            turnOnLocation.show();
        }//End if
    }//End checkLocationOn()

    //set the map to follow the user
    public void setFollow(boolean fm) {
        this.followme = fm;
    }//end setFollow()

    //Get the current location if available
    public GeoPoint getCurrentLocation() {
        if (this.currentLocation != null) {
            return this.currentLocation;
        } else {
            return null;
        }//end if else
    }//end getCurrentLocation

    //Stop the location services, generally called in onDestroy()
    public void stopLocationServices() {
        Log.d(TAG, "Stopping Location Services");
        if (mgr != null)
            try {
                mgr.removeUpdates(this);
                mgr = null;
            } catch (Exception ex) {
                ex.printStackTrace();
            }//end try catch
    }//end stopLocationServices()

    //This method will be called every time a change is detected in the users location
    @Override
    public void onLocationChanged(Location location) {
        TestFairy.updateLocation(location);
        currentLocation = new GeoPoint(location);
        Log.i("Activity: ", activity.getLocalClassName());

        //Activity specific actions
        if (activity.getLocalClassName().equals("MainActivity")) {
            TextView grabbingLocation = (TextView) activity.findViewById(R.id.grabbing_location);
            if (grabbingLocation.getVisibility() == TextView.VISIBLE) {
                grabbingLocation.setText("Location Found!");
                grabbingLocation.setVisibility(TextView.GONE);
            }//end if

            FloatingActionButton fab = (FloatingActionButton) activity.findViewById(R.id.fab);
            if (fab.getVisibility() == TextView.GONE) {
                grabbingLocation.setVisibility(TextView.VISIBLE);
            }//end if

            ToggleButton locationToggle = (ToggleButton) activity.findViewById(R.id.toggleDVL);
            if (locationToggle.getText().equals("Yes")) {
                EditText editTextLat = (EditText) activity.findViewById((R.id.editTextLat));
                EditText editTextLon = (EditText) activity.findViewById((R.id.editTextLon));
                editTextLat.setText(String.valueOf(currentLocation.getLatitude()));
                editTextLon.setText(String.valueOf(currentLocation.getLongitude()));

                mMapView.setMultiTouchControls(false);
                mMapView.setBuiltInZoomControls(false);

                /*
                BoundingBox bb = mMapView.getProjection().getBoundingBox();
                mMapView.setScrollableAreaLimitDouble(bb);
                mMapView.getController().animateTo(currentLocation);
                */
            }//end if
        } else if (activity.getLocalClassName().equals("MapActivity")) {
            final Button toggleRunButton = activity.findViewById(R.id.toggle_run);
            if (toggleRunButton.getText().equals("Stop Run") && followme == false) {
                followme = true;
            }//end if
        }//end if else

        Log.i("currentLocation2", String.valueOf(currentLocation));
        myLocation.setPosition(new GeoPoint(currentLocation));
        if (!added) {
            mMapView.getOverlayManager().add(myLocation);
            added = true;
        }//end if
        if (followme) {
            mMapView.getController().animateTo(myLocation.getPosition());
            mMapView.setMultiTouchControls(false);
            mMapView.setBuiltInZoomControls(false);
        }//end if
    }//End onLocationChanged()

    //Methods not used but required to implement LocationListener
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}//End Location Controller
