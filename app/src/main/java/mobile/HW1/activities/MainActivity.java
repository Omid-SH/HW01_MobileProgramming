package mobile.HW1.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;


import java.util.ArrayList;
import java.util.List;

import mobile.HW1.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // TAG for debugging.
    private static String TAG = "TAG";

    // button is local defined.
    private TextView textView;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

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

        if (!isConnected()) {
            noInternetMode();
            return;
        }

        setContentView(R.layout.first_page);
        setUpViewComponents();
        // handler part
        mHandlerThread = new geocodeSearcherHandler(results);

    }

    public void setUpViewComponents() {

        textView = findViewById(R.id.input_string);
        Button button = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.my_recycler_view);
        progressBar = findViewById(R.id.progressBar);

        // setting button listener.
        button.setOnClickListener(v -> {

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

            String location = textView.getText().toString();

            if (location.equals("")) {
                Toast.makeText(v.getContext(), "Please write sth!", Toast.LENGTH_LONG).show();
                return;
            }

            Message message = new Message();
            message.what = START_SEARCHING;
            message.obj = location;
            mHandlerThread.sendMessage(message);
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
        Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        dataHolder.setData(null);
        Intent i = new Intent(this, mobile.HW1.activities.SecondActivity.class);
        startActivity(i);
        Log.v(TAG, "activity one :( ");
    }

    public boolean isConnected() {

        // internet connection check
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;

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
            Toast.makeText(v.getContext(), item.placeName(), Toast.LENGTH_LONG).show();

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

}


