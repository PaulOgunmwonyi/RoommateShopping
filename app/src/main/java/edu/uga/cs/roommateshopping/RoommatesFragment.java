package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.databinding.FragmentRoommatesBinding;
import edu.uga.cs.roommateshopping.models.Roommate;

public class RoommatesFragment extends Fragment {

    private static final String TAG = "RoommatesFragment";
    private FragmentRoommatesBinding binding;
    private DatabaseReference usersRef;
    private List<Roommate> roommateList;
    private RoommateAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRoommatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roommateList = new ArrayList<>();
        adapter = new RoommateAdapter(roommateList);
        binding.recyclerViewRoommates.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRoommates.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Make sure the current user is added to the users list if they aren't already
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).child("email").setValue(currentUser.getEmail());
        }

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                roommateList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Roommate roommate = postSnapshot.getValue(Roommate.class);
                    if (roommate != null) {
                        roommateList.add(roommate);
                    }
                }
                adapter.notifyDataSetChanged();

                if (roommateList.isEmpty()) {
                    binding.textViewNoRoommates.setVisibility(View.VISIBLE);
                } else {
                    binding.textViewNoRoommates.setVisibility(View.GONE);
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
