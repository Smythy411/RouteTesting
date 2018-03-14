package org.eoin.route.routetesting;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

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
        new HttpRequestTask().execute();
        new HttpGraphRequestTask().execute();
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, OSMNode> {
        @Override
        protected OSMNode doInBackground(Void... params) {
            try {

                final String url2 = "http://46.101.77.71:8080/gs-rest-service-initial/OSMNode";
                RestTemplate restTemplate2 = new RestTemplate();
                restTemplate2.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                OSMNode node = restTemplate2.getForObject(url2, OSMNode.class);

                return node;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(OSMNode node) {
            TextView nodeIdText = (TextView) findViewById(R.id.node_value);

            nodeIdText.setText(Long.toString(node.getNodeID()));
        }
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
                Log.i("Egde " + i + ": ", edges[i].toString());
            }
        }
    }
}
