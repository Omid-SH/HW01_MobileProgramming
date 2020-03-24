package mobile.HW1.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dd.processbutton.iml.ActionProcessButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;


import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import mobile.HW1.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    // TAG for debugging.
    private static String TAG = "TAG";

    // button is local defined.
    private EditText editText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ActionProcessButton button;

    // mHandler and his! whats.
    private static Handler mHandlerThread;
    private static final int SEARCH_DONE = 0;
    private static final int START_SEARCHING = 1;

    // result of search...
    private static ArrayList<CarmenFeature> results = new ArrayList<>();
    private MyAdapter mAdapter = new MyAdapter(results, new MyOnClickListener());

    // singleton object contains the clicked item in recyclerView.
    // in order to communicate between activities.
    private DataHolder dataHolder = DataHolder.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // location part
        if (isDisConnected()) {
            noInternetMode();
            return;
        }

        setContentView(R.layout.first_page);
        setUpViewComponents();
        // handler part
        mHandlerThread = new geocodeSearcherHandler(results);

    }

    public void setUpViewComponents() {

        editText = findViewById(R.id.input_string);
        button = findViewById(R.id.search_button);
        Button locationButton = findViewById(R.id.location_button);
        recyclerView = findViewById(R.id.my_recycler_view);
        progressBar = findViewById(R.id.progressBar);


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                button.setProgress(0);
                button.setText(R.string.search);
            }
        });

        // setting button listener.
        button.setMode(ActionProcessButton.Mode.PROGRESS);
        button.setOnClickListener(v -> {

            if (isDisConnected()) {
                Toasty.error(v.getContext(), "No internet Connection", Toast.LENGTH_SHORT, true).show();
                return;
            }

            if (!String.valueOf(editText.getText()).equals("")) {
                button.setMode(ActionProcessButton.Mode.ENDLESS);
                // set progress > 0 to start progress indicator animation
                button.setProgress(1);
            }

            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputManager != null) {

                try {
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                } catch (Exception e) {
                    Log.e(TAG, "Window Token Exception happen.");
                }

            }

            String location = editText.getText().toString();

            if (location.equals("")) {
                Toasty.error(v.getContext(), "Please write sth!", Toast.LENGTH_SHORT, true).show();
                return;
            }

            Message message = new Message();
            message.what = START_SEARCHING;
            message.obj = location;
            mHandlerThread.sendMessage(message);

        });

        locationButton.setOnClickListener(v -> {

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fetchLastLocation();

        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        recyclerView.setAdapter(mAdapter);

    }

    public void noInternetMode() {
        Toasty.warning(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT, true).show();
        dataHolder.setData(null);
        Intent i = new Intent(this, mobile.HW1.activities.SecondActivity.class);
        startActivity(i);
    }

    public boolean isDisConnected() {

        // internet connection check
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return !isConnected;

    }

    class geocodeSearcherHandler extends Handler {
        private final ArrayList<CarmenFeature> results;

        geocodeSearcherHandler(ArrayList<CarmenFeature> results) {
            this.results = results;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {

            Log.v(TAG, "handling Massage: mHandler");
            super.handleMessage(msg);

            if (msg.what == START_SEARCHING) {
                progressBar.setVisibility(View.VISIBLE);
                // start Searching
                gecode(msg.obj.toString());

            } else {

                if (results.size() != 0) {
                    // resetting recycler view.
                    mAdapter.reset();
                }

                results.addAll((ArrayList<CarmenFeature>) msg.obj);
                mAdapter.notifyDataSetChanged();
                button.setProgress(100);
                button.setText(R.string.done);
                progressBar.setVisibility(View.GONE);
                Log.v(TAG, "workDone");

            }
        }

    }

    class MyOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            int itemPosition = recyclerView.getChildLayoutPosition(v);
            CarmenFeature item = results.get(itemPosition);
            dataHolder.setData(item);
            Intent i = new Intent(v.getContext(), mobile.HW1.activities.SecondActivity.class);
            startActivity(i);

            String cityName = item.placeName();
            if (cityName != null)
                Toasty.success(v.getContext(), cityName, Toast.LENGTH_LONG).show();

        }
    }

    public static void gecode(String name) {

        String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYWhyZWNjZXNlIiwiYSI6ImNrN28yZjE1ZzA0bnIzZG0zb2kxNzlrcHkifQ.J5-vlsAJMMenyqXyRoq32A";

        ArrayList<CarmenFeature> cityResults = new ArrayList<>();

        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(MAPBOX_ACCESS_TOKEN)
                .query(name)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                List<CarmenFeature> results;
                if (response.body() != null) {
                    results = response.body().features();

                } else {
                    Log.v(TAG, "onResponse: Response Body is empty");
                    return;
                }

                if (results.size() > 0) {

                    for (int i = 0; i < results.size(); i++) {

                        cityResults.add(results.get(i));
                        // Log the first results Point.
                        String firstResultPoint = results.get(i).placeName();
                        Log.v(TAG, "onResponse: " + firstResultPoint);

                    }

                    Message message = new Message();
                    message.what = SEARCH_DONE;
                    message.obj = cityResults;
                    mHandlerThread.sendMessage(message);

                } else {
                    // No result for your request were found.
                    Log.v(TAG, "onResponse: No result found");
                }

            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                throwable.printStackTrace();
            }
        });

    }


    private void fetchLastLocation() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
//                    Toast.makeText(MainActivity.this, "Permission not granted, Kindly allow permission", Toast.LENGTH_LONG).show();
                showPermissionAlert();
                return;
            }
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        Log.e("LAST LOCATION: ", location.toString());

                        dataHolder.setLocation(location);
                        Intent i = new Intent(getApplicationContext(), mobile.HW1.activities.SecondActivity.class);
                        startActivity(i);


                    }
                });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.v(TAG, String.valueOf(requestCode));
        if (requestCode == 123) {// If request is cancelled, the result arrays are empty.
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // permission was denied, show alert to explain permission
                Toasty.warning(getApplicationContext(), "Sorry you didn't allow permissions", Toasty.LENGTH_LONG).show();
            } else {
                //permission is granted now start a background service
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
            }
        }
    }

    private void showPermissionAlert() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        }
    }

}


