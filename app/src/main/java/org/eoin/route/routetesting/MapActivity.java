package org.eoin.route.routetesting;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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

public class MapActivity extends AppCompatActivity {

    MapView map;
    LocationController lc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        GeoPoint source = (GeoPoint) intent.getSerializableExtra("sourceLocation");
        OSMEdge[] edges = (OSMEdge[]) intent.getSerializableExtra("edges");
        BoundingBox dublin = (BoundingBox) intent.getSerializableExtra("dublin");
        System.out.println("Source: " + source);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setScrollableAreaLimitDouble(dublin);

        map.setMaxZoomLevel(17.0);
        map.setMinZoomLevel(13.0);

        //Enables zoom
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        this.lc = new LocationController(this, ctx, map, source);
        lc.addOverlays();

        //Sets the inital zoom level and starting location
        final IMapController mapController = map.getController();
        mapController.setCenter(source);
        mapController.setZoom(17.0);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Focusing on Location", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                GeoPoint currentLocation = lc.getCurrentLocation();
                mapController.setCenter(currentLocation);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        constructRoute(edges);
    }

    @Override
    protected void onStart() {
        lc.setCurrentLocation();
        super.onStart();
        //new HttpGraphRequestTask().execute("GetGraph");
    }

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
        }

        BoundingBox bb = map.getBoundingBox();
        System.out.println(bb);

        //Simple marker for the starting node of the route
        Marker startMarker = new Marker(map);
        startMarker.setPosition(waypoints.get(0));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startMarker);
        startMarker.setTitle("Start point");

        map.invalidate();

        new UpdateRoadTask().execute(waypoints);
    }

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
            Toast.makeText(MapActivity.this, "distance="+road.mLength, Toast.LENGTH_SHORT).show();
            Toast.makeText(MapActivity.this, "duration="+road.mDuration, Toast.LENGTH_SHORT).show();

            if(road.mStatus != Road.STATUS_OK)
                Toast.makeText(MapActivity.this, "Error when loading the road - status="+road.mStatus, Toast.LENGTH_SHORT).show();
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

            map.getOverlay().clear();
            map.getOverlays().add(roadOverlay);
            map.invalidate();

            //Adding visible icons for each node in route
            //Being able to handle these nodes is very important.
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
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }//End onStop()
}
