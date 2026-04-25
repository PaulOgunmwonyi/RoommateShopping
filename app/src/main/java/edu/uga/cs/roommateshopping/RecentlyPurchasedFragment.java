package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.fragment.NavHostFragment;
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

        setupMenu();

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
                    NavHostFragment.findNavController(RecentlyPurchasedFragment.this)
                            .navigate(R.id.LoginFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_purchased) {
                    NavHostFragment.findNavController(RecentlyPurchasedFragment.this)
                            .navigate(R.id.action_RecentlyPurchasedFragment_to_PurchasedGroupsFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void checkout() {
        if (purchasedItemList.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "Basket is empty", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        double total = 0;
        for (ShoppingItem item : purchasedItemList) {
            try {
                total += Double.parseDouble(item.getPrice());
            } catch (NumberFormatException e) {
                // ignore or handle
            }
        }

        String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Anonymous";
        PurchaseGroup group = new PurchaseGroup(userEmail, new ArrayList<>(purchasedItemList), total);

        DatabaseReference purchasedGroupsRef = FirebaseDatabase.getInstance().getReference("purchased_groups");
        purchasedGroupsRef.push().setValue(group).addOnSuccessListener(aVoid -> {
            databaseReference.removeValue(); // Clear recently_purchased
            android.widget.Toast.makeText(getContext(), "Checked out successfully!", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onItemClick(ShoppingItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove from Basket")
                .setMessage("Do you want to remove " + item.getName() + " from the basket and put it back on the shopping list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance().getReference("shopping_list");
                    // Clear price and purchaser before putting back
                    item.setPrice(null);
                    item.setPurchasedBy(null);
                    shoppingListRef.push().setValue(item).addOnSuccessListener(aVoid -> {
                        databaseReference.child(item.getKey()).removeValue();
                        android.widget.Toast.makeText(getContext(), "Item moved back to shopping list", android.widget.Toast.LENGTH_SHORT).show();
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
