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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.databinding.FragmentRoommatesBinding;

public class RoommatesFragment extends Fragment {

    private static final String TAG = "RoommatesFragment";
    private FragmentRoommatesBinding binding;
    private DatabaseReference databaseReference;
    private List<String> roommateList;
    private RoommateAdapter adapter;
    private ValueEventListener valueEventListener;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRoommatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        roommateList = new ArrayList<>();
        adapter = new RoommateAdapter(roommateList);
        binding.recyclerViewRoommates.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRoommates.setAdapter(adapter);

        setupMenu();

        databaseReference = FirebaseDatabase.getInstance().getReference("roommates");

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;

                roommateList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String email = postSnapshot.getValue(String.class);
                    if (email != null) {
                        roommateList.add(email);
                    }
                }
                adapter.notifyDataSetChanged();

                if (roommateList.isEmpty()) {
                    binding.textViewEmptyRoommates.setVisibility(View.VISIBLE);
                } else {
                    binding.textViewEmptyRoommates.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadRoommates:onCancelled", error.toException());
            }
        };
        databaseReference.addValueEventListener(valueEventListener);
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
                    NavHostFragment.findNavController(RoommatesFragment.this)
                            .navigate(R.id.LoginFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_basket) {
                    NavHostFragment.findNavController(RoommatesFragment.this)
                            .navigate(R.id.action_RoommatesFragment_to_RecentlyPurchasedFragment);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_purchased) {
                    NavHostFragment.findNavController(RoommatesFragment.this)
                            .navigate(R.id.action_RoommatesFragment_to_PurchasedGroupsFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
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
