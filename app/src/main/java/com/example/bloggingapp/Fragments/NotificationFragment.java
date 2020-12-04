package com.example.bloggingapp.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bloggingapp.Adapter.NotificationAdapter;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.NotificationPackage.NotificationActivity;
import com.example.bloggingapp.NotificationPackage.NotificationActivityViewModel;
import com.example.bloggingapp.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationFragment extends Fragment {

    private NotificationAdapter notificationAdapter;
    private Context context;
    private RecyclerView rvNotification;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_notification, container, false);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String currentUser = firebaseAuth.getCurrentUser().getUid();

        rvNotification = view.findViewById(R.id.rvNotification);
        context = getContext();

        setUpRecycler();
        NotificationActivityViewModel notificationActivityViewModel = new ViewModelProvider(this)
                .get(NotificationActivityViewModel.class);
        notificationActivityViewModel.init(currentUser);

        if (firebaseAuth.getCurrentUser() != null) {
            notificationActivityViewModel.getNotification(currentUser)
                    .observe(getViewLifecycleOwner(), new Observer<List<Notification>>() {
                        @Override
                        public void onChanged(List<Notification> notifications) {
                            notificationAdapter.setNotificationList(notifications);
                            notificationAdapter.notifyDataSetChanged();
                        }
                    });
        }

        return view;
    }

    private void setUpRecycler() {
        rvNotification.setLayoutManager(new LinearLayoutManager(getActivity()));
        notificationAdapter = new NotificationAdapter(getActivity());
        rvNotification.setAdapter(notificationAdapter);
    }
}
