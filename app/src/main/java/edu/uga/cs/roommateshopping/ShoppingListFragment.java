package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.uga.cs.roommateshopping.databinding.FragmentShoppingListBinding;
import edu.uga.cs.roommateshopping.models.PurchaseGroup;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class ShoppingListFragment extends Fragment implements ShoppingItemAdapter.OnItemClickListener {

    private static final String TAG = "ShoppingListFragment";
    private FragmentShoppingListBinding binding;
    private DatabaseReference databaseReference;
    private List<ShoppingItem> shoppingItemList;
    private ShoppingItemAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShoppingListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        shoppingItemList = new ArrayList<>();
        adapter = new ShoppingItemAdapter(shoppingItemList, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("shopping_list");

        binding.buttonConfirmSelection.setOnClickListener(v -> {
            Set<Integer> selected = adapter.getSelectedPositions();
            if (!selected.isEmpty()) {
                showMultiPurchaseDialog(selected);
            } else {
                Toast.makeText(getContext(), "No items selected", Toast.LENGTH_SHORT).show();
            }
        });

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shoppingItemList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ShoppingItem item = postSnapshot.getValue(ShoppingItem.class);
                    if (item != null) {
                        item.setKey(postSnapshot.getKey());
                        shoppingItemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        });

        FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAddDialog());
        }

        setupMenu();
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_shopping, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_logout) {
                    mAuth.signOut();
                    FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
                    if (fab != null) fab.setVisibility(View.GONE);
                    NavHostFragment.findNavController(ShoppingListFragment.this)
                            .navigate(R.id.LoginFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_basket) {
                    NavHostFragment.findNavController(ShoppingListFragment.this)
                            .navigate(R.id.action_ShoppingListFragment_to_RecentlyPurchasedFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_purchased || menuItem.getItemId() == R.id.action_roommates) {
                    NavHostFragment.findNavController(ShoppingListFragment.this)
                            .navigate(R.id.action_ShoppingListFragment_to_PurchasedGroupsFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_select_mode) {
                    toggleSelectionMode(menuItem);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void toggleSelectionMode(MenuItem menuItem) {
        if (adapter.isSelectionMode()) {
            // Confirm purchase for selected items
            Set<Integer> selected = adapter.getSelectedPositions();
            if (!selected.isEmpty()) {
                showMultiPurchaseDialog(selected);
            } else {
                adapter.setSelectionMode(false);
                menuItem.setTitle("Select Items");
                binding.buttonConfirmSelection.setVisibility(View.GONE);
            }
        } else {
            adapter.setSelectionMode(true);
            menuItem.setTitle("Cancel Selection");
            binding.buttonConfirmSelection.setVisibility(View.VISIBLE);
        }
    }

    private void showMultiPurchaseDialog(Set<Integer> selectedPositions) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Move to Basket")
                .setMessage("Do you want to move " + selectedPositions.size() + " selected items to the basket?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    List<ShoppingItem> selectedItems = new ArrayList<>();
                    List<Integer> sortedPositions = new ArrayList<>(selectedPositions);
                    java.util.Collections.sort(sortedPositions, java.util.Collections.reverseOrder());

                    for (int pos : sortedPositions) {
                        selectedItems.add(shoppingItemList.get(pos));
                    }

                    DatabaseReference recentlyPurchasedRef = FirebaseDatabase.getInstance().getReference("recently_purchased");
                    String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Anonymous";

                    for (ShoppingItem item : selectedItems) {
                        String oldKey = item.getKey();
                        item.setPurchasedBy(userEmail);
                        item.setPrice("0"); // Default price in basket, can be updated later or at checkout

                        recentlyPurchasedRef.push().setValue(item).addOnSuccessListener(aVoid -> {
                            databaseReference.child(oldKey).removeValue();
                        });
                    }

                    Toast.makeText(getContext(), selectedItems.size() + " items moved to basket", Toast.LENGTH_SHORT).show();
                    adapter.setSelectionMode(false);
                    binding.buttonConfirmSelection.setVisibility(View.GONE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    adapter.setSelectionMode(false);
                    binding.buttonConfirmSelection.setVisibility(View.GONE);
                })
                .show();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Item");
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(view);

        final EditText nameInput = view.findViewById(R.id.editTextItemName);
        final EditText quantityInput = view.findViewById(R.id.editTextItemQuantity);
        final EditText imageUrlInput = view.findViewById(R.id.editTextItemImageUrl);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String quantity = quantityInput.getText().toString().trim();
            String imageUrl = imageUrlInput.getText().toString().trim();
            if (!name.isEmpty()) {
                // Duplicate check
                boolean isDuplicate = false;
                for (ShoppingItem existingItem : shoppingItemList) {
                    if (existingItem.getName().equalsIgnoreCase(name)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Duplicate Item")
                            .setMessage("An item with the name '" + name + "' already exists in the list. Do you still want to add it?")
                            .setPositiveButton("Yes", (dialog1, which1) -> {
                                ShoppingItem item = new ShoppingItem(name, quantity);
                                if (!imageUrl.isEmpty()) {
                                    item.setImageUrl(imageUrl);
                                }
                                databaseReference.push().setValue(item);
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    ShoppingItem item = new ShoppingItem(name, quantity);
                    if (!imageUrl.isEmpty()) {
                        item.setImageUrl(imageUrl);
                    }
                    databaseReference.push().setValue(item);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onPurchaseClick(ShoppingItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Add to Basket")
                .setMessage("Do you want to add " + item.getName() + " to the basket?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (mAuth.getCurrentUser() != null) {
                        item.setPurchasedBy(mAuth.getCurrentUser().getEmail());
                    }
                    item.setPrice("0"); // Default price, can be set in basket

                    DatabaseReference recentlyPurchasedRef = FirebaseDatabase.getInstance().getReference("recently_purchased");
                    recentlyPurchasedRef.push().setValue(item).addOnSuccessListener(aVoid -> {
                        databaseReference.child(item.getKey()).removeValue();
                        Toast.makeText(getContext(), item.getName() + " added to basket!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onItemClick(ShoppingItem item) {
        // Show update/delete dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update or Delete Item");
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(view);

        final EditText nameInput = view.findViewById(R.id.editTextItemName);
        final EditText quantityInput = view.findViewById(R.id.editTextItemQuantity);
        final EditText imageUrlInput = view.findViewById(R.id.editTextItemImageUrl);

        nameInput.setText(item.getName());
        quantityInput.setText(item.getQuantity());
        imageUrlInput.setText(item.getImageUrl());

        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String quantity = quantityInput.getText().toString();
            String imageUrl = imageUrlInput.getText().toString();
            if (!name.isEmpty()) {
                item.setName(name);
                item.setQuantity(quantity);
                item.setImageUrl(imageUrl);
                databaseReference.child(item.getKey()).setValue(item);
            }
        });
        builder.setNeutralButton("Delete", (dialog, which) -> {
            databaseReference.child(item.getKey()).removeValue();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
