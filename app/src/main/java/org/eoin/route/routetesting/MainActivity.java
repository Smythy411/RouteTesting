package org.eoin.route.routetesting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    //Map view
    MapView map;
    LocationController lc;
    private MyLocationNewOverlay mLocationOverlay;

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
        lc.setCurrentLocation();
        lc.addOverlays();

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        mapController.setZoom(17);
        mapController.setCenter(lc.getCurrentLocation());
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
            new HttpGraphRequestTask().execute("GetSimpleGraph");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        lc.setCurrentLocation();
        super.onStart();
        new HttpGraphRequestTask().execute("GetGraph");
    }

    private class HttpGraphRequestTask extends AsyncTask<String, Void, OSMEdge[]> {
        @Override
        protected OSMEdge[] doInBackground(String... endpoint) {
            try {

                final String url = "http://46.101.77.71:8080/drfr-backend/" + endpoint[0];
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
            ArrayList<GeoPoint> waypoints = new ArrayList<>();

            OSMNode source = edges[0].getSourceNode();
            waypoints.add(new GeoPoint(Double.parseDouble(source.getLat()),
                    Double.parseDouble(source.getLon())));
            for (int i = 0; i < edges.length; i++) {
                Log.i("Edge " + i + ": ", edges[i].toString());
                OSMNode target = edges[i].getTargetNode();
                waypoints.add(new GeoPoint(Double.parseDouble(target.getLat()),
                        Double.parseDouble(target.getLon())));
            }

            TextView nodeIdText = (TextView) findViewById(R.id.node_value);
            nodeIdText.setText(Long.toString(source.getNodeID()));

            //Sets the inital zoom level and starting location
            IMapController mapController = map.getController();
            mapController.setZoom(17);
            mapController.setCenter(waypoints.get(0));

            //Simple marker for the starting node of the route
            Marker startMarker = new Marker(map);
            startMarker.setPosition(waypoints.get(0));
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(startMarker);
            startMarker.setTitle("Start point");

            map.invalidate();

            new UpdateRoadTask().execute(waypoints);
        }
    }

    /**
     * Async task to get the road in a separate thread.
     * Credit to https://stackoverflow.com/questions/21213224/roadmanager-for-osmdroid-error
     */
    private class UpdateRoadTask extends AsyncTask<Object, Void, Road> {

        protected Road doInBackground(Object... params) {
            @SuppressWarnings("unchecked")
            ArrayList<GeoPoint> waypoints = (ArrayList<GeoPoint>)params[0];
            RoadManager roadManager = new OSRMRoadManager(getApplicationContext());


            return roadManager.getRoad(waypoints);
        }//End doInBackground()
        @Override
        protected void onPostExecute(Road result) {
            Road road = result;
            // showing distance and duration of the road
            Toast.makeText(MainActivity.this, "distance="+road.mLength, Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity.this, "duration="+road.mDuration, Toast.LENGTH_SHORT).show();

            if(road.mStatus != Road.STATUS_OK)
                Toast.makeText(MainActivity.this, "Error when loading the road - status="+road.mStatus, Toast.LENGTH_SHORT).show();
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

            map.getOverlay().clear();
            map.getOverlays().add(roadOverlay);
            map.invalidate();

            //Adding visible icons for each node in route
            //Being able to handle these nodes is very important.
            Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
            for (int i=0; i<road.mNodes.size(); i++){
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step "+i);
                nodeMarker.setSnippet(node.mInstructions);
                nodeMarker.setSubDescription(Road.getLengthDurationText(MainActivity.this, node.mLength, node.mDuration));
                map.getOverlays().add(nodeMarker);
            }//End for
        }//End onPostExecute()
    }//End UpdateRoadTask()

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
