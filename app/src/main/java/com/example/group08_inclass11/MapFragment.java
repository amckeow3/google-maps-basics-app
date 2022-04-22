package com.example.group08_inclass11;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.internal.maps.zzad;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapFragment extends Fragment {

    private static final String TAG = "map fragment";
    MapFragment.MapFragmentListener mListener;
    ArrayList<Double> latArray = new ArrayList<>();
    ArrayList<Double> longArray = new ArrayList<>();

    private ArrayList<Double> latitudePoints = new ArrayList<>();
    private ArrayList<Double> longitudePoints = new ArrayList<>();
    private List<LatLng> latLngValues = new ArrayList<>();

    private final OkHttpClient client = new OkHttpClient();
    private GoogleMap mMap;

    public MapFragment() {
        // Required empty public constructor
    }
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getRoute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        // Initialize map fragment
        SupportMapFragment supportMapFragment=(SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
                

                Log.d(TAG, "onMapReady: Latitude Points ==>" + latitudePoints);
                Log.d(TAG, "onMapReady: Longitude Points ==>" + longitudePoints);

                Double startLat = latitudePoints.get(0);
                Double startLong = longitudePoints.get(0);

                Log.d(TAG, "onMapReady: Route Start --- (" + startLat + ", " + startLong + ")");
                Double endLat = latitudePoints.get(latitudePoints.size() - 1);
                Double endLong = longitudePoints.get(longitudePoints.size() - 1);
                Log.d(TAG, "onMapReady: Route End --- (" + endLat + ", " + endLong + ")");

                LatLng start = new LatLng(startLat, startLong);
                googleMap.addMarker(new MarkerOptions()
                        .position(start)
                        .title("Route Start"));

                LatLng end = new LatLng(endLat, endLong);
                googleMap.addMarker(new MarkerOptions()
                        .position(end)
                        .title("Route End"));

                Polyline polyline1;
                polyline1 = googleMap.addPolyline(new PolylineOptions()
                        .clickable(true));
                LatLngBounds latlngbounds = new LatLngBounds(new LatLng(startLat, startLong), new LatLng(endLat, endLong));
                for (int i = 0; i < latitudePoints.size(); i++) {
                    LatLng latLng = new LatLng(latitudePoints.get(i), longitudePoints.get(i));
                    latLngValues.add(latLng);
                }
                Log.d(TAG, "Poly line LatLng: " + latLngValues);
                polyline1.setPoints(latLngValues);
                latlngbounds.getCenter();

                Double smallestLat = latitudePoints.get(0);
                Double largestLat = latitudePoints.get(0);
                Double smallestLong = longitudePoints.get(0);
                Double largestLong = longitudePoints.get(0);

                for(int i = 0; i< latitudePoints.size(); i++) {
                    if (latitudePoints.get(i) > largestLat)
                        largestLat = latitudePoints.get(i);
                    else if (latitudePoints.get(i) < smallestLat)
                        smallestLat = latitudePoints.get(i) ;
                }
                Log.d(TAG, "Largest Latitude value: " + largestLat);
                Log.d(TAG, "Smallest Latitude value: " + smallestLat);

                for(int i = 0; i< longitudePoints.size(); i++) {
                    if (longitudePoints.get(i) > largestLong)
                        largestLong = longitudePoints.get(i);
                    else if (longitudePoints.get(i) < smallestLong)
                        smallestLong = longitudePoints.get(i) ;
                }
                Log.d(TAG, "Largest Longitude value: " + largestLong);
                Log.d(TAG, "Smallest Longitude value: " + smallestLong);

                Double midLat = (largestLat + smallestLat) / 2;
                Double midLong = (largestLong + smallestLong) / 2;
                Log.d(TAG, "Mid Point: (" + midLat + ", " + midLong + ")");

                LatLng mid = new LatLng(midLat, midLong);
                googleMap.addMarker(new MarkerOptions()
                        .position(end)
                        .title("Route End"));

                LatLngBounds routeBounds = new LatLngBounds(
                        new LatLng(smallestLat, smallestLong), // SW bounds
                        new LatLng(largestLat, largestLong)  // NE bounds
                );

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routeBounds.getCenter(), 10));
            }
        });

        return view;
    }

    void getRoute() {
        Request request = new Request.Builder()
                .url("https://www.theappsdr.com/map/route")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    String body = responseBody.string();
                    Log.d(TAG, "onResponse: " + body);

                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray pointsJson = json.getJSONArray("path");

                        for (int i = 0; i < pointsJson.length(); i++) {
                            JSONObject pointJsonObject = pointsJson.getJSONObject(i);

                            Double latitude;
                            Double longitude;

                            latitude = pointJsonObject.getDouble("latitude");
                            longitude = pointJsonObject.getDouble("longitude");

                            latArray.add(latitude);
                            longArray.add(longitude);
                        }
                        latitudePoints.addAll(latArray);
                        longitudePoints.addAll(longArray);

                        Log.d(TAG, "Number of points: " + latitudePoints.size());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Paths Activity");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (MapFragment.MapFragmentListener) context;
    }

    interface MapFragmentListener {

    }
}