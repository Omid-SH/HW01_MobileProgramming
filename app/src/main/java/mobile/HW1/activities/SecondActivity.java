package mobile.HW1.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

import mobile.HW1.R;

public class SecondActivity extends AppCompatActivity {


    static String TAG = "TAG";

    private static final int START_GETTING_JSON = 0;
    private static final int RENDER_WEATHER = 1;

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


        // getting pressed city ...
        CarmenFeature cityName = DataHolder.getInstance().getData();

        // setting up views
        Typeface weatherFont = ResourcesCompat.getFont(this, R.font.weather);
        weatherIcon.setTypeface(weatherFont);
        weatherIcon.setText(getString(R.string.weather_clear_night));
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

        mHandlerThread.sendEmptyMessage(START_GETTING_JSON);

    }

    class MapBoxSearcherHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == START_GETTING_JSON) {
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
                Log.v(TAG, tmp);
                json.append(tmp).append("\n");
            }
            reader.close();

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
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }

}

