package com.example.vamshi.cmsc628_project1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView latitudetxt;
    private TextView longitudetxt;
    private TextView latitude2txt;
    private TextView longitude2txt;
    private TextView providertxt, distancetxt;
    private LocationManager locationManager_;
    private SensorManager accManager, magManager;
    private Sensor accelerometer, magnetometer;
    private Handler myhandler = new Handler();
    private Timer t = new Timer();
    double secndLat, secondLong;
    int a = 0, it = 1;
    private double t1, t2, dist = 0;
    private float[] gravity = {0, 0, 0};
    boolean stopM2 = false;
    private boolean isGPS = false, isNetwork = false;
    private Location gpsLoc = null;
    private Double flat,flong;
    private int Angle;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private float[] AccelerometerReadings = new float[3];
    private float[] MagnetometerReadings = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager_ = (LocationManager) getSystemService(LOCATION_SERVICE);
        accManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = accManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = magManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        accManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        magManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        latitudetxt = findViewById(R.id.Latitude);
        longitudetxt = findViewById(R.id.Longitude);
        latitude2txt = findViewById(R.id.Latitude2);
        longitude2txt = findViewById(R.id.Longitude2);
        providertxt = findViewById(R.id.Provider);
        distancetxt = findViewById(R.id.Distance);

    }


    @Override
    protected void onResume() {
        super.onResume();
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

        isGPS = locationManager_.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetwork = locationManager_.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        if (isGPS) {
            Log.d("Testing", "Entered GPS condition");
            locationManager_.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSListener);


            //Toast.makeText(getApplicationContext(), "Location by GPS first", Toast.LENGTH_SHORT).show();
        }

        if (isNetwork) {
            Log.d("Testing", "Entered Network Condition");
            locationManager_.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, NetworkListener);


            //Toast.makeText(getApplicationContext(), "Location by network first", Toast.LENGTH_SHORT).show();
        }


        if (!isGPS && !isNetwork) {
            Toast.makeText(getApplicationContext(), "No Provider Available", Toast.LENGTH_LONG);
        }
        Log.d("Testing", "Now going to timer");
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                myhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Testing","#####checking GPS availability#####");
                        checkGPS();
                        if (a == 0) {

                            requestGPS();
                        } else {
                            requestNetwork();
                        }
                    }
                });
            }
        },0,2000);



    }

    public void requestGPS() {

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
        Log.d("Testing","Getting GPS");
        locationManager_.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSListener);
    }

    public void requestNetwork() {
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
        Log.d("Testing","Getting Network");
        locationManager_.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, NetworkListener);
    }

    public void checkGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return ;
        }
        gpsLoc = locationManager_.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(gpsLoc == null){
            Log.d("Testing","$$$$$$Setting a to 0");
            a=0;
        }
        else{
            Log.d("Testing","$$$$$$Setting a to 1");

            a=1;
        }

    }

    LocationListener GPSListener = new LocationListener() {


        @Override
        public void onLocationChanged(Location location) {


            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            myhandler.post(new LocationWork(latitude, longitude, "GPS"));
            if(!stopM2){
                setCoordinates(latitude,longitude);
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
    };

    LocationListener NetworkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            myhandler.post(new LocationWork(latitude,longitude,"NETWORK"));
            if(!stopM2){
                setCoordinates(latitude,longitude);
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
    };




    private class LocationWork implements Runnable {

        private double latitude_d, longitude_d;
        private String provider_d;

        public LocationWork(double latitude_, double longitude_,String provider) {
            latitude_d = latitude_;
            longitude_d = longitude_;
            provider_d = provider;
            flat = latitude_d;
            flong = longitude_d;


        }



        @Override
        public void run() {

            latitudetxt.setText(new Double(latitude_d).toString());
            longitudetxt.setText(new Double(longitude_d).toString());
            providertxt.setText(new String(provider_d));

        }
    }

    public void setCoordinates(double secondLatitude, double secondLongitude){

            secndLat = secondLatitude;
            secondLong = secondLongitude;

            Log.d("Testing","Setting second method coordinates");



            latitude2txt.setActivated(true);

            latitude2txt.setText(new Double(secndLat).toString());
            longitude2txt.setText(new Double(secondLong).toString());


    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d("Testing","Getting Accelerometer");
            System.arraycopy(sensorEvent.values, 0, AccelerometerReadings, 0, sensorEvent.values.length);
            Log.d("Testing","Got accelerometer");
            t1 = (double) System.currentTimeMillis() / 500;
            if (it != 1) {
                Log.d("Testing","Entered Distance calculation");
                float ax = sensorEvent.values[0];
                float ay = sensorEvent.values[1];
                float az = sensorEvent.values[2];

                float alpha = (float) 0.8;
                gravity[0] = gravity[0] * alpha - (1 - alpha) * ax;
                gravity[1] = gravity[1] * alpha - (1 - alpha) * ay;
                gravity[2] = gravity[2] * alpha - (1 - alpha) * az;

                ax = ax - gravity[0];
                ay = ay - gravity[1];
                az = az - gravity[2];


                double dt = t1 - t2;
                double vel_x = ax * dt;
                double vel_y = ay * dt;
                double vel_z = az * dt;

                double d_x = vel_x * dt;
                double d_y = vel_y * dt;
                double d_z = vel_z * dt;
                dist = Math.sqrt(d_x * d_x + d_y * d_y + d_z * d_z);

            }

            t2 = (double) System.currentTimeMillis() / 500;
            it = 2;


        }


        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            System.arraycopy(sensorEvent.values, 0, MagnetometerReadings, 0, sensorEvent.values.length);
            Log.d("Testing","Got magnetometer");


        }


        if (latitude2txt.isActivated()) {

            if (AccelerometerReadings!=null && MagnetometerReadings!=null) {
                SensorManager.getRotationMatrix(rotationMatrix, null, AccelerometerReadings, MagnetometerReadings);
                SensorManager.getOrientation(rotationMatrix, orientation);
                Angle = (int) Math.round((Math.toDegrees(orientation[0]) + 360) % 360);


                double xc = dist * Math.cos(Math.toRadians(Angle));
                double yc = dist * Math.sin(Math.toRadians(Angle));

                double currentLatitude = Double.parseDouble(String.valueOf(latitude2txt.getText()));
                double currentLongitude = Double.parseDouble((String.valueOf(longitude2txt.getText())));

                double longitudeChange = yc / (111320 * Math.cos(Math.toRadians(currentLatitude)));
                double latitudeChange = xc / 110540;


                double newLat = latitudeChange + currentLatitude;
                double newLong = longitudeChange + currentLongitude;

                latitude2txt.setText(String.valueOf(newLat));
                longitude2txt.setText(String.valueOf(newLong));

                //Distance Calculation
                double dlat = Math.toRadians(newLat - flat);
                double dlon = Math.toRadians(newLong - flong);
                double a = Math.pow(Math.sin((dlat) / 2), 2) + Math.cos((flat)) * Math.cos(newLat) * Math.pow(Math.sin(dlon / 2), 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                double d = c * 6371000;
                distancetxt.setText(String.format("%5f", d));
            }
        }
    }
}
