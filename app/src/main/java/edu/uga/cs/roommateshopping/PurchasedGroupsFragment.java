package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.databinding.FragmentPurchasedGroupsBinding;
import edu.uga.cs.roommateshopping.models.PurchaseGroup;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class PurchasedGroupsFragment extends Fragment implements PurchasedGroupAdapter.OnItemClickListener {

    private static final String TAG = "PurchasedGroupsFragment";
    private FragmentPurchasedGroupsBinding binding;
    private DatabaseReference databaseReference;
    private List<PurchaseGroup> purchaseGroupList;
    private PurchasedGroupAdapter adapter;
    private ValueEventListener valueEventListener;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPurchasedGroupsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        purchaseGroupList = new ArrayList<>();
        adapter = new PurchasedGroupAdapter(purchaseGroupList, this);
        binding.recyclerViewPurchasedGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPurchasedGroups.setAdapter(adapter);

        setupMenu();

        databaseReference = FirebaseDatabase.getInstance().getReference("purchased_groups");

        valueEventListener = new ValueEventListener() {
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
                } else {
                    binding.textViewEmptyGroups.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        };
        databaseReference.addValueEventListener(valueEventListener);

        binding.buttonSettle.setOnClickListener(v -> settleCosts());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_shopping, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_logout) {
                    mAuth.signOut();
                    android.view.View fab = requireActivity().findViewById(R.id.fab);
                    if (fab != null) fab.setVisibility(android.view.View.GONE);
                    NavHostFragment.findNavController(PurchasedGroupsFragment.this)
                            .navigate(R.id.LoginFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_basket) {
                    NavHostFragment.findNavController(PurchasedGroupsFragment.this)
                            .navigate(R.id.action_PurchasedGroupsFragment_to_RecentlyPurchasedFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onItemClick(PurchaseGroup group) {
        // Requirements 12 and 13: Update price or remove item from group
        String[] options = {"Update Total Price", "Remove Item from Group", "Delete Group"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Group Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showUpdatePriceDialog(group);
            } else if (which == 1) {
                showRemoveItemDialog(group);
            } else if (which == 2) {
                databaseReference.child(group.getKey()).removeValue();
            }
        });
        builder.show();
    }

    private void showUpdatePriceDialog(PurchaseGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Price");
        final EditText input = new EditText(requireContext());
        input.setText(String.valueOf(group.getTotalPrice()));
        builder.setView(input);
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                double newPrice = Double.parseDouble(input.getText().toString());
                group.setTotalPrice(newPrice);
                databaseReference.child(group.getKey()).setValue(group);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showRemoveItemDialog(PurchaseGroup group) {
        if (group.getItems() == null || group.getItems().isEmpty()) {
            Toast.makeText(getContext(), "No items in group", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ShoppingItem> items = group.getItems();
        String[] itemNames = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemNames[i] = items.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select item to remove");
        builder.setItems(itemNames, (dialog, which) -> {
            ShoppingItem removedItem = items.remove(which);
            // Recalculate price? Requirement 13 says "Remove/cancel an item... (item was not really purchased)"
            // Usually this means the total price should be updated too.
            try {
                double itemPrice = Double.parseDouble(removedItem.getPrice());
                group.setTotalPrice(group.getTotalPrice() - itemPrice);
            } catch (Exception e) {}
            
            databaseReference.child(group.getKey()).setValue(group);
        });
        builder.show();
    }

    private void settleCosts() {
        // Requirement 14: Settle the cost of the purchased items
        double total = 0;
        for (PurchaseGroup group : purchaseGroupList) {
            if (!group.isSettled()) {
                total += group.getTotalPrice();
                group.setSettled(true);
                databaseReference.child(group.getKey()).setValue(group);
            }
        }
        Toast.makeText(getContext(), "Total settled: $" + String.format("%.2f", total), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
