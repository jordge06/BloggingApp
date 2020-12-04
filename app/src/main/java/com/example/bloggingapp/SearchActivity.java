package com.example.bloggingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.example.bloggingapp.Adapter.UserAdapter;
import com.example.bloggingapp.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    private List<User> userList = new ArrayList<>();

    private UserAdapter userAdapter;

    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        EditText txtSearch = findViewById(R.id.txtSearch);
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
    }

    private void setUpRecycler() {
        RecyclerView rvUsers = findViewById(R.id.rvUsers);
        userAdapter = new UserAdapter(SearchActivity.this, userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
        rvUsers.setAdapter(userAdapter);
    }

    private void searchUser(String searchText) {

        firebaseFirestore.collection("Users").orderBy("lowerCaseName").startAt(searchText).endAt(searchText + "\uf8ff")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
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
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
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