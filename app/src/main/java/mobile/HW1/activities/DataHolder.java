package mobile.HW1.activities;

import android.location.Location;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

public class DataHolder {
    private CarmenFeature data;
    private Location location;

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
}
