package com.android.lambada.weatherforecast;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            //have to check trantResult.length, maybe sometime it will be 0
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //check map permission
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 5, locationListener);

                }

            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

//        queue = getRequestQueue(getApplicationContext());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        mMap.setOnMapLongClickListener(this);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                //13 is zoom in level,  1-20, 1 is smallest one
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));
                getWeatherInfo(userLocation);

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


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {

            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 5, locationListener);

            //update a location when user just open the app, doesn't happen location change event
            Location lastknownLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
            LatLng userLocation = new LatLng(lastknownLocation.getLatitude(), lastknownLocation.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            //13 is zoom in level,  1-20, 1 is smallest one
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));
            getWeatherInfo(userLocation);

        }

    }


    /**
     * @param latLng where you click
     */
    @Override
    public void onMapLongClick(LatLng latLng) {

        Log.i("Info latLng", latLng.toString());


        //get city name
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {
            List<Address> addressesList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addressesList != null && addressesList.size() > 0) {

                String placeInfo = "";


                if (addressesList.get(0).getThoroughfare() != null) {
                    placeInfo += addressesList.get(0).getThoroughfare() + " ";
                }

                if (addressesList.get(0).getLocality() != null) {
                    placeInfo += addressesList.get(0).getLocality() + " ";
                }

                if (addressesList.get(0).getCountryName() != null) {
                    placeInfo += addressesList.get(0).getCountryName() + " ";
                }


                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(placeInfo).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                getWeatherInfo(latLng);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getWeatherInfo(final LatLng latLng) {
        // String url = "https://query.yahooapis.com/v1/public/yql?q=select * from weather.forecast where woeid in (SELECT woeid FROM geo.places WHERE text=\"(-36.881248, 174.708303)\")&format=json";
        String url = "https://query.yahooapis.com/v1/public/yql?q=select * from weather.forecast where woeid in (SELECT woeid FROM geo.places WHERE text=\"(" + latLng.latitude + "," + latLng.longitude + ")\")&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("volley Response: ", response.toString());

                        try {
                            if(response.toString().equals(null)){

                                showTips("No weather info for current position.");
                            }
                            //TODO use Jsoup to get the elements
                            JSONObject data = response.getJSONObject("query");
                            JSONObject result = data.getJSONObject("results");
                            JSONObject channel = result.getJSONObject("channel");
                            JSONObject location = channel.getJSONObject("location");
                            JSONObject item = channel.getJSONObject("item");
                            JSONArray forecast = item.getJSONArray("forecast");

                            Log.i("data Info", forecast.toString());

                            String city = location.getString("city");
                            String country = location.getString("country");

                            JSONObject curDateInfo = forecast.getJSONObject(0);
//                            String curDate = curDateInfo.getString("date");
//                            String curDay = curDateInfo.getString("day");
                            String extraInfo = curDateInfo.getString("text");
                            String lowTemperature = curDateInfo.getString("low");
                            String highTemperature = curDateInfo.getString("high");

                            showTips(city + ", " + country + "\r\n-Temperature(F):" + lowTemperature + " ~ " + highTemperature + " " + extraInfo);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.i("volley error", error.toString());
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

    }

    public void showTips(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();

    }
}
