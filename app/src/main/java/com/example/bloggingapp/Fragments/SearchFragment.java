package com.example.bloggingapp.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.bloggingapp.Adapter.UserAdapter;
import com.example.bloggingapp.Model.User;
import com.example.bloggingapp.R;
import com.example.bloggingapp.SearchActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchActivity";

    private List<User> userList = new ArrayList<>();

    private UserAdapter userAdapter;

    RecyclerView rvUsers;

    private FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_search, container, false);

        EditText txtSearch = view.findViewById(R.id.txtSearch);
        rvUsers = view.findViewById(R.id.rvUsers);
        firebaseFirestore = FirebaseFirestore.getInstance();

        setUpRecycler();
        if (txtSearch.getText().toString().equals("")) {
            readUsers();
        }

        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchUser(charSequence.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }

    private void setUpRecycler() {
        userAdapter = new UserAdapter(getActivity(), userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvUsers.setAdapter(userAdapter);
    }

    private void searchUser(String searchText) {

        firebaseFirestore.collection("Users").orderBy("lowerCaseName").startAt(searchText).endAt(searchText + "\uf8ff")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                        } else {
                            userList.clear();
                            userAdapter.notifyDataSetChanged();
                            if (!queryDocumentSnapshots.isEmpty()) {

                                for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                                    if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                        User user = documentSnapshot.getDocument().toObject(User.class);
                                        userList.add(user);

                                    }
                                }
                                userAdapter.notifyDataSetChanged();

                            }
                        }
                    }
                });
    }

    private void readUsers() {
        firebaseFirestore.collection("Users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                } else {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                            if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                User user = documentSnapshot.getDocument().toObject(User.class);
                                userList.add(user);

                            }
                        }
                        userAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
}
