package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.databinding.FragmentRecentlyPurchasedBinding;
import edu.uga.cs.roommateshopping.models.PurchaseGroup;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class RecentlyPurchasedFragment extends Fragment implements PurchasedItemAdapter.OnItemClickListener {

    private static final String TAG = "RecentlyPurchased";
    private FragmentRecentlyPurchasedBinding binding;
    private DatabaseReference databaseReference;
    private List<ShoppingItem> purchasedItemList;
    private PurchasedItemAdapter adapter;
    private FirebaseAuth mAuth;
    private ValueEventListener valueEventListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecentlyPurchasedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        purchasedItemList = new ArrayList<>();
        adapter = new PurchasedItemAdapter(purchasedItemList, this);
        binding.recyclerViewRecentlyPurchased.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRecentlyPurchased.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("recently_purchased");

        binding.buttonCheckout.setOnClickListener(v -> checkout());

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) {
                    return;
                }

                purchasedItemList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ShoppingItem item = postSnapshot.getValue(ShoppingItem.class);
                    if (item != null) {
                        item.setKey(postSnapshot.getKey());
                        purchasedItemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (purchasedItemList.isEmpty()) {
                    binding.textViewEmpty.setVisibility(View.VISIBLE);
                    binding.buttonCheckout.setEnabled(false);
                } else {
                    binding.textViewEmpty.setVisibility(View.GONE);
                    binding.buttonCheckout.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        };
        databaseReference.addValueEventListener(valueEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    // Handle the checkout process for all items currently in the basket
    private void checkout() {
        if (purchasedItemList.isEmpty()) {
            Toast.makeText(getContext(), "Basket is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Checkout");
        builder.setMessage("Enter the total price for all items in the basket:");

        final android.widget.EditText priceInput = new android.widget.EditText(requireContext());
        priceInput.setHint("Total Price");
        priceInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(priceInput);

        builder.setPositiveButton("Checkout", (dialog, which) -> {
            String priceStr = priceInput.getText().toString();
            if (priceStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }

            double total;
            try {
                total = Double.parseDouble(priceStr);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Anonymous";
            PurchaseGroup group = new PurchaseGroup(userEmail, new ArrayList<>(purchasedItemList), total);

            DatabaseReference purchasedGroupsRef = FirebaseDatabase.getInstance().getReference("purchased_groups");
            purchasedGroupsRef.push().setValue(group).addOnSuccessListener(aVoid -> {
                databaseReference.removeValue(); // Clear recently_purchased
                Toast.makeText(getContext(), "Checked out successfully!", Toast.LENGTH_SHORT).show();
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // When an item in the basket is clicked, ask if it should be moved back to the shopping list
    @Override
    public void onItemClick(ShoppingItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove from Basket")
                .setMessage("Do you want to remove " + item.getName() + " from the basket and put it back on the shopping list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String oldKey = item.getKey();
                    if (oldKey == null) return;

                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance().getReference("shopping_list");

                    // Create a clean item to move back to the shopping list
                    ShoppingItem restoredItem = new ShoppingItem(item.getName(), item.getQuantity());
                    restoredItem.setImageUrl(item.getImageUrl());
                    // price and purchasedBy are null by default in constructor

                    shoppingListRef.push().setValue(restoredItem).addOnSuccessListener(aVoid -> {
                        databaseReference.child(oldKey).removeValue();
                        Toast.makeText(getContext(), "Item moved back to shopping list", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
