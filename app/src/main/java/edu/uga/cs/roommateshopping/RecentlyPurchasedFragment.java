package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.databinding.FragmentRecentlyPurchasedBinding;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class RecentlyPurchasedFragment extends Fragment {

    private static final String TAG = "RecentlyPurchased";
    private FragmentRecentlyPurchasedBinding binding;
    private DatabaseReference databaseReference;
    private List<ShoppingItem> purchasedItemList;
    private PurchasedItemAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecentlyPurchasedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        purchasedItemList = new ArrayList<>();
        adapter = new PurchasedItemAdapter(purchasedItemList);
        binding.recyclerViewRecentlyPurchased.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRecentlyPurchased.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("recently_purchased");

        databaseReference.addValueEventListener(new ValueEventListener() {
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
                } else {
                    binding.textViewEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
