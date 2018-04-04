package org.eoin.route.routetesting;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //Map view
    MapView map;
    LocationController lc;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //Enables zoom
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        this.lc = new LocationController(this, ctx, map);
        lc.addOverlays();

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        GeoPoint startLocation = new GeoPoint(lc.getCurrentLocation());
        mapController.setCenter(startLocation);
        mapController.setZoom(16.0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            map.getOverlays().clear();
            map.invalidate();
            new HttpGraphRequestTask().execute("GetGraph");
            return true;
        }
        if (id == R.id.action_simpleLoop) {
            map.getOverlays().clear();
            map.invalidate();

            GeoPoint source = lc.getCurrentLocation();
            double latitude = source.getLatitude();
            double longitude = source.getLongitude();

            String sourceLat = "?sourceLat=" + String.valueOf(latitude);
            String sourceLon =  "?sourceLon=" + String.valueOf(longitude);
            String reqDistance =  "?reqDistance=" + "6";

            new HttpGraphRequestTask().execute("GetSimpleGraph", reqDistance, sourceLat, sourceLon);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        lc.setCurrentLocation();
        super.onStart();
        //new HttpGraphRequestTask().execute("GetGraph");
    }

    private void launchMapActivity(OSMEdge[] edges) {
        Intent mapIntent = new Intent(this, MapActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable("sourceLocation", lc.getCurrentLocation());
        extras.putSerializable("edges", edges);

        mapIntent.putExtras(extras);
        startActivity(mapIntent);
    }

    private class HttpGraphRequestTask extends AsyncTask<String, Void, OSMEdge[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            BoundingBox bb = map.getProjection().getBoundingBox();
            System.out.println("Bounding Box: " + bb);

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setCancelable(true);
            progressDialog.setMessage("Your route is being generated!");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected OSMEdge[] doInBackground(String... endpoint) {
            try {
                String url = "http://46.101.77.71:8080/drfr-backend/";
                if (endpoint[0].equals("GetSimpleGraph")){
                     url = url + endpoint[0] + endpoint[1] + "&" + endpoint[2] +  "&" + endpoint[3];
                } else {
                    url = url + endpoint[0];
                }
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                ResponseEntity<OSMEdge[]> responseEntity = restTemplate.getForEntity(url, OSMEdge[].class);

                OSMEdge[] edges =  responseEntity.getBody();

                return edges;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(OSMEdge[] edges) {
            progressDialog.dismiss();
            launchMapActivity(edges);
        }
    }

    public void onResume(){
        super.onResume();
        lc.setCurrentLocation();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume();
    }//End onResume()

    protected void onStop() {
        super.onStop();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }//End onStop()
}
