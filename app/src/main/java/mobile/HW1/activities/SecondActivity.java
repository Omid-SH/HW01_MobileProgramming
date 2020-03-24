package mobile.HW1.activities;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

import mobile.HW1.R;

public class SecondActivity extends AppCompatActivity {


    static String TAG = "TAG";

    private String fileName = "Weather";

    private static final int START_GETTING_JSON_InternetMode = 0;
    private static final int RENDER_WEATHER = 2;

    TextView cityField;
    TextView updatedField;
    TextView detailsField;
    TextView currentTemperatureField;
    TextView weatherIcon;
    Thread jsonGetterThread;
    ProgressBar progressBar;

    JSONObject json;

    private static final String DARK_SKY = "https://api.forecast.io/forecast/a6b8c7b90a261e493e22279291026462/%s";

    private static Handler mHandlerThread;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_page);


        cityField = findViewById(R.id.cityField);
        updatedField = findViewById(R.id.update);
        detailsField = findViewById(R.id.detailsField);
        currentTemperatureField = findViewById(R.id.temperature);
        weatherIcon = findViewById(R.id.weatherIcon);
        progressBar = findViewById(R.id.secondProgressBar);
        mHandlerThread = new MapBoxSearcherHandler();

        Typeface weatherFont = ResourcesCompat.getFont(this, R.font.weather);
        weatherIcon.setTypeface(weatherFont);
        weatherIcon.setText(getString(R.string.weather_clear_night));


        // getting pressed city ...
        CarmenFeature cityName = DataHolder.getInstance().getData();

        if (cityName == null) {
            // setting NoInternetMode
            Log.v(TAG, "lunchNoInternetMode");
            lunchNoInternetMode();
            Log.v(TAG, "here :(");
            return;
        }

        // setting Up internetMode
        else {
            // setting up views
            cityField.setText(cityName.placeName());

            // loading data ...
            String coordinates = String.valueOf(cityName.center().latitude())
                    .concat(",")
                    .concat(String.valueOf(cityName.center().longitude()));


            jsonGetterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    json = getJSON(coordinates);
                    mHandlerThread.sendEmptyMessage(RENDER_WEATHER);
                }
            });

            mHandlerThread.sendEmptyMessage(START_GETTING_JSON_InternetMode);
        }
    }

    class MapBoxSearcherHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == START_GETTING_JSON_InternetMode) {
                jsonGetterThread.start();
            } else if (msg.what == RENDER_WEATHER) {
                progressBar.setVisibility(View.GONE);
                renderWeather(json);
            }

        }

    }


    public static JSONObject getJSON(String coordintate) {

        try {
            URL url = new URL(String.format((DARK_SKY), coordintate));

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuilder json = new StringBuilder(1024);
            String tmp;

            while ((tmp = reader.readLine()) != null) {
                Log.v(TAG, "gettingJson");
                json.append(tmp).append("\n");
            }
            reader.close();

            Log.v(TAG, "#" + json.toString() + "#");
            return new JSONObject(json.toString());

        } catch (
                Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private void renderWeather(JSONObject json) {

        try {
            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK);

            String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
            JSONArray data_array = json.getJSONObject("daily").getJSONArray("data");

            for (int i = 0; i < 7; i++) {

                JSONObject item = data_array.getJSONObject(i);

                String temperatureMax = item.getString("temperatureMax");
                String temperatureMin = item.getString("temperatureMin");
                String w_summary = item.getString("summary");
                temperatureMax = temperatureMax.substring(0, 2);
                temperatureMin = temperatureMin.substring(0, 2);

                detailsField.setText(detailsField.getText() + days[(today + i) % 7] + ": " + temperatureMin + " - " + temperatureMax + " " + w_summary + "\n");
            }


            currentTemperatureField.setText(json.getJSONObject("currently").getString("temperature") + " \u00b0 F");
            updatedField.setText(

                    // "SUMMARY OF WEEK  : " +
                    json.getJSONObject("daily").getString("summary")
                    // +      "\nTIME ZONE  : " + json.getString("timezone")
            );

        } catch (Exception e) {
            Log.e(TAG, "One or more fields not found in the JSON data");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public JSONObject getSavedData() {

        FileInputStream fis = null;
        try {
            fis = this.openFileInput(fileName);
            Log.v(TAG, "save:file exists");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.v(TAG, "save:file doesn't exist");
        }

        InputStreamReader inputStreamReader = null;
        if (fis != null) {
            inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            Log.v(TAG, "save:open stream done ");
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (inputStreamReader != null) {
            Log.v(TAG, "save: getting data ");
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append('\n');
                    line = reader.readLine();
                }
            } catch (IOException e) {
                // Error occurred when opening raw file for reading.
                Log.v(TAG, "save: can not read Data");
            }

            Log.v(TAG, "Data Got successfully");
        }

        String rawData = stringBuilder.toString();
        if (rawData.equals("")) {
            Toast.makeText(getApplicationContext(), "No previousData Saved.", Toast.LENGTH_LONG).show();
            return null;
        } else {
            try {
                return new JSONObject(stringBuilder.toString());
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "JSON Creation Exception.", Toast.LENGTH_LONG).show();
                return null;
            }
        }

    }


    @Override
    protected void onPause() {
        Log.v(TAG, "onPause Happened");
        super.onPause();

        if (json != null) {
            saveData(json);
            Log.v(TAG, "Data Saved Successfully");
        } else {
            Log.v(TAG, "There is no Data to save");
        }
    }


    public void saveData(JSONObject json) {

        Log.v(TAG, "Saving Data");
        Log.v(TAG, "#" + json.toString() + "#");
        try (FileOutputStream fos = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(json.toString().getBytes());
            Log.v(TAG, "Data Saved");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void lunchNoInternetMode() {
        Log.v(TAG, "getting Data: ");
        json = getSavedData();
        Message message = new Message();
        message.what = RENDER_WEATHER;
        mHandlerThread.sendMessage(message);
    }

}