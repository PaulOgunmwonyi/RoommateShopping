package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uga.cs.roommateshopping.databinding.FragmentPurchasedGroupsBinding;
import edu.uga.cs.roommateshopping.models.PurchaseGroup;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class PurchasedGroupsFragment extends Fragment implements PurchasedGroupAdapter.OnItemClickListener {

    private static final String TAG = "PurchasedGroups";
    private FragmentPurchasedGroupsBinding binding;
    private DatabaseReference databaseReference;
    private List<PurchaseGroup> purchaseGroupList;
    private PurchasedGroupAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPurchasedGroupsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        purchaseGroupList = new ArrayList<>();
        adapter = new PurchasedGroupAdapter(purchaseGroupList, this);
        binding.recyclerViewPurchasedGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPurchasedGroups.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("purchased_groups");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                purchaseGroupList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    PurchaseGroup group = postSnapshot.getValue(PurchaseGroup.class);
                    if (group != null) {
                        group.setKey(postSnapshot.getKey());
                        purchaseGroupList.add(group);
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (purchaseGroupList.isEmpty()) {
                    binding.textViewEmptyGroups.setVisibility(View.VISIBLE);
                    binding.buttonSettle.setEnabled(false);
                } else {
                    binding.textViewEmptyGroups.setVisibility(View.GONE);
                    binding.buttonSettle.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        });

        binding.buttonSettle.setOnClickListener(v -> settleAccounts());
    }

    // Calculate how much everyone spent and figure out the balances for each roommate
    private void settleAccounts() {
        if (purchaseGroupList.isEmpty()) return;

        Map<String, Double> expenses = new HashMap<>();
        double totalGlobal = 0;
        List<String> roommates = new ArrayList<>();

        for (PurchaseGroup group : purchaseGroupList) {
            String email = group.getPurchasedBy();
            double price = group.getTotalPrice();
            totalGlobal += price;
            
            expenses.put(email, expenses.getOrDefault(email, 0.0) + price);
            if (!roommates.contains(email)) {
                roommates.add(email);
            }
        }

        if (roommates.isEmpty()) return;

        double average = totalGlobal / roommates.size();
        StringBuilder sb = new StringBuilder();
        sb.append("Total Expenses: $").append(String.format("%.2f", totalGlobal)).append("\n");
        sb.append("Average per roommate: $").append(String.format("%.2f", average)).append("\n\n");

        for (String roommate : roommates) {
            double spent = expenses.getOrDefault(roommate, 0.0);
            double balance = spent - average;
            sb.append(roommate).append(" spent $").append(String.format("%.2f", spent));
            if (balance >= 0) {
                sb.append(" (Receives $").append(String.format("%.2f", balance)).append(")\n");
            } else {
                sb.append(" (Owes $").append(String.format("%.2f", Math.abs(balance))).append(")\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Settle Accounts")
                .setMessage(sb.toString())
                .setPositiveButton("Mark All Settled", (dialog, which) -> {
                    for (PurchaseGroup group : purchaseGroupList) {
                        databaseReference.child(group.getKey()).child("settled").setValue(true);
                    }
                    Toast.makeText(getContext(), "All groups marked as settled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // If a group is clicked, give the option to undo the purchase and move items back to the list
    @Override
    public void onItemClick(PurchaseGroup group) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Purchased Group")
                .setMessage("Do you want to remove this group and move all its items back to the shopping list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String groupKey = group.getKey();
                    if (groupKey == null) return;

                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance().getReference("shopping_list");
                    
                    List<ShoppingItem> items = group.getItems();
                    if (items != null) {
                        for (ShoppingItem item : items) {
                            // Create clean items to move back
                            ShoppingItem restoredItem = new ShoppingItem(item.getName(), item.getQuantity());
                            restoredItem.setImageUrl(item.getImageUrl());
                            shoppingListRef.push().setValue(restoredItem);
                        }
                    }

                    databaseReference.child(groupKey).removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Group removed and items restored to shopping list", Toast.LENGTH_SHORT).show();
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