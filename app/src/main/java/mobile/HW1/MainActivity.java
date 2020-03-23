package mobile.HW1;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mobile.HW1.recyclerView.ThirdActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    Handler mHandlerThread;
    private static final int START_PROGRESS = 1;
    private static final int DONE = 0;

    ArrayList<String> results = new ArrayList<>();

    static String TAG = "tag";

    String filename = "myfile";

    String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYWhyZWNjZXNlIiwiYSI6ImNrN28yZjE1ZzA0bnIzZG0zb2kxNzlrcHkifQ.J5-vlsAJMMenyqXyRoq32A";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSavedData();

        //  running networking in main thread requesting
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String name = "Tehran";

        // connect to UI handler.
        Intent intent = new Intent(this, ThirdActivity.class);

        mHandlerThread = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == START_PROGRESS && results.size() == 0) {
                    testGeo(name);
                } else {
                    if (msg.obj != null) {
                        results.addAll((ArrayList<String>) msg.obj);
                    }
                    intent.putExtra("strings", results);
                    startActivity(intent);
                }
            }
        };


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void getSavedData() {

        FileInputStream fis = null;

        try {
            fis = this.openFileInput(filename);
            Log.v("save", "file exists");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.v("save", "file doesn't exist");
        }

        InputStreamReader inputStreamReader =
                null;
        if (fis != null) {
            inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            Log.v("save", " open stream done ");
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (inputStreamReader != null) {
            Log.v("save", " getting data ");
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append('\n');
                    line = reader.readLine();
                }
            } catch (IOException e) {
                // Error occurred when opening raw file for reading.
                Log.v("save", "can not read Data");
            }

            String contents = stringBuilder.toString();
            results = new ArrayList<String>(Arrays.asList(contents.split("#")));
            Log.v("Data", contents);

        }


    }

    public void onClick(View v) {

        Intent i;
        switch (v.getId()) {

            case R.id.newYork:

                Toast.makeText(MainActivity.this, "You clicked", Toast.LENGTH_LONG).show();
                mHandlerThread.sendEmptyMessage(START_PROGRESS);

                break;

            case R.id.london:

                Toast.makeText(MainActivity.this, "You clicked", Toast.LENGTH_LONG).show();

                i = new Intent(MainActivity.this, SecondActivity.class);
                i.putExtra("CityName", "London");
                startActivity(i);

                break;

            case R.id.paris:

                Toast.makeText(MainActivity.this, "You clicked", Toast.LENGTH_LONG).show();

                i = new Intent(MainActivity.this, SecondActivity.class);
                i.putExtra("CityName", "Paris");
                startActivity(i);

                break;

            case R.id.tokyo:

                Toast.makeText(MainActivity.this, "You clicked", Toast.LENGTH_LONG).show();

                i = new Intent(MainActivity.this, SecondActivity.class);
                i.putExtra("CityName", "Tokyo");
                startActivity(i);

                break;

            case R.id.losAngles:

                Toast.makeText(MainActivity.this, "You clicked", Toast.LENGTH_LONG).show();

                i = new Intent(MainActivity.this, SecondActivity.class);
                i.putExtra("CityName", "Los Angeles");
                startActivity(i);

                break;

            default:
                break;
        }

    }

    public ArrayList<String> testGeo(String name) {

        ArrayList<String> cityResults = new ArrayList<>();

        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(MAPBOX_ACCESS_TOKEN)
                .query(name)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                List<CarmenFeature> results = response.body().features();

                if (results.size() > 0) {

                    for (int i = 0; i < results.size(); i++) {

                        cityResults.add(results.get(i).placeName());
                        // Log the first results Point.
                        String firstResultPoint = results.get(i).placeName();
                        Log.v(TAG, "onResponse: " + firstResultPoint);

                    }

                    Message message = new Message();
                    message.what = DONE;
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

        return cityResults;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try (FileOutputStream fos = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE)) {
            for (int i = 0; i < results.size(); i++) {
                fos.write(results.get(i).getBytes());
                fos.write("#".getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
