package mobile.HW1.classes.Activities;

import android.annotation.SuppressLint;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

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
import mobile.HW1.classes.Adapters.DayAdapter;
import mobile.HW1.classes.Utils.DataHolder;

import static mobile.HW1.R.*;
import static mobile.HW1.R.color.*;
import static mobile.HW1.R.color.text_color_cloudy;
import static mobile.HW1.R.color.text_color_night;
import static mobile.HW1.R.color.text_color_rainy;


public class SecondActivity extends AppCompatActivity {

    static String TAG = "TAG";

    //specific key in order to save json data.
    private String fileName = "Weather";

    private static final int SAVE_DATA = -2;
    private static final int START_GETTING_JSON = -1;
    private static final int LOCATION_MODE = 0;
    private static final int RENDER = 1;
    private static final int CITY_NAME = 2;
    private static final int LOCATION_MODE_CITY_NAME_NOT_AVAILABLE = 3;


    TextView cityName;

    TextView currentDayName;
    TextView currentTemperature;
    TextView currentSummary;

    TextView clickedDayHumidity, clickedDayPrecipitation, clickedDayWind;

    ConstraintLayout mainBackground;
    LinearLayout moreDetail;

    Thread jsonGetterThread;
    ProgressBar progressBar;

    JSONObject json;
    Handler mHandlerThread;
    Thread nameGetterThreadLocationMode;

    RecyclerView dayRecyclerView;
    private DayAdapter mAdapter = new DayAdapter(new DayClickListener());


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {


        Log.v(TAG, "Start Creation");

        super.onCreate(savedInstanceState);
        setContentView(layout.second_page);

        setUpViewComponents();

        mHandlerThread = new MapBoxSearcherHandler();

        String coordinates = getCityCoordinates();

        jsonGetterThread = new Thread(() -> {
            Log.v(TAG, "inside of getterJsonThread");
            json = getJSON(coordinates);
            Log.v(TAG, "json Got");
            mHandlerThread.sendEmptyMessage(RENDER);
        });

        mHandlerThread.sendEmptyMessage(START_GETTING_JSON);

    }

    private void setUpViewComponents() {


        // extracting components.
        cityName = findViewById(id.city_name);

        currentDayName = findViewById(id.current_day_name);
        currentTemperature = findViewById(id.current_temperature);
        currentSummary = findViewById(id.current_summary);

        // bottom bar ...
        moreDetail = findViewById(id.more_detail);
        clickedDayWind = findViewById(id.clicked_day_wind);
        clickedDayPrecipitation = findViewById(id.clicked_day_precipitation);
        clickedDayHumidity = findViewById(id.clicked_day_humidity);

        mainBackground = findViewById(id.main_background);

        progressBar = findViewById(id.secondProgressBar);

        dayRecyclerView = findViewById(id.days);

        dayRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        dayRecyclerView.setLayoutManager(layoutManager);


        // specify an adapter
        dayRecyclerView.setAdapter(mAdapter);


    }

    private String getCityCoordinates() {

        Log.v(TAG, "Start getting coordinates");
        // default is CarmenFeature ...
        // getting pressed city and its coordinates ...
        CarmenFeature cityName = DataHolder.getInstance().getData();
        String coordinates;

        if (cityName != null) {
            // network mode

            this.cityName.setText(cityName.placeName());
            // loading data ...
            coordinates = String.valueOf(cityName.center().latitude())
                    .concat(",")
                    .concat(String.valueOf(cityName.center().longitude()));

        } else {
            // Location mode
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
                            mHandlerThread.sendEmptyMessage(LOCATION_MODE_CITY_NAME_NOT_AVAILABLE);
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

        Log.v(TAG, "coordinates Got" + coordinates);
        return coordinates;
    }


    class MapBoxSearcherHandler extends Handler {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == START_GETTING_JSON) {
                Log.v(TAG, "requesting getting Json");
                jsonGetterThread.start();

            } else if (msg.what == RENDER) {

                try {
                    Log.v(TAG, "Start Rendering " + json.toString());
                    mAdapter.setDataSet(json.getJSONObject("daily").getJSONArray("data"));
                    Log.v(TAG, String.valueOf(mAdapter.getDataSet().length()));
                    mAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                progressBar.setVisibility(View.GONE);
                renderWeather(0);

            } else if (msg.what == LOCATION_MODE) {

                nameGetterThreadLocationMode.start();

            } else if (msg.what == CITY_NAME) {

                cityName.setText(msg.obj.toString());

            } else if (msg.what == LOCATION_MODE_CITY_NAME_NOT_AVAILABLE) {
                Toasty.error(getApplicationContext(), "CityName is not available:(", Toasty.LENGTH_LONG).show();
            } else if (msg.what == SAVE_DATA) {
                ((Thread) msg.obj).start();
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

                Message message = new Message();
                message.what = CITY_NAME;
                message.obj = data[0];
                mHandlerThread.sendMessage(message);

                return new JSONObject(data[1]);

            } catch (JSONException e) {
                Log.e(TAG, "JSON Creation Exception.");
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

        Thread saveDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (FileOutputStream fos = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE)) {

                    String storeData = String.valueOf(cityName.getText()).concat("#").concat(json.toString());
                    fos.write(storeData.getBytes());
                    Log.v(TAG, "Data Saved");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Message message = new Message();
        message.obj = saveDataThread;
        message.what = SAVE_DATA;
        mHandlerThread.sendMessage(message);


    }

    class DayClickListener implements View.OnClickListener {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onClick(View v) {

            int itemPosition = dayRecyclerView.getChildLayoutPosition(v);
            renderWeather(itemPosition);
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void renderWeather(int dayOffSet) {

        if (json == null) {
            Toasty.warning(getApplicationContext(), "No saved/gotten Data!", Toast.LENGTH_LONG).show();
            return;
        }

        try {

            // current box
            currentDayName.setVisibility(View.VISIBLE);
            currentTemperature.setVisibility(View.VISIBLE);
            currentSummary.setVisibility(View.VISIBLE);

            // day box
            dayRecyclerView.setVisibility(View.VISIBLE);
            // details box
            moreDetail.setVisibility(View.VISIBLE);

            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK) - 1 + dayOffSet;

            String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

            currentDayName.setText(days[today % 7]);

            if (dayOffSet == 0) {

                String temperature = String.valueOf(Math.round(Double.parseDouble(json.getJSONObject("currently")
                        .getString("temperature")))).concat(" \u00b0 F");

                currentTemperature.setText(temperature);

                String summary = json.getJSONObject("currently").getString("summary");
                currentSummary.setText(summary);

                // set main colors and background based on current icon

                String currentIcon = json.getJSONObject("currently").getString("icon");
                Log.v(TAG, "MainBack" + currentIcon);
                switch (currentIcon) {

                    // setting background page according to the icon.
                    case "snow":
                    case "sleet":
                        currentDayName.setTextColor(getResources().getColor(text_color_snowy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_snowy));
                        currentSummary.setTextColor(getResources().getColor(text_color_snowy));
                        moreDetail.setBackgroundResource(more_detail_snowy);
                        mainBackground.setBackgroundResource(drawable.snowy_background);
                        break;

                    case "clear-day":
                    case "clear":
                        currentDayName.setTextColor(getResources().getColor(text_color_sunny));
                        currentTemperature.setTextColor(getResources().getColor(text_color_sunny));
                        currentSummary.setTextColor(getResources().getColor(text_color_sunny));
                        moreDetail.setBackgroundResource(more_detail_sunny);
                        mainBackground.setBackgroundResource(drawable.sunny_background);
                        break;

                    case "clear-night":
                        currentDayName.setTextColor(getResources().getColor(text_color_night));
                        currentTemperature.setTextColor(getResources().getColor(text_color_night));
                        currentSummary.setTextColor(getResources().getColor(text_color_night));
                        moreDetail.setBackgroundResource(more_detail_night);
                        mainBackground.setBackgroundResource(drawable.clear_night_background);
                        break;

                    case "rain":
                        currentDayName.setTextColor(getResources().getColor(text_color_rainy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_rainy));
                        currentSummary.setTextColor(getResources().getColor(text_color_rainy));
                        moreDetail.setBackgroundResource(more_detail_rainy);
                        mainBackground.setBackgroundResource(drawable.rainy_background);
                        break;

                    default:
                        currentDayName.setTextColor(getResources().getColor(text_color_cloudy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_cloudy));
                        currentSummary.setTextColor(getResources().getColor(text_color_cloudy));
                        moreDetail.setBackgroundResource(more_detail_cloudy);
                        mainBackground.setBackgroundResource(drawable.cloudy_background);
                        break;
                }

            } else {

                JSONObject data = mAdapter.getBottomBarData(dayOffSet);

                String temperature = String.valueOf(
                        Math.round((Double.parseDouble(data.getString("temperatureHigh"))
                                + Double.parseDouble(data.getString("temperatureLow"))) / 2)
                ).concat(" \u00b0 F");

                currentTemperature.setText("Avg: ".concat(temperature));

                String summary = data.getString("summary");
                currentSummary.setText(summary);

                // set main colors and background based on current icon

                String currentIcon = data.getString("icon");

                Log.v(TAG, "MainBack" + currentIcon);
                switch (currentIcon) {

                    // setting background page according to the icon.
                    case "snow":
                    case "sleet":
                        currentDayName.setTextColor(getResources().getColor(text_color_snowy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_snowy));
                        currentSummary.setTextColor(getResources().getColor(text_color_snowy));
                        moreDetail.setBackgroundResource(more_detail_snowy);
                        mainBackground.setBackgroundResource(drawable.snowy_background);
                        break;

                    case "clear-day":
                    case "clear":
                        currentDayName.setTextColor(getResources().getColor(text_color_sunny));
                        currentTemperature.setTextColor(getResources().getColor(text_color_sunny));
                        currentSummary.setTextColor(getResources().getColor(text_color_sunny));
                        moreDetail.setBackgroundResource(more_detail_sunny);
                        mainBackground.setBackgroundResource(drawable.sunny_background);
                        break;

                    case "clear-night":
                        currentDayName.setTextColor(getResources().getColor(text_color_night));
                        currentTemperature.setTextColor(getResources().getColor(text_color_night));
                        currentSummary.setTextColor(getResources().getColor(text_color_night));
                        moreDetail.setBackgroundResource(more_detail_night);
                        mainBackground.setBackgroundResource(drawable.clear_night_background);
                        break;

                    case "rain":
                        currentDayName.setTextColor(getResources().getColor(text_color_rainy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_rainy));
                        currentSummary.setTextColor(getResources().getColor(text_color_rainy));
                        moreDetail.setBackgroundResource(more_detail_rainy);
                        mainBackground.setBackgroundResource(drawable.rainy_background);
                        break;

                    default:
                        currentDayName.setTextColor(getResources().getColor(text_color_cloudy));
                        currentTemperature.setTextColor(getResources().getColor(text_color_cloudy));
                        currentSummary.setTextColor(getResources().getColor(text_color_cloudy));
                        moreDetail.setBackgroundResource(more_detail_cloudy);
                        mainBackground.setBackgroundResource(drawable.cloudy_background);
                        break;
                }

            }


            JSONObject bottomBarData = mAdapter.getBottomBarData(dayOffSet);
            clickedDayWind.setText(bottomBarData.getString("windSpeed"));
            clickedDayPrecipitation.setText(bottomBarData.getString("precipProbability"));
            clickedDayHumidity.setText(bottomBarData.getString("humidity"));


        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "One or more fields not found in the JSON data");
        }

    }

    @Override
    public void onBackPressed() {

        if (!isDisConnected()) {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            return;
        }

        Toasty.warning(getApplicationContext(), "Please Connect to Internet then press Back Button.", Toasty.LENGTH_LONG).show();

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


}
