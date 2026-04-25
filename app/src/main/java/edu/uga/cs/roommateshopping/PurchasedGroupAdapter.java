package edu.uga.cs.roommateshopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.uga.cs.roommateshopping.models.PurchaseGroup;

public class PurchasedGroupAdapter extends RecyclerView.Adapter<PurchasedGroupAdapter.PurchasedGroupHolder> {

    public interface OnItemClickListener {
        void onItemClick(PurchaseGroup group);
    }

    private List<PurchaseGroup> purchaseGroupList;
    private OnItemClickListener listener;

    public PurchasedGroupAdapter(List<PurchaseGroup> purchaseGroupList, OnItemClickListener listener) {
        this.purchaseGroupList = purchaseGroupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PurchasedGroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.purchased_group_item, parent, false);
        return new PurchasedGroupHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchasedGroupHolder holder, int position) {
        PurchaseGroup group = purchaseGroupList.get(position);
        holder.textViewPurchasedBy.setText("Purchased by: " + group.getPurchasedBy());
        holder.textViewTotalPrice.setText("Total: $" + String.format("%.2f", group.getTotalPrice()));
        
        StringBuilder itemsText = new StringBuilder("Items: ");
        if (group.getItems() != null) {
            for (int i = 0; i < group.getItems().size(); i++) {
                itemsText.append(group.getItems().get(i).getName());
                if (i < group.getItems().size() - 1) {
                    itemsText.append(", ");
                }
            }
        }
        holder.textViewItems.setText(itemsText.toString());
        holder.textViewSettled.setText("Status: " + (group.isSettled() ? "Settled" : "Not Settled"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return purchaseGroupList.size();
    }

    public static class PurchasedGroupHolder extends RecyclerView.ViewHolder {
        TextView textViewPurchasedBy;
        TextView textViewTotalPrice;
        TextView textViewItems;
        TextView textViewSettled;

        public PurchasedGroupHolder(@NonNull View itemView) {
            super(itemView);
            textViewPurchasedBy = itemView.findViewById(R.id.textViewPurchasedBy);
            textViewTotalPrice = itemView.findViewById(R.id.textViewTotalPrice);
            textViewItems = itemView.findViewById(R.id.textViewItems);
            textViewSettled = itemView.findViewById(R.id.textViewSettled);
        }
    }
}
