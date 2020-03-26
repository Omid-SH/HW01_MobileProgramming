package mobile.HW1.classes.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import mobile.HW1.R;


public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final View.OnClickListener mOnClickListener;
    private JSONArray mDataSet;


    public void setmDataSet(JSONArray mDataSet) {
        this.mDataSet = mDataSet;
    }

    public JSONArray getmDataSet() {
        return mDataSet;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class DayViewHolder extends RecyclerView.ViewHolder {

        // each item data ...
        private TextView dayName;
        private ImageView dayIcon;
        private TextView minTemp;
        private TextView maxTemp;

        DayViewHolder(@NonNull View itemView) {

            super(itemView);
            dayName = itemView.findViewById(R.id.day_name);
            dayIcon = itemView.findViewById(R.id.day_icon);
            minTemp = itemView.findViewById(R.id.day_min_temperature);
            maxTemp = itemView.findViewById(R.id.day_max_temperature);

        }
    }


    // Provide a suitable constructor (depends on the kind of dataSet)
    public DayAdapter(View.OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // create a new View
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.each_day, parent, false);

        v.setOnClickListener(mOnClickListener);

        return new DayAdapter.DayViewHolder(v);
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {

        Log.v("TAG", String.valueOf(position));

        // - get element from your dataSet at this position
        // - replace the contents of the view with that element

        try {

            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};


            JSONObject item = mDataSet.getJSONObject(position);

            String temperatureMax = item.getString("temperatureMax");
            String temperatureMin = item.getString("temperatureMin");
            String icon = item.getString("icon");

            temperatureMax = String.valueOf(Math.round(Double.parseDouble(temperatureMax)));
            temperatureMin = String.valueOf(Math.round(Double.parseDouble(temperatureMin)));

            holder.dayName.setText(days[(today + position) % 7]);
            holder.maxTemp.setText(temperatureMax);
            holder.minTemp.setText(temperatureMin);

            //set icon
            switch (icon) {

                case "snow":
                    holder.dayIcon.setImageResource(R.drawable.ic_snowy);
                    break;

                case "clear-day":
                case "clear-night":
                case "clear":
                    holder.dayIcon.setImageResource(R.drawable.ic_sunny);
                    break;

                case "rain":
                    holder.dayIcon.setImageResource(R.drawable.ic_rainy);
                    break;

                case "sleet":
                    holder.dayIcon.setImageResource(R.drawable.ic_sleet);
                    break;

                case "wind":
                    holder.dayIcon.setImageResource(R.drawable.ic_windy);
                    break;

                case "fog":
                    holder.dayIcon.setImageResource(R.drawable.ic_fog);
                    break;

                default:
                    holder.dayIcon.setImageResource(R.drawable.ic_cloudy);
                    break;
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    /*// Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(AddressViewHolder holder, int position) {

        // - get element from your dataSet at this position
        // - replace the contents of the view with that element

        String detail = mDataSet.get(position).placeName();
        String title = (detail != null ? detail.split(",") : new String[]{""})[0];

        holder.details.setText(detail);
        holder.title.setText(title);

        // beautiful UI!
        if (position == getItemCount() - 1) {
            holder.line.setVisibility(View.GONE);
        }

    }*/

    public JSONObject getBottomBarData(int itemPosition) {

        String[] results = new String[3];


        try {
            JSONObject item = mDataSet.getJSONObject(itemPosition);
            return item;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }


    @Override
    public int getItemCount() {
        if (mDataSet == null) {
            return 0;
        } else {
            return mDataSet.length();
        }
    }
}