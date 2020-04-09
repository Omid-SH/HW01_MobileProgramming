package mobile.HW1.classes.Utils;

import android.location.Location;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

public class DataHolder {

    private CarmenFeature data;
    private Location location;
    private static String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public CarmenFeature getData() {
        return data;
    }

    public void setData(CarmenFeature data) {
        this.data = data;
    }

    private static final DataHolder holder = new DataHolder();

    public static DataHolder getInstance() {
        return holder;
    }

    public void invalid() {
        this.data = null;
        this.location = null;
    }

    public static String getMapBoxToken() {
        return "pk.eyJ1IjoiYWhyZWNjZXNlIiwiYSI6ImNrN28yZjE1ZzA0bnIzZG0zb2kxNzlrcHkifQ.J5-vlsAJMMenyqXyRoq32A";
    }

    public static String getDarkSkyToken() {
        return "https://api.forecast.io/forecast/a6b8c7b90a261e493e22279291026462/%s";
    }

    public static String[] getDays() {
        return days;
    }
}
