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

    private List<ShoppingItem> purchasedItemList;

    public PurchasedItemAdapter(List<ShoppingItem> purchasedItemList) {
        this.purchasedItemList = purchasedItemList;
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
        holder.textViewPrice.setText("Price: $" + item.getPrice());
        holder.textViewPurchasedBy.setText("Purchased by: " + item.getPurchasedBy());

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.imageViewItem.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imageViewItem);
        } else {
            holder.imageViewItem.setVisibility(View.GONE);
        }
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
        ImageView imageViewItem;

        public PurchasedItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewPurchasedBy = itemView.findViewById(R.id.textViewPurchasedBy);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
        }
    }
}
