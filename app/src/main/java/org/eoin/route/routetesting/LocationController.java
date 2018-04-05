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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import static android.content.ContentValues.TAG;

public class LocationController implements LocationListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    Activity activity;
    Context context;
    MapView mMapView;
    GeoPoint currentLocation;
    LocationManager mgr;
    Marker myLocation;
    boolean added = false;
    boolean followme=true;

    public LocationController(Activity activity, Context ctx, MapView m) {
        this.activity = activity;
        this.context = ctx;
        this.mMapView = m;
        this.currentLocation = new GeoPoint(0.0, 0.0);
        mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setCurrentLocation();
    }

    public LocationController(Activity activity, Context ctx, MapView m, GeoPoint cl) {
        this.activity = activity;
        this.context = ctx;
        this.mMapView = m;
        this.currentLocation = cl;
        mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setCurrentLocation();
    }

    public void addOverlays() {
        mMapView.getOverlays().remove(myLocation);
        myLocation = new Marker(mMapView);
        myLocation.setIcon(context.getResources().getDrawable(R.drawable.marker_node));
        mMapView.getOverlays().add(myLocation);
    }

    public void setCurrentLocation() {
        checkLocationPermission();
        checkLocationOn();
        try {
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this);
            Location location = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if( location != null ) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.i("currentLocation1", String.valueOf(currentLocation));
            } else {
                Log.i("currentLocation", "In the Ivory Coast somewhere I guess: " + String.valueOf(currentLocation));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //if (!enabled) {
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
            }
            return;
        }
    }

    public void checkLocationOn() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = mgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            networkEnabled = mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gpsEnabled && !networkEnabled) {
            final AlertDialog.Builder enableLocation = new AlertDialog.Builder(activity);
            //final AlertDialog el = enableLocation.create();
            enableLocation.setTitle("This application requires your location.");
            enableLocation.setMessage("Would you like to enable your location?");
            enableLocation.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(settings);
                }//End onClick
            });// End Positive Button
            enableLocation.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    activity.setResult(Activity.RESULT_CANCELED);
                }//End onClick
            });//End Negative Button
            enableLocation.show();
        }//End if
    }//End checkLocationOn()

    public GeoPoint getCurrentLocation() {
        return this.currentLocation;
    }

    public void onPause() {
        mMapView.onPause();
        if (mgr != null)
            try {
                mgr.removeUpdates(this);
                mgr = null;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mgr != null)
            try {
                mgr.removeUpdates(this);
                mgr = null;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location);
        Log.i("currentLocation2", String.valueOf(currentLocation));
        myLocation.setPosition(new GeoPoint(currentLocation));
        if (!added) {
            mMapView.getOverlayManager().add(myLocation);
            added = true;
        }
        if (followme) {
            mMapView.getController().animateTo(myLocation.getPosition());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
