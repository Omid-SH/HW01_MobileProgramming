package mobile.HW1.activities;

import android.annotation.SuppressLint;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
    private static final int NUMBER_OF_FUTURE_DAYS = 4;

    private int clickedDay = 0;
    TextView cityName;
    Button option;

    TextView currentDayName;
    TextView currentTemperature;
    TextView currentSummary;

    Button[] dayName = new Button[NUMBER_OF_FUTURE_DAYS];
    TextView[] dayMinTemperature = new TextView[NUMBER_OF_FUTURE_DAYS];
    TextView[] dayMaxTemperature = new TextView[NUMBER_OF_FUTURE_DAYS];
    ImageView[] dayIcon = new ImageView[NUMBER_OF_FUTURE_DAYS];

    TextView clickedDayHumidity, clickedDayPrecipitation, clickedDayWind;

    ConstraintLayout mainBackground;
    LinearLayout days, moreDetail;

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

        dayName[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDay = 0;
                renderWeather(json);
            }
        });

        dayName[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDay = 1;
                renderWeather(json);
            }
        });

        dayName[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDay = 2;
                renderWeather(json);
            }
        });

        dayName[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDay = 3;
                renderWeather(json);
            }
        });
    }

    private void setUpViewComponents() {


        // extracting components.
        cityName = (TextView) findViewById(R.id.city_name);
        option = (Button) findViewById(R.id.option);

        currentDayName = (TextView) findViewById(R.id.current_day_name);
        currentTemperature = (TextView) findViewById(R.id.current_temperature);
        currentSummary = (TextView) findViewById(R.id.current_summary);

        // set all days elements
        dayName[0] = (Button) findViewById(R.id.day0_name);
        dayIcon[0] = (ImageView) findViewById(R.id.day0_icon);
        dayMaxTemperature[0] = (TextView) findViewById(R.id.day0_max_temperature);
        dayMinTemperature[0] = (TextView) findViewById(R.id.day0_min_temperature);

        dayName[1] = (Button) findViewById(R.id.day1_name);
        dayIcon[1] = (ImageView) findViewById(R.id.day1_icon);
        dayMaxTemperature[1] = (TextView) findViewById(R.id.day1_max_temperature);
        dayMinTemperature[1] = (TextView) findViewById(R.id.day1_min_temperature);

        dayName[2] = (Button) findViewById(R.id.day2_name);
        dayIcon[2] = (ImageView) findViewById(R.id.day2_icon);
        dayMaxTemperature[2] = (TextView) findViewById(R.id.day2_max_temperature);
        dayMinTemperature[2] = (TextView) findViewById(R.id.day2_min_temperature);

        dayName[3] = (Button) findViewById(R.id.day3_name);
        dayIcon[3] = (ImageView) findViewById(R.id.day3_icon);
        dayMaxTemperature[3] = (TextView) findViewById(R.id.day3_max_temperature);
        dayMinTemperature[3] = (TextView) findViewById(R.id.day3_min_temperature);

        clickedDayWind = (TextView) findViewById(R.id.clicked_day_wind);
        clickedDayPrecipitation = (TextView) findViewById(R.id.clicked_day_precipitation);
        clickedDayHumidity = (TextView) findViewById(R.id.clicked_day_humidity);

        mainBackground = (ConstraintLayout) findViewById(R.id.main_background);
        days = (LinearLayout) findViewById(R.id.days);
        moreDetail = (LinearLayout) findViewById(R.id.more_detail);
        // weatherIcon = findViewById(R.id.weatherIcon);
        progressBar = findViewById(R.id.secondProgressBar);

        // setting weatherFont ...
        //Typeface weatherFont = ResourcesCompat.getFont(this, R.font.weather);
        //weatherIcon.setTypeface(weatherFont);
        //weatherIcon.setText(getString(R.string.weather_clear_night));

    }

    private String getCityCoordinates() {

        // default is CarmenFeature ...
        // getting pressed city and its coordinates ...
        CarmenFeature cityName = DataHolder.getInstance().getData();
        String coordinates;

        if (cityName != null) {

            this.cityName.setText(cityName.placeName());
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

                cityName.setText(msg.obj.toString());

            }

        }

    }

    private void cityNameAnimation() {
        @SuppressLint("ResourceType") AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.anim.city_name_anim);
        set.setTarget(R.id.city_name);
        set.start();


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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void renderWeather(JSONObject json) {

        if (json == null) {
            Toasty.warning(getApplicationContext(), "No saved/gotten Data!", Toast.LENGTH_LONG).show();
            return;
        }

        try {

            //set visibility
            currentDayName.setVisibility(View.VISIBLE);
            currentTemperature.setVisibility(View.VISIBLE);
            currentSummary.setVisibility(View.VISIBLE);
            days.setVisibility(View.VISIBLE);
            moreDetail.setVisibility(View.VISIBLE);

            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

            currentDayName.setText(days[today % 7]);

            String temperature = String.valueOf(Math.round(Double.parseDouble(json.getJSONObject("currently")
                    .getString("temperature")))).concat(" \u00b0 F");
            currentTemperature.setText(temperature);

            String summary = json.getJSONObject("currently").getString("summary");
            currentSummary.setText(summary);

            // set main colors and background based on current icon

            String currentIcon = json.getJSONObject("currently").getString("icon");
            if (currentIcon.equals("snow") || currentIcon.equals("sleet")) {
                mainBackground.setBackgroundResource(R.drawable.snowy_background);
            } else if (currentIcon.equals("clear-day") || currentIcon.equals("clear")) {
                mainBackground.setBackgroundResource(R.drawable.sunny_background);
            } else if (currentIcon.equals("clear-night")) {
                mainBackground.setBackgroundResource(R.drawable.clear_night_background);
            } else if (currentIcon.equals("rain")) {
                mainBackground.setBackgroundResource(R.drawable.rainy_background);
            } else {
                mainBackground.setBackgroundResource(R.drawable.cloudy_background);
            }

            JSONArray data_array = json.getJSONObject("daily").getJSONArray("data");

            for (int i = 0; i < NUMBER_OF_FUTURE_DAYS; i++) {

                JSONObject item = data_array.getJSONObject(i);

                String temperatureMax = item.getString("temperatureMax");
                String temperatureMin = item.getString("temperatureMin");
                String icon = item.getString("icon");

                temperatureMax = String.valueOf(Math.round(Double.parseDouble(temperatureMax)));
                temperatureMin = String.valueOf(Math.round(Double.parseDouble(temperatureMin)));

                dayName[i].setText(days[(today + i) % 7]);
                dayMaxTemperature[i].setText(temperatureMax);
                dayMinTemperature[i].setText(temperatureMin);

                //set icon
                if(icon.equals("snow"))
                    dayIcon[i].setImageResource(R.drawable.ic_snowy);
                else if(icon.equals("clear-day") || icon.equals("clear-night") || icon.equals("clear"))
                    dayIcon[i].setImageResource(R.drawable.ic_sunny);
                else if(icon.equals("rain"))
                    dayIcon[i].setImageResource(R.drawable.ic_rainy);
                else if(icon.equals("sleet"))
                    dayIcon[i].setImageResource(R.drawable.ic_sleet);
                else if(icon.equals("wind"))
                    dayIcon[i].setImageResource(R.drawable.ic_windy);
                else if(icon.equals("fog"))
                    dayIcon[i].setImageResource(R.drawable.ic_fog);
                else
                    dayIcon[i].setImageResource(R.drawable.ic_cloudy);

                if(i == clickedDay) {
                    String wind = item.getString("windSpeed");
                    wind = String.valueOf(Math.round(Double.parseDouble(wind)));
                    clickedDayWind.setText(wind);

                    String precipitation = item.getString("precipProbability");
                    clickedDayPrecipitation.setText(precipitation);

                    String humidity = item.getString("humidity");
                    clickedDayHumidity.setText(humidity);
                }

            }


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

                cityName.setText(data[0]);
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

            String storeData = String.valueOf(cityName.getText()).concat("#").concat(json.toString());
            fos.write(storeData.getBytes());
            Log.v(TAG, "Data Saved");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
