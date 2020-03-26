package mobile.HW1.classes.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;

import java.util.ArrayList;

import mobile.HW1.R;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.AddressViewHolder> {

    private final View.OnClickListener mOnClickListener;
    private ArrayList<CarmenFeature> mDataSet;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class AddressViewHolder extends RecyclerView.ViewHolder {
        // each data item
        private TextView title;
        private TextView details;
        // divider Line.
        private View line;

        AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.address_title);
            details = itemView.findViewById(R.id.address_detail);
            line = itemView.findViewById(R.id.divider);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataSet)
    public PlaceAdapter(ArrayList<CarmenFeature> myDataset, View.OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
        mDataSet = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(ViewGroup parent,
                                                int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.address, parent, false);

        v.setOnClickListener(mOnClickListener);

        return new AddressViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
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

    }

    // Return the size of your dataSet (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void reset() {

        while (mDataSet.size() > 0) {
            mDataSet.remove(0);
            notifyItemRemoved(0);
            notifyItemRangeRemoved(0, mDataSet.size());
        }

    }


}


