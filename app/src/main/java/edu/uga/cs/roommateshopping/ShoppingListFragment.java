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

import edu.uga.cs.roommateshopping.databinding.FragmentShoppingListBinding;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class ShoppingListFragment extends Fragment implements ShoppingItemAdapter.OnItemClickListener {

    private static final String TAG = "ShoppingListFragment";
    private FragmentShoppingListBinding binding;
    private DatabaseReference databaseReference;
    private List<ShoppingItem> shoppingItemList;
    private ShoppingItemAdapter adapter;
    private FirebaseAuth mAuth;
    private ValueEventListener valueEventListener;

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

        ensureUserInRoommatesList();

        valueEventListener = new ValueEventListener() {
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
        };
        databaseReference.addValueEventListener(valueEventListener);

        FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAddDialog());
        }

        setupMenu();
    }

    private void ensureUserInRoommatesList() {
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            DatabaseReference roommatesRef = FirebaseDatabase.getInstance().getReference("roommates");
            roommatesRef.orderByValue().equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        roommatesRef.push().setValue(email);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "ensureUserInRoommatesList:onCancelled", error.toException());
                }
            });
        }
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
                } else if (menuItem.getItemId() == R.id.action_purchased) {
                    NavHostFragment.findNavController(ShoppingListFragment.this)
                            .navigate(R.id.action_ShoppingListFragment_to_PurchasedGroupsFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_roommates) {
                    NavHostFragment.findNavController(ShoppingListFragment.this)
                            .navigate(R.id.action_ShoppingListFragment_to_RoommatesFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Mark as Purchased");
        builder.setMessage("Enter the price for " + item.getName());

        final EditText priceInput = new EditText(requireContext());
        priceInput.setHint("Price");
        builder.setView(priceInput);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String price = priceInput.getText().toString();
            if (!price.isEmpty()) {
                item.setPrice(price);
                if (mAuth.getCurrentUser() != null) {
                    item.setPurchasedBy(mAuth.getCurrentUser().getEmail());
                }
                
                // Move to recently_purchased
                DatabaseReference purchasedRef = FirebaseDatabase.getInstance().getReference("recently_purchased");
                purchasedRef.push().setValue(item).addOnSuccessListener(aVoid -> {
                    // Remove from shopping_list
                    databaseReference.child(item.getKey()).removeValue();
                    Toast.makeText(getContext(), "Item purchased!", Toast.LENGTH_SHORT).show();
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
    public void onStop() {
        super.onStop();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
