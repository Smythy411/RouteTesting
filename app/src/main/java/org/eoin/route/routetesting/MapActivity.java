package org.eoin.route.routetesting;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

import javax.crypto.SealedObject;

/*
This Activity will display the generated Route to the user.
The user is able to initiate a run on that route
 */

public class MapActivity extends AppCompatActivity {

    MapView map;
    LocationController lc;
    Chronometer mChrono;
    long timeWhenStopped = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Context ctx = getApplicationContext();
        //important! Setting user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //Retrieving information that has been passed from previous activity
        Intent intent = getIntent();
        final GeoPoint source = (GeoPoint) intent.getSerializableExtra("sourceLocation");
        final OSMEdge[] edges = (OSMEdge[]) intent.getSerializableExtra("edges");
        double routeLength = intent.getDoubleExtra("routeLength", 0.0);
        BoundingBox dublin = (BoundingBox) intent.getSerializableExtra("dublin");
        System.out.println("Source: " + source);

        //Setting up map
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setScrollableAreaLimitDouble(dublin);

        map.setMaxZoomLevel(18.0);
        map.setMinZoomLevel(13.0);

        //Enables zoom
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        this.lc = new LocationController(this, ctx, map, source, false);
        lc.addOverlays();

        //Sets the inital zoom level and starting location
        final IMapController mapController = map.getController();
        mapController.setCenter(source);
        mapController.setZoom(17.0);

        TextView routeLengthDisplay = (TextView) findViewById((R.id.routeLengthDisplay));
        routeLengthDisplay.setText("Your Route Length is " + routeLength + "km");

        //Centers the map on the users location when pressed
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GeoPoint currentLocation = lc.getCurrentLocation();
                mapController.setCenter(currentLocation);
            }//end onClick()
        });//end setOnClickListener()

        //Returns the user to the previous screen when pressed
        final FloatingActionButton backButton = (FloatingActionButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }//end onClick
        });//end backButton

        //Stopwatch for the run
        mChrono = (Chronometer) findViewById(R.id.chronometer);

        //When pressed the screen will zoom in to the users location and a run can be initiated
        //It will follow the user as they run
        final Button toggleRunButton = findViewById(R.id.toggle_run);
        toggleRunButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (toggleRunButton.getText().equals("Start Run")) {
                    mapController.setZoom(18.0);
                    //Center on the users location if available, else on first node in route
                    if (source.getLatitude() != 53.3498 && source.getLongitude() != -6.2603) {
                        lc.setFollow(true);
                    } else {
                        mapController.setCenter(new GeoPoint((Double.parseDouble(edges[0].getSourceNode().getLat())),
                                Double.parseDouble(edges[0].getSourceNode().getLon())));
                    }//end if else
                    toggleRunButton.setText("Stop Run");

                    backButton.setVisibility(TextView.GONE);

                    mChrono.setBase(SystemClock.elapsedRealtime());
                    timeWhenStopped = 0;
                    mChrono.start();
                } else {
                    mapController.setZoom(16.0);
                    lc.setFollow(false);
                    toggleRunButton.setText("Start Run");

                    backButton.setVisibility(TextView.VISIBLE);

                    timeWhenStopped = mChrono.getBase() - SystemClock.elapsedRealtime();

                    mChrono.stop();
                    mChrono.setBase(SystemClock.elapsedRealtime());
                }//end if else
            }//end onClick()
        });//end setOnClickListener()

        constructRoute(edges);
    }//end onCreate

    @Override
    protected void onStart() {
        lc.setCurrentLocation();
        super.onStart();
        //new HttpGraphRequestTask().execute("GetGraph");
    }//end onStart()

    //Construct a polyline of the route to be displayed to the user
    public void constructRoute(OSMEdge[] edges) {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();

        OSMNode source = edges[0].getSourceNode();
        waypoints.add(new GeoPoint(Double.parseDouble(source.getLat()),
                Double.parseDouble(source.getLon())));
        for (int i = 0; i < edges.length; i++) {
            Log.i("Edge " + i + ": ", edges[i].toString());
            OSMNode target = edges[i].getTargetNode();
            waypoints.add(new GeoPoint(Double.parseDouble(target.getLat()),
                    Double.parseDouble(target.getLon())));
        }//end for

        BoundingBox bb = map.getBoundingBox();
        System.out.println(bb);

        //Simple marker for the starting node of the route
        Marker startMarker = new Marker(map);
        startMarker.setPosition(waypoints.get(0));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        startMarker.setTitle("Start point");

        map.invalidate();

        //The route as a polyline
        Polyline route = new Polyline();
        route.setPoints(waypoints);
        map.getOverlayManager().add(route);

        //new UpdateRoadTask().execute(waypoints);
    }//End constructRoute()

    //*LEGACY* I used to make a request to a Road Manager to construct a polyline of the route for me.
    //This was because the road manager would 'snap' the route to the roads,
    //however it would fail as much as worked, possibly due to the fact that
    //I am not only using roads, but footpaths as well
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
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

            map.getOverlay().clear();
            map.getOverlays().add(roadOverlay);
            map.invalidate();

            //Adding visible icons for each node in route
            /*
            Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
            for (int i=0; i<road.mNodes.size(); i++){
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step "+i);
                nodeMarker.setSnippet(node.mInstructions);
                nodeMarker.setSubDescription(Road.getLengthDurationText(MapActivity.this, node.mLength, node.mDuration));
                map.getOverlays().add(nodeMarker);
            }//End for
            */
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
        lc.stopLocationServices();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }//End onStop()
}//End MapActivity
