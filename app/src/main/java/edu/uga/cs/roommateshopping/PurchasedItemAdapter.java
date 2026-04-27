/**
 * RecyclerView adapter for displaying items in the shopping basket.
 * Shows detailed item information including purchaser and date added.
 */
package edu.uga.cs.roommateshopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class PurchasedItemAdapter extends RecyclerView.Adapter<PurchasedItemAdapter.PurchasedItemHolder> {

    public interface OnItemClickListener {
        void onItemClick(ShoppingItem item);
    }

    private List<ShoppingItem> purchasedItemList;
    private OnItemClickListener listener;

    public PurchasedItemAdapter(List<ShoppingItem> purchasedItemList, OnItemClickListener listener) {
        this.purchasedItemList = purchasedItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PurchasedItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.purchased_item, parent, false);
        return new PurchasedItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchasedItemHolder holder, int position) {
        ShoppingItem item = purchasedItemList.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewQuantity.setText("Quantity: " + item.getQuantity());
        // holder.textViewPrice.setText("Price: $" + item.getPrice()); // Price removed per request
        holder.textViewPurchasedBy.setText("In basket: " + item.getPurchasedBy());
        holder.textViewDate.setText("Added to basket: " + (item.getPurchaseDate() != null ? item.getPurchaseDate() : "N/A"));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.imageViewItem.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imageViewItem);
        } else {
            holder.imageViewItem.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return purchasedItemList.size();
    }

    public static class PurchasedItemHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewQuantity;
        TextView textViewPrice;
        TextView textViewPurchasedBy;
        TextView textViewDate;
        ImageView imageViewItem;

        public PurchasedItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewPurchasedBy = itemView.findViewById(R.id.textViewPurchasedBy);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
        }
    }
}
