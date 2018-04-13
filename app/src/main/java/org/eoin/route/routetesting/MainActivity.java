package org.eoin.route.routetesting;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.testfairy.TestFairy;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;

/*
The Main Activity of the application. This Activity allows the user to input their
desired parameters and request for a route to be generated
 */

public class MainActivity extends AppCompatActivity {

    //Map view
    MapView map;
    LocationController lc;
    GeoPoint startLocation =  new GeoPoint(53.3498, -6.2603);
    String lastLat, lastLon;
    BoundingBox dublin = new BoundingBox(53.4766, -5.9924, 53.2295, -6.6900);
    private ProgressDialog progressDialog;
    String routeChoice = "GetBestGraph";
    boolean includeResidentialTags = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Used for User Testing, disabled in final build
        //TestFairy.begin(this, "0f9247e0759acc42c24772ab9afc0a23f6c0163a");

        Context ctx = getApplicationContext();
        //important! Setting user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        //Setting up the MapView and limiting the user from scrolling outside of the project scope
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setScrollableAreaLimitDouble(dublin);

        //Setting min and max zoom levels
        map.setMaxZoomLevel(17.0);
        map.setMinZoomLevel(13.0);

        this.lc = new LocationController(this, ctx, map);
        lc.addOverlays();

        final IMapController mapController = map.getController();
        initialMapSetup(mapController);
        setupSubmitButton(mapController);
    }//End onCreate

    //Setting up the map, depending on if the application can grab the users location
    public void initialMapSetup(final IMapController mapController) {

        //Centers the map on the users location when pressed
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GeoPoint currentLocation = lc.getCurrentLocation();
                mapController.setCenter(currentLocation);
            }
        });// End centerLocationButton

        //If able to get location
        if (lc.getCurrentLocation() != null) {
            map.setMultiTouchControls(false);
            map.setBuiltInZoomControls(false);
            startLocation = new GeoPoint(lc.getCurrentLocation());
            mapController.setCenter(startLocation);
            mapController.setZoom(16.0);

            EditText editTextLat = (EditText) findViewById((R.id.editTextLat));
            EditText editTextLon = (EditText) findViewById((R.id.editTextLon));
            editTextLat.setEnabled(false);
            editTextLon.setEnabled(false);

            fab.setVisibility(TextView.VISIBLE);

            TextView grabbingLocation = (TextView) findViewById(R.id.grabbing_location);
            grabbingLocation.setText("Location Found!");
            grabbingLocation.setVisibility(TextView.GONE);

        //If Unable to get location
        } else {
            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);
            mapController.setCenter(new GeoPoint(53.3498, -6.2603));
            mapController.setZoom(13.0);

            ToggleButton locationToggle = (ToggleButton) findViewById(R.id.toggleDVL);
            locationToggle.setChecked(false);

            fab.setVisibility(TextView.GONE);

            map.zoomToBoundingBox(dublin, true, 5);
        }//end if else
    }//End initalMapSetup()

    //For setting up map interaction on user press.
    //Initially intended for the user to pick their start location by finding it on the map.
    //However it was breaking other listeners for the map, such as zoom, so it was disabled
    public void setUpMapTouch() {
        map.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Projection proj = map.getProjection();
                GeoPoint p = (GeoPoint) proj.fromPixels((int) arg1.getX(), (int) arg1.getY());
                Location location = new Location("");
                location.setLatitude((double) p.getLatitudeE6() / 1000000);
                location.setLongitude((double) p.getLongitudeE6() / 1000000);
                if(arg1.getAction() ==  MotionEvent.ACTION_UP)
                {
                    Log.i("Touch", "" + location);
                    return true;
                } else {
                    return true;
                }// end if else
            }
        });//end map onTouchListener
    }//End setUpMapTouch()

    //Setting up the UI for when the user presses the GENERATE ROUTE button
    public void setupSubmitButton(final IMapController mapController) {
        double startLat = startLocation.getLatitude();
        double startLon = startLocation.getLongitude();

        final EditText editTextLat = (EditText) findViewById((R.id.editTextLat));
        final EditText editTextLon = (EditText) findViewById((R.id.editTextLon));
        editTextLat.setText(String.valueOf(startLat));
        editTextLon.setText(String.valueOf(startLon));

        lastLat = String.valueOf(startLat);
        lastLon = String.valueOf(startLon);

        //Toggle button for if the user wants to use their device's location or their own
        //inputted location. Disables the EditText for entering in custom coordinates if
        //using the device location
        ToggleButton locationToggle = (ToggleButton) findViewById(R.id.toggleDVL);
        locationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (isChecked) {
                    // The toggle is enabled
                    fab.setVisibility(TextView.VISIBLE);
                    editTextLat.setEnabled(false);
                    editTextLon.setEnabled(false);
                    if (lc.getCurrentLocation() != null) {
                        editTextLat.setText(String.valueOf(lc.getCurrentLocation().getLatitude()));
                        editTextLon.setText(String.valueOf(lc.getCurrentLocation().getLongitude()));
                        mapController.setCenter(lc.getCurrentLocation());

                    } else {
                        editTextLat.setText(String.valueOf(53.3498));
                        editTextLon.setText(String.valueOf(-6.2603));
                    }//end inner if else
                } else {
                    // The toggle is disabled
                    editTextLat.setEnabled(true);
                    editTextLon.setEnabled(true);

                    fab.setVisibility(TextView.GONE);
                }//end if else
            }
        });// end locationToggle onCheckedChangedListener()

        //Toggle for the user to decide if they want to include residential roads
        ToggleButton residentialToggle = (ToggleButton) findViewById(R.id.toggleResidential);
        residentialToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    includeResidentialTags = true;
                } else {
                    // The toggle is disabled
                    includeResidentialTags = false;
                }//end if else
            }
        });//end residentialToggle setOnCheckedChangeListener

        //Adding a listener to change to the correct zoom level depending on route length input
        //And to display a message to the user if it is an unusable route length
        final EditText routeLengthET = (EditText) findViewById(R.id.routeLengthET);
        routeLengthET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String routeLength = editable.toString();
                if (routeLength.equals("")) {
                    Toast.makeText(MainActivity.this, "Distance must be at least 2km", Toast.LENGTH_SHORT).show();
                } else {
                    if (Double.parseDouble(routeLength) > 8.5 && Double.parseDouble(routeLength) < 12.01) {
                        mapController.setZoom(15.0);
                        Toast.makeText(MainActivity.this, "Routes of this length may take a long time to generate", Toast.LENGTH_SHORT).show();
                    } else if (Double.parseDouble(routeLength) > 5.0 && Double.parseDouble(routeLength) <= 8.5) {
                        mapController.setZoom(15.5);
                        Toast.makeText(MainActivity.this, "Routes of this length may take a long time to generate", Toast.LENGTH_SHORT).show();
                    } else if (Double.parseDouble(routeLength) <= 5.0) {
                        mapController.setZoom(16.0);
                    }//end inner if else
                }//end if else
            }//end afterTextChanged
        });//end routeLengthET TextChangedListener

        //On button press a request will be sent to the server to generate a route
        final Button generateRouteButton = findViewById(R.id.generateRouteButton);
        generateRouteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String latStart = editTextLat.getText().toString();
                String lonStart = editTextLon.getText().toString();
                String routeLength = routeLengthET.getText().toString();

                startLocation = new GeoPoint(Double.parseDouble(latStart), Double.parseDouble(lonStart));

                //Centers at the user's location to get the correct bounding box to send to the server
                mapController.setCenter(startLocation);
                BoundingBox bb = map.getProjection().getBoundingBox();
                System.out.println("Bounding Box: " + bb);

                //Check to ensure the route is within the correct length range
                if (latStart.equals("") || Double.parseDouble(latStart) <= 53.2295 || Double.parseDouble(latStart) >= 53.4788) {
                    Toast.makeText(MainActivity.this, "Latitude must be between 53.2295 and 53.4766", Toast.LENGTH_SHORT).show();
                } else  if (lonStart.equals("") || Double.parseDouble(lonStart) <= -6.6900 || Double.parseDouble(lonStart) >= -5.9924) {
                    Toast.makeText(MainActivity.this, "Longitude must be between -6.6900 and -5.9924", Toast.LENGTH_SHORT).show();
                } else {
                    if (routeLength.equals("") || Double.parseDouble(routeLength) <= 1.99) {
                        Toast.makeText(MainActivity.this, "Distance must be at least 2km", Toast.LENGTH_SHORT).show();
                    } else if (Double.parseDouble(routeLength) >= 12.01) {
                        Toast.makeText(MainActivity.this, "Distance must be less than 12km", Toast.LENGTH_SHORT).show();
                    } else {

                        //construct data to send in the GET request
                        String reqDistance = "reqDistance=" + routeLength;
                        String sourceLat = "sourceLat=" + latStart;
                        String sourceLon = "sourceLon=" + lonStart;
                        String x1 = "x1=" + bb.getLatSouth();
                        String y1 = "y1=" + bb.getLonWest();
                        String x2 = "x2=" + bb.getLatNorth();
                        String y2 = "y2=" + bb.getLonEast();
                        String includeResidential = "includeResidential=" + includeResidentialTags;

                        new HttpGraphRequestTask().execute(routeChoice, reqDistance, sourceLat, sourceLon, x1, y1, x2, y2, includeResidential);
                    }//end inner if else
                }//end if else
            }
        });//end generateRouteButton onClick Listener
    }//end setupSubmitButton()

    //Checks which routing choice was selected
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

    //Used to create the menu in the top right corner
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }//End onCreateOptionsMenu()

    //Handler for the menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Legacy selection for Approach 2.
        if (id == R.id.action_simpleLoop) {
            map.getOverlays().clear();
            map.invalidate();

            BoundingBox bb = map.getProjection().getBoundingBox();
            System.out.println("Bounding Box: " + bb);

            final EditText editTextLat = (EditText) findViewById((R.id.editTextLat));
            final EditText editTextLon = (EditText) findViewById((R.id.editTextLon));

            String latStart = editTextLat.getText().toString();
            String lonStart = editTextLon.getText().toString();

            final EditText routeLengthET = (EditText) findViewById(R.id.routeLengthET);
            String sourceLat = "sourceLat=" + latStart;
            String sourceLon =  "sourceLon=" + lonStart;
            String reqDistance =  "reqDistance=" + routeLengthET.getText().toString();
            String x1 = "x1=" + bb.getLatSouth();
            String y1 = "y1=" + bb.getLonWest();
            String x2 = "x2=" + bb.getLatNorth();
            String y2 = "y2=" + bb.getLonEast();

            new HttpGraphRequestTask().execute("GetSimpleGraph", reqDistance, sourceLat, sourceLon, x1, y1, x2, y2);
            return true;
        }//end if
        return super.onOptionsItemSelected(item);
    }//end onOptionsItemSelected()

    @Override
    protected void onStart() {
        super.onStart();

        //new HttpGraphRequestTask().execute("GetGraph");
    }//end onStart()

    //The generated route is passed to this method,
    //which will send the returned information to the MapActivity to display it to the user
    private void launchMapActivity(Route route) {
        ArrayList<OSMEdge> routeEdges = route.getRoute();
        OSMEdge[] edges = routeEdges.toArray(new OSMEdge[routeEdges.size()]);
        double routeLength = route.getWeight();
        Intent mapIntent = new Intent(this, MapActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable("sourceLocation", startLocation);
        extras.putSerializable("edges", edges);
        extras.putDouble("routeLength", routeLength);
        extras.putSerializable("dublin", dublin);

        mapIntent.putExtras(extras);
        startActivity(mapIntent);
    }//End launchMapActivity()

    //Asynchronously sends a GET request to the server for a route
    private class HttpGraphRequestTask extends AsyncTask<String, Integer, Route> {

        public HttpGraphRequestTask thisAsyncTask;

        //This method happens before the GET request.
        // It sets up a progress dialog to display while the user is waiting for their route
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
                    }//end if else
                }//end onTick()
                public void onFinish() {
                    if (thisAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                        //thisAsyncTask.cancel(false);

                        progressDialog.dismiss();
                    }//end if
                }//end onFinsh()
            }.start();
        }//end onPreExecute()

        //This GET request happens in the background so as not to lock the main thread
        @Override
        protected Route doInBackground(String... endpoint) {
            try {
                String url = "http://46.101.77.71:8080/drfr-backend/";
                if (endpoint[0].equals("GetQuickGraph") ||
                        endpoint[0].equals("GetBestGraph")||
                        endpoint[0].equals("GetSimpleGraph")){
                    url = url + endpoint[0] + "?";
                    //Constructing the GET request
                    for (int i = 1; i < endpoint.length; i++) {
                        url = url + endpoint[i];
                        if (i == endpoint.length - 1) {
                            Log.i("GET", url);
                        } else {
                            url = url + "&";
                        }//end if else
                    }//end for
                } else {
                    url = url + endpoint[0];
                }//end if else

                //Deserializing the returned route from its JSON format
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                ResponseEntity<Route> responseEntity = restTemplate.getForEntity(url, Route.class);

                Route route =  responseEntity.getBody();
                Log.i("edges", "" + route.getRoute().size());
                Log.i("length", "" + route.getWeight());

                return route;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }//end try catch

            return null;
        }//end doInBackground

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        //This method happens after the GET request has returned something
        @Override
        protected void onPostExecute(Route route) {
            if (route != null) {
                progressDialog.setProgress(100);
                progressDialog.dismiss();
                progressDialog.cancel();
                launchMapActivity(route);
            } else {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Unable to Generate Route. Please try again with different parameters", Toast.LENGTH_LONG).show();
            }//end if else
        }//end onPostExecute()
    }//End HTTPGraphRequestTask

    public void onResume(){
        super.onResume();
        //lc.setCurrentLocation();
        //this will refresh the osmdroid configuration on resuming.
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
}//End MainActivity
