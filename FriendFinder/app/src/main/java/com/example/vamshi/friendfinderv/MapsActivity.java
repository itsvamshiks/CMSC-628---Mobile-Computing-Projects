package com.example.vamshi.friendfinderv;

import android.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener {

    private GoogleMap mMap;
    private String BASE_URL = "", user_email;
    private LatLng currentLocation;
    private Handler myHandler;
    private LocationManager locationManager;
    private Button btn_Logout;
    private Context currContext=null;
    private Timer locTimer=null;
    private Boolean isLocationUpdated=false,flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        BASE_URL = getString(R.string.baseurl);
        btn_Logout = (Button) findViewById(R.id.btn_logout);
        currContext = this;
        Intent i = getIntent();
        user_email = i.getStringExtra("email");
        myHandler = new Handler();
        init();

    }

    private void init() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        btn_Logout.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(currContext);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnownLocation!=null){
             currentLocation = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
        }else{
            currentLocation= new LatLng(39.2538094,-76.714081);
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);
        startLocationTimer();
    }

    private void startLocationTimer(){
        if(locTimer==null){
            locTimer= new Timer();
            TimerTask checkForFriends = new TimerTask() {
                @Override
                public void run() {
                    if(isLocationUpdated){
                        Log.d("DEBUG","CHECKING FOR FRIENDS");
                        String []userDet = new String[3];
                        userDet[0] = user_email;
                        synchronized (currentLocation){
                            userDet[1] = String.valueOf(currentLocation.latitude);
                            userDet[2] = String.valueOf(currentLocation.longitude);
                            new LocationUpdateTask().execute(userDet);
                            new FinderTask().execute(userDet);
                        }
                      }
                }
            };
            locTimer.scheduleAtFixedRate(checkForFriends,20000,60000);
        }

    }

    private void stopLocationTimer(){
        if(locTimer!=null){
            locTimer.cancel();
            locTimer.purge();
            locTimer = null;
            Log.d("DEBUG","Stopping Location Timer");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        stopLocationTimer();
        isLocationUpdated = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        stopLocationTimer();
        isLocationUpdated = false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /*if (currentLocation != null) {


            Log.d("RESPONSE", "currentLocation got a value");
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        } */
    }

    @Override
    public void onLocationChanged(Location location) {

          synchronized (currentLocation){
              isLocationUpdated = true;
              currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
          }

          /*  String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            Log.d("RESPONSE","getting location and setting currentLocation");
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            String[] data = new String[3];
            data[0] = user_email;
            data[1] = latitude;
            data[2] = longitude;*/


    }

    private class FinderTask extends AsyncTask<String,Integer,List<users>>{

        @Override
        protected List<users> doInBackground(String... strings) {
           URL url;
           String response="";
           String REQUEST_URL = BASE_URL+"locateFriends.php";
           List<users> friends_nearby = new ArrayList<>();
           try{
               url = new URL(REQUEST_URL);

               HttpURLConnection conn = (HttpURLConnection) url.openConnection();
               conn.setReadTimeout(15000);
               conn.setConnectTimeout(15000);
               conn.setRequestMethod("POST");
               conn.setDoInput(true);
               conn.setDoOutput(true);



               OutputStream os = conn.getOutputStream();
               BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
               String requestJSONString = new JSONObject().put("email",strings[0])
                                          .put("latitude",strings[1])
                                          .put("longitude",strings[2]).toString();

               Log.d("RESPONSE - f",requestJSONString);
               bwriter.write(requestJSONString);
               bwriter.flush();
               bwriter.close();
               os.close();

               int responseCode = conn.getResponseCode();
               Log.d("RESPONSE CODE fet",String.valueOf(responseCode));

               if(responseCode == HttpsURLConnection.HTTP_OK){
                   String line;
                   Log.d("RESPONSE CODE","CODE OK");
                   BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                   line = br.readLine();
                   while(line!=null){
                       response+=line;
                       line = br.readLine();
                       Log.d("RESPONSE LADDED",response);
                   }
                   br.close();
               }


               Log.d("RESPONSE BODY","RESPONSE is: "+response);
               JSONArray friendsArray = new JSONArray(response);
               Log.d("--RESPONSE JSONARRAY--",friendsArray.toString());

               for(int i = 0;i<friendsArray.length();i++) {

                   JSONObject jsonObject = friendsArray.getJSONObject(i);
                   Log.d("RESPONSE SINGLEJSON",jsonObject.toString());
                   users user = new users(jsonObject.getString("email"),
                           jsonObject.getString("full_name"),
                           jsonObject.getDouble("latitude"),
                           jsonObject.getDouble("longitude"),
                           jsonObject.getString("last_active_time"));

                   Log.d("RESPONSE JSON_ADDED", user.full_name.toString());

                   friends_nearby.add(user);


               }


               conn.disconnect();

           }
           catch(JSONException | IOException e){
                e.printStackTrace();
           }
           Log.d("RESPONSE RETURNED",response);
           Log.d("RESPONSE ARR_RETURN",friends_nearby.toString());
           return friends_nearby;
        }

        @Override
        protected void onPostExecute(List<users> users_nearby) {
            super.onPostExecute(users_nearby);
            mMap.clear();
            LatLng myLocation,friendLocation;
            for(users friends : users_nearby){
                Log.d("Locating Friend",friends.getFull_name());
                friendLocation = new LatLng(friends.getLatitude(),friends.getLongitude());
                if(mMap!=null){
                    Log.d("Locating","Locating "+friends.getFull_name());
                    mMap.addMarker(new MarkerOptions().position(friendLocation).title(friends.getFull_name()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))).showInfoWindow();



                }
            }

            myLocation = currentLocation;
            if(myLocation!=null && myLocation.latitude!=0){
                mMap.addMarker(new MarkerOptions().position(myLocation).title("my location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))).showInfoWindow();
                if(!flag){
                    CameraPosition cp = new CameraPosition.Builder().target(myLocation).zoom(15).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
                }

            }

        }
    }

    private class LocationUpdateTask extends AsyncTask<String,Integer,String>{

        @Override
        protected String doInBackground(String... strings) {
            URL url;
            String response = "";
            String REQUEST_URL = BASE_URL + "updateUserLoc.php";
            Log.d("RESPONSE URL",REQUEST_URL);
            try{
                url = new URL(REQUEST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
                String requestJSONString = new JSONObject().put("email",strings[0])
                                                           .put("latitude",strings[1])
                                                           .put("longitude",strings[2])
                                                           .toString();
                Log.d("RESPONSE USERLOC",requestJSONString);
                bufferedWriter.write(requestJSONString);
                bufferedWriter.flush();
                bufferedWriter.close();
                os.close();
                int responseCode = conn.getResponseCode();
                Log.d("RESPONSE CODE LOC",String.valueOf(responseCode));

                if(responseCode==HttpsURLConnection.HTTP_OK){
                    Log.d("REPONSE CODE LOCOK","LOC RESPONSE CODE OK");
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    line = br.readLine();
                    while (line!=null){
                        response+=line;
                        line = br.readLine();
                    }

                    br.close();
                }
                else{
                    response = "Error updating response for updating user loc";
                }
                conn.disconnect();


            }catch (IOException | JSONException e){
                e.printStackTrace();
            }
            Log.d("RESPONSE USERLOC FINAL",response);
            return response;
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


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_logout:
                SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(currContext);
                spref.edit().remove("name").commit();
                spref.edit().remove("isSignedIN").commit();
                spref.edit().remove("emailID").commit();
                MapsActivity.this.finish();
                break;
        }

    }


    public class users {
        private String email;
        private String full_name;
        private Double latitude;
        private Double longitude;
        private String last_active_time;
        users(String email, String full_name, Double latitude, Double longitude, String last_active_time) {
            this.email = email;
            this.full_name = full_name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.last_active_time = last_active_time;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFull_name() {
            return full_name;
        }

        public void setFull_name(String full_name) {
            this.full_name = full_name;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getLast_active_time() {
            return last_active_time;
        }

        public void setLast_active_time(String last_active_time) {
            this.last_active_time = last_active_time;
        }
    }
}


