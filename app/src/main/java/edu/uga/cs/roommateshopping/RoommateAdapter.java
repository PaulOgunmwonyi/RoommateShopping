package edu.uga.cs.roommateshopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoommateAdapter extends RecyclerView.Adapter<RoommateAdapter.RoommateHolder> {

    private List<String> roommateList;

    public RoommateAdapter(List<String> roommateList) {
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
        String email = roommateList.get(position);
        holder.textViewEmail.setText(email);
    }

    @Override
    public int getItemCount() {
        return roommateList.size();
    }

    public static class RoommateHolder extends RecyclerView.ViewHolder {
        TextView textViewEmail;

        public RoommateHolder(@NonNull View itemView) {
            super(itemView);
            textViewEmail = itemView.findViewById(R.id.textViewRoommateEmail);
        }
    }
}
