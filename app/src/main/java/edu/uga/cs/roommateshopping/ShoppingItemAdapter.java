package edu.uga.cs.roommateshopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import android.widget.CheckBox;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ShoppingItemHolder> {

    private List<ShoppingItem> shoppingItemList;
    private OnItemClickListener listener;
    private boolean selectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();

    public interface OnItemClickListener {
        void onPurchaseClick(ShoppingItem item);
        void onItemClick(ShoppingItem item);
    }

    public ShoppingItemAdapter(List<ShoppingItem> shoppingItemList, OnItemClickListener listener) {
        this.shoppingItemList = shoppingItemList;
        this.listener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    @NonNull
    @Override
    public ShoppingItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shopping_item, parent, false);
        return new ShoppingItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingItemHolder holder, int position) {
        ShoppingItem item = shoppingItemList.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewQuantity.setText("Quantity: " + item.getQuantity());

        if (selectionMode) {
            holder.checkBoxSelect.setVisibility(View.VISIBLE);
            holder.checkBoxSelect.setChecked(selectedPositions.contains(position));
            holder.buttonPurchase.setVisibility(View.GONE);
            
            holder.itemView.setOnClickListener(v -> {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            });
            holder.checkBoxSelect.setOnClickListener(v -> {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            });
        } else {
            holder.checkBoxSelect.setVisibility(View.GONE);
            holder.buttonPurchase.setVisibility(View.VISIBLE);
            holder.buttonPurchase.setOnClickListener(v -> {
                if (listener != null) listener.onPurchaseClick(item);
            });
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

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
        return shoppingItemList.size();
    }

    public static class ShoppingItemHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewQuantity;
        Button buttonPurchase;
        ImageView imageViewItem;
        CheckBox checkBoxSelect;

        public ShoppingItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity);
            buttonPurchase = itemView.findViewById(R.id.buttonPurchase);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
            checkBoxSelect = itemView.findViewById(R.id.checkBoxSelect);
        }
    }
}
