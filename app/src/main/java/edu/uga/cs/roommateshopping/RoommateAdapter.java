/**
 * RecyclerView adapter for displaying registered roommates.
 */
package edu.uga.cs.roommateshopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.uga.cs.roommateshopping.models.Roommate;

public class RoommateAdapter extends RecyclerView.Adapter<RoommateAdapter.RoommateHolder> {

    private List<Roommate> roommateList;

    public RoommateAdapter(List<Roommate> roommateList) {
        this.roommateList = roommateList;
    }

    @NonNull
    @Override
    public RoommateHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roommate_item, parent, false);
        return new RoommateHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoommateHolder holder, int position) {
        Roommate roommate = roommateList.get(position);
        holder.textViewName.setText(roommate.getEmail());
    }

    @Override
    public int getItemCount() {
        return roommateList.size();
    }

    public static class RoommateHolder extends RecyclerView.ViewHolder {
        TextView textViewName;

        public RoommateHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewRoommateName);
        }
    }
}
