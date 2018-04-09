package org.eoin.route.routetesting;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //Map view
    MapView map;
    LocationController lc;
    GeoPoint startLocation =  new GeoPoint(0,0);

    BoundingBox dublin = new BoundingBox(53.4766, -5.9924, 53.2295, -6.6900);

    private ProgressDialog progressDialog;

    String routeChoice = "GetBestGraph";

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
        map.setScrollableAreaLimitDouble(dublin);
        //map.zoomToBoundingBox(dublin, true, 5);

        this.lc = new LocationController(this, ctx, map);
        lc.addOverlays();

        System.out.println(map.getMaxZoomLevel() + " / " + map.getMinZoomLevel());
        map.setMaxZoomLevel(17.0);
        map.setMinZoomLevel(13.0);

        //Sets the inital zoom level and starting location
        IMapController mapController = map.getController();
        if (lc.getCurrentLocation() != null) {
            map.setMultiTouchControls(false);
            map.setBuiltInZoomControls(false);
            startLocation = new GeoPoint(lc.getCurrentLocation());
            mapController.setCenter(startLocation);
            mapController.setZoom(16.0);

            TextView grabbingLocation = (TextView) findViewById(R.id.grabbing_location);
            grabbingLocation.setText("Location Found!");
            grabbingLocation.setVisibility(TextView.GONE);

        } else {
            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);
            mapController.setCenter(new GeoPoint(53.3498, -6.2603));
            mapController.setZoom(13.0);

            map.zoomToBoundingBox(dublin, true, 5);

            ConstraintLayout generateRouteUI = (ConstraintLayout) findViewById(R.id.generateRouteUI);
            generateRouteUI.setVisibility(ConstraintLayout.GONE);
        }

        double startLat = startLocation.getLatitude();
        double startLon = startLocation.getLongitude();

        TextView currentLocation = (TextView) findViewById((R.id.currentLocationTV));
        currentLocation.setText(startLat + ", " + startLon);

        final Button generateRouteButton = findViewById(R.id.generateRouteButton);
        generateRouteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                BoundingBox bb = map.getProjection().getBoundingBox();
                System.out.println("Bounding Box: " + bb);

                EditText routeLengthET = (EditText) findViewById(R.id.routeLengthET);
                String routeLength = routeLengthET.getText().toString();

                if (routeLength.equals("") || Double.parseDouble(routeLength) <= 1.99) {
                    Toast.makeText(MainActivity.this, "Distance must be at least 2km", Toast.LENGTH_SHORT).show();
                } else {
                    String reqDistance =  "reqDistance=" + routeLength;
                    String sourceLat = "sourceLat=" + String.valueOf(startLocation.getLatitude());
                    String sourceLon =  "sourceLon=" + String.valueOf(startLocation.getLongitude());
                    String x1 = "x1=" + bb.getLatSouth();
                    String y1 = "y1=" + bb.getLonWest();
                    String x2 = "x2=" + bb.getLatNorth();
                    String y2 = "y2=" + bb.getLonEast();

                    new HttpGraphRequestTask().execute(routeChoice, reqDistance, sourceLat, sourceLon, x1, y1, x2, y2);
                }
            }
        });
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_quick:
                if (checked) {
                    routeChoice = "GetQuickGraph";
                    Log.i("routeChoice", routeChoice);
                    break;
                }//end if
            case R.id.radio_optimal:
                if (checked) {
                    routeChoice = "GetBestGraph";
                    Log.i("routeChoice", routeChoice);
                    break;
                }//end if
        }//end switch
    }//end onRadioButtonClicked

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
            BoundingBox bb = map.getProjection().getBoundingBox();
            System.out.println("Bounding Box: " + bb);

            String sourceLat = "sourceLat=" + String.valueOf(latitude);
            String sourceLon =  "sourceLon=" + String.valueOf(longitude);
            String reqDistance =  "reqDistance=" + "3";
            String x1 = "x1=" + bb.getLatSouth();
            String y1 = "y1=" + bb.getLonWest();
            String x2 = "x2=" + bb.getLatNorth();
            String y2 = "y2=" + bb.getLonEast();

            new HttpGraphRequestTask().execute("GetBestGraph", reqDistance, sourceLat, sourceLon, x1, y1, x2, y2);
            return true;
        }
        if (id == R.id.action_refreshLocation) {
            lc.setCurrentLocation();
            IMapController mapController = map.getController();
            if (lc.getCurrentLocation() != null) {
                Log.i("Refresh", "succeeded");
                startLocation = new GeoPoint(lc.getCurrentLocation());
                mapController.setCenter(startLocation);
                mapController.setZoom(17.0);
            } else {
                Log.i("Refresh", "failed");
                map.setBuiltInZoomControls(true);
                map.setMultiTouchControls(true);
            }
            System.out.println(startLocation);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //new HttpGraphRequestTask().execute("GetGraph");
    }

    private void launchMapActivity(Route route) {
        ArrayList<OSMEdge> routeEdges = route.getRoute();
        OSMEdge[] edges = routeEdges.toArray(new OSMEdge[routeEdges.size()]);
        double routeLength = route.getWeight();
        Intent mapIntent = new Intent(this, MapActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable("sourceLocation", lc.getCurrentLocation());
        extras.putSerializable("edges", edges);
        extras.putDouble("routeLength", routeLength);
        extras.putSerializable("dublin", dublin);

        mapIntent.putExtras(extras);
        startActivity(mapIntent);
    }

    private class HttpGraphRequestTask extends AsyncTask<String, Integer, Route> {

        public HttpGraphRequestTask thisAsyncTask;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            thisAsyncTask = this;

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Your route is being generated!");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();

            new CountDownTimer(270000, 1000) {
                public void onTick(long millisUntilFinished) {
                    Log.i("Time Left", millisUntilFinished / 1000 + " seconds left");
                    if (millisUntilFinished / 1000 == 180) {
                        progressDialog.setMessage("This sure is taking some time!");
                    } else if (millisUntilFinished / 1000 == 60) {
                        progressDialog.setCancelable(true);
                        progressDialog.setMessage("Its working hard behind the scenes I swear!");
                    }
                }
                public void onFinish() {
                    if (thisAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                        //thisAsyncTask.cancel(false);

                        progressDialog.dismiss();
                    }
                }
            }.start();
        }

        @Override
        protected Route doInBackground(String... endpoint) {
            try {
                String url = "http://46.101.77.71:8080/drfr-backend/";
                if (endpoint[0].equals("GetQuickGraph") || endpoint[0].equals("GetBestGraph")){
                    url = url + endpoint[0] + "?";
                    for (int i = 1; i < endpoint.length; i++) {
                        url = url + endpoint[i];
                        if (i == endpoint.length - 1) {
                            Log.i("GET", url);
                        } else {
                            url = url + "&";
                        }
                    }
                } else {
                    url = url + endpoint[0];
                }
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                ResponseEntity<Route> responseEntity = restTemplate.getForEntity(url, Route.class);

                Route route =  responseEntity.getBody();
                Log.i("edges", "" + route.getRoute().size());
                Log.i("length", "" + route.getWeight());

                return route;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Route route) {
            progressDialog.dismiss();
            launchMapActivity(route);
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
        lc.stopLocationServices();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }//End onStop()
}
