/**
 * Fragment that displays history of purchased item groups.
 * Provides functionality to settle costs among roommates and manage purchase history.
 */
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

import com.google.firebase.auth.FirebaseAuth;
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

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> allRoommates = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String email = postSnapshot.child("email").getValue(String.class);
                    if (email != null) {
                        allRoommates.add(email);
                    }
                }

                if (allRoommates.isEmpty()) {
                    // Fallback if no users found in 'users' node
                    settleWithCurrentPurchasers();
                    return;
                }

                performSettlement(allRoommates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                settleWithCurrentPurchasers();
            }
        });
    }

    private void settleWithCurrentPurchasers() {
        List<String> roommates = new ArrayList<>();
        for (PurchaseGroup group : purchaseGroupList) {
            String email = group.getPurchasedBy();
            if (!roommates.contains(email)) {
                roommates.add(email);
            }
        }
        performSettlement(roommates);
    }

    private void performSettlement(List<String> allRoommates) {
        Map<String, Double> expenses = new HashMap<>();
        double totalGlobal = 0;

        for (PurchaseGroup group : purchaseGroupList) {
            String email = group.getPurchasedBy();
            double price = group.getTotalPrice();
            totalGlobal += price;
            expenses.put(email, expenses.getOrDefault(email, 0.0) + price);
        }

        double average = totalGlobal / allRoommates.size();
        StringBuilder sb = new StringBuilder();
        sb.append("Total Expenses: $").append(String.format("%.2f", totalGlobal)).append("\n");
        sb.append("Number of Roommates: ").append(allRoommates.size()).append("\n");
        sb.append("Average per roommate: $").append(String.format("%.2f", average)).append("\n\n");

        for (String roommate : allRoommates) {
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
                .setPositiveButton("Settle & Clear", (dialog, which) -> {
                    databaseReference.removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Accounts settled and list cleared", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // If a group is clicked, give options to update price, restore items, or remove specific item
    @Override
    public void onItemClick(PurchaseGroup group) {
        String[] options = {"Update Total Price", "Remove Specific Item", "Remove Entire Group", "Cancel"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Purchase Group Actions")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Update Total Price
                            showUpdatePriceDialog(group);
                            break;
                        case 1: // Remove Specific Item
                            showRemoveItemDialog(group);
                            break;
                        case 2: // Remove Entire Group
                            confirmRemoveGroup(group);
                            break;
                    }
                })
                .show();
    }

    private void showUpdatePriceDialog(PurchaseGroup group) {
        final android.widget.EditText priceInput = new android.widget.EditText(requireContext());
        priceInput.setHint("New Total Price");
        priceInput.setText(String.valueOf(group.getTotalPrice()));
        priceInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Price")
                .setView(priceInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String priceStr = priceInput.getText().toString();
                    if (!priceStr.isEmpty()) {
                        try {
                            double newPrice = Double.parseDouble(priceStr);
                            group.setTotalPrice(newPrice);
                            databaseReference.child(group.getKey()).setValue(group);
                            Toast.makeText(getContext(), "Price updated", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveItemDialog(PurchaseGroup group) {
        List<ShoppingItem> items = group.getItems();
        if (items == null || items.isEmpty()) return;

        String[] itemNames = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemNames[i] = items.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Item to Remove")
                .setItems(itemNames, (dialog, which) -> {
                    ShoppingItem itemToRemove = items.remove(which);
                    
                    // Move back to shopping list
                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance().getReference("shopping_list");
                    ShoppingItem restoredItem = new ShoppingItem(itemToRemove.getName(), itemToRemove.getQuantity());
                    restoredItem.setImageUrl(itemToRemove.getImageUrl());
                    shoppingListRef.push().setValue(restoredItem);

                    // Update group in DB
                    if (items.isEmpty()) {
                        databaseReference.child(group.getKey()).removeValue();
                    } else {
                        databaseReference.child(group.getKey()).setValue(group);
                    }
                    Toast.makeText(getContext(), itemToRemove.getName() + " moved back to shopping list", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmRemoveGroup(PurchaseGroup group) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Purchased Group")
                .setMessage("Do you want to move all its items back to the shopping list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance().getReference("shopping_list");
                    List<ShoppingItem> items = group.getItems();
                    if (items != null) {
                        for (ShoppingItem item : items) {
                            ShoppingItem restoredItem = new ShoppingItem(item.getName(), item.getQuantity());
                            restoredItem.setImageUrl(item.getImageUrl());
                            shoppingListRef.push().setValue(restoredItem);
                        }
                    }
                    databaseReference.child(group.getKey()).removeValue();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    databaseReference.child(group.getKey()).removeValue();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}