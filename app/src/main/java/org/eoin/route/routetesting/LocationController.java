package org.eoin.route.routetesting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
        setCurrentLocation();
    }

    public void addOverlays() {
        System.out.println("Adding Overlay");
        myLocation = new Marker(mMapView);
        myLocation.setIcon(context.getResources().getDrawable(R.drawable.marker_node));
        myLocation.setImage(context.getResources().getDrawable(R.drawable.marker_node));
        mMapView.getOverlays().add(myLocation);
    }

    public void setCurrentLocation() {
        System.out.println("In setCurrentLocation");
        mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //if (!enabled) {
                //Simple AlertBox to ask the user to enable their location.
                final AlertDialog.Builder enableLocation = new AlertDialog.Builder(context);
                enableLocation.setTitle("This application requires permission to access your location." +
                        "Would you like to enable your location?");

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
        try {
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this);
            Location location = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if( location != null ) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.i("currentLocation", String.valueOf(currentLocation));
            } else {
                Log.i("currentLocation", String.valueOf(currentLocation));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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
        System.out.println("In onLocationChanged");
        currentLocation = new GeoPoint(location);
        Log.i("currentLocation", String.valueOf(currentLocation));
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
