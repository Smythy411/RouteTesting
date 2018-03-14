package org.eoin.route.routetesting;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class MainActivity extends AppCompatActivity {

    //Map view
    MapView map;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        new HttpGraphRequestTask().execute();
    }

    private class HttpGraphRequestTask extends AsyncTask<Void, Void, OSMEdge[]> {
        @Override
        protected OSMEdge[] doInBackground(Void... params) {
            try {

                final String url = "http://46.101.77.71:8080/gs-rest-service-initial/GetGraph";
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
            for (int i = 0; i < edges.length; i++) {
                Log.i("Edge " + i + ": ", edges[i].toString());
            }

            OSMNode source = edges[0].getSourceNode();
            OSMNode target = edges[0].getTargetNode();

            GeoPoint startPoint = new GeoPoint(Double.parseDouble(source.getLat()),
                    Double.parseDouble(target.getLon()));

            TextView nodeIdText = (TextView) findViewById(R.id.node_value);
            nodeIdText.setText(Long.toString(source.getNodeID()));

            //Sets the inital zoom level and starting location
            IMapController mapController = map.getController();
            mapController.setZoom(17);
            mapController.setCenter(startPoint);

            //Simple marker for the starting node of the route
            Marker startMarker = new Marker(map);
            startMarker.setPosition(startPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(startMarker);
            startMarker.setTitle("Start point");
        }
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }//End onResume()

    protected void onStop() {
        super.onStop();
    }//End onStop()
}
