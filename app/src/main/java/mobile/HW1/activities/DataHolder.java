package mobile.HW1.activities;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

public class DataHolder {
    private CarmenFeature data;

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
