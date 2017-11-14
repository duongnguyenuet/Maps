package binary.maps.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.akexorcist.googledirection.model.Step;
import com.google.gson.Gson;

import java.util.List;

import binary.maps.R;

/**
 * Created by duong on 11/8/2017.
 */

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {
    private List<Step> stepList;
    private Context context;
    Gson gson = new Gson();

    public StepAdapter(List<Step> stepList) {
        this.stepList = stepList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View placeView = inflater.inflate(R.layout.item_instruction, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(placeView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Step step = stepList.get(position);
        TextView tvDirection = holder.tvDirection;
        tvDirection.setText(Html.fromHtml(Html.fromHtml(step.getHtmlInstruction()).toString()));

        TextView tvDuration = holder.tvDuration;
        tvDuration.setText(step.getDuration().getText());

        TextView tvDistance = holder.tvDistance;
        tvDistance.setText(step.getDistance().getText());
    }

    @Override
    public int getItemCount() {
        return stepList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDirection;
        public TextView tvDuration;
        public TextView tvDistance;

        public ViewHolder(View itemView) {
            super(itemView);

            tvDirection = itemView.findViewById(R.id.directions);
            tvDuration = itemView.findViewById(R.id.duration);
            tvDistance = itemView.findViewById(R.id.distance);
        }
    }
}
