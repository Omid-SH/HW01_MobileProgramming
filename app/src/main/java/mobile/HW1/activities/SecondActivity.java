package mobile.HW1.activities;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import es.dmoral.toasty.Toasty;
import mobile.HW1.R;

public class SecondActivity extends AppCompatActivity {


    static String TAG = "TAG";

    //specific key in order to save json data.
    private String fileName = "Weather";

    private static final int START_GETTING_JSON = 0;
    private static final int LOCATION_MODE = 1;
    private static final int RENDER = 2;
    private static final int CITY_NAME = 3;

    TextView cityField;
    TextView updatedField;
    TextView detailsField;
    TextView currentTemperatureField;
    // TextView weatherIcon;
    Thread jsonGetterThread;
    ProgressBar progressBar;

    JSONObject json;
    Handler mHandlerThread;
    Thread nameGetterThreadLocationMode;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_page);

        setUpViewComponents();

        mHandlerThread = new MapBoxSearcherHandler();

        String coordinates = getCityCoordinates();

        jsonGetterThread = new Thread(() -> {
            json = getJSON(coordinates);
            mHandlerThread.sendEmptyMessage(RENDER);
        });

        mHandlerThread.sendEmptyMessage(START_GETTING_JSON);
    }

    private void setUpViewComponents() {

        // extracting components.
        cityField = findViewById(R.id.cityField);
        updatedField = findViewById(R.id.update);
        detailsField = findViewById(R.id.detailsField);
        currentTemperatureField = findViewById(R.id.temperature);
        // weatherIcon = findViewById(R.id.weatherIcon);
        progressBar = findViewById(R.id.secondProgressBar);

        // setting weatherFont ...
        // Typeface weatherFont = ResourcesCompat.getFont(this, R.font.weather);
        // weatherIcon.setTypeface(weatherFont);
        // weatherIcon.setText(getString(R.string.weather_clear_night));

    }

    private String getCityCoordinates() {

        // default is CarmenFeature ...
        // getting pressed city and its coordinates ...
        CarmenFeature cityName = DataHolder.getInstance().getData();
        String coordinates;

        if (cityName != null) {

            cityField.setText(cityName.placeName());
            // loading data ...
            coordinates = String.valueOf(cityName.center().latitude())
                    .concat(",")
                    .concat(String.valueOf(cityName.center().longitude()));

        } else {

            Location location = DataHolder.getInstance().getLocation();
            if (location != null) {

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());

                // todo memory Leakage ...
                nameGetterThreadLocationMode = new Thread(() -> {

                    List<Address> addresses = null;
                    try {
                        DecimalFormat df = new DecimalFormat();
                        df.setMaximumFractionDigits(3);

                        double lat = Double.parseDouble(df.format(location.getLatitude()));
                        double lon =
                                Double.parseDouble(df.format(location.getLongitude()));
                        addresses = geocoder.getFromLocation(lat, lon, 1);

                    } catch (IOException e) {

                        e.printStackTrace();

                    } finally {

                        if (addresses == null) {
                            Toasty.error(getApplicationContext(), "CityName is not available:(", Toasty.LENGTH_LONG).show();
                        } else {

                            Message message = new Message();
                            message.what = CITY_NAME;
                            message.obj = addresses.get(0).getAddressLine(0);
                            mHandlerThread.sendMessage(message);

                        }

                    }
                });

                mHandlerThread.sendEmptyMessage(LOCATION_MODE);

                coordinates = String.valueOf(location.getLatitude())
                        .concat(",")
                        .concat(String.valueOf(location.getLongitude()));
            } else {

                coordinates = null;
            }
        }

        return coordinates;
    }


    class MapBoxSearcherHandler extends Handler {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == START_GETTING_JSON) {

                jsonGetterThread.start();

            } else if (msg.what == RENDER) {

                progressBar.setVisibility(View.GONE);
                renderWeather(json);

            } else if (msg.what == LOCATION_MODE) {

                nameGetterThreadLocationMode.start();

            } else if (msg.what == CITY_NAME) {

                cityField.setText(msg.obj.toString());

            }

        }

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public JSONObject getJSON(String coordintate) {

        // read from internal Storage
        if (coordintate == null) {
            return getSavedData();
        }

        // get json from network.
        try {

            final String DARK_SKY = "https://api.forecast.io/forecast/a6b8c7b90a261e493e22279291026462/%s";
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

        if (json == null) {
            Toasty.warning(getApplicationContext(), "No saved/gotten Data!", Toast.LENGTH_LONG).show();
            return;
        }

        try {

            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK);

            String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
            JSONArray data_array = json.getJSONObject("daily").getJSONArray("data");

            for (int i = 0; i < 7; i++) {

                JSONObject item = data_array.getJSONObject(i);

                String temperatureMax = item.getString("temperatureMax");
                String temperatureMin = item.getString("temperatureMin");

                String summary = item.getString("summary");
                temperatureMax = temperatureMax.substring(0, 2);
                temperatureMin = temperatureMin.substring(0, 2);

                String detailField = String.valueOf(detailsField.getText())
                        .concat(days[(today + i) % 7])
                        .concat(": ")
                        .concat(temperatureMin)
                        .concat(" - ")
                        .concat(temperatureMax)
                        .concat(" ")
                        .concat(summary)
                        .concat("\n");

                detailsField.setText(detailField);

            }

            String temperature = json.getJSONObject("currently").getString("temperature").concat(" \u00b0 F");
            currentTemperatureField.setText(temperature);

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
            return null;
        } else {
            try {
                String[] data = rawData.split("#");

                cityField.setText(data[0]);
                return new JSONObject(data[1]);

            } catch (JSONException e) {
                Toasty.error(getApplicationContext(), "JSON Creation Exception.", Toast.LENGTH_LONG).show();
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
        try (FileOutputStream fos = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE)) {

            String storeData = String.valueOf(cityField.getText()).concat("#").concat(json.toString());
            fos.write(storeData.getBytes());
            Log.v(TAG, "Data Saved");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
