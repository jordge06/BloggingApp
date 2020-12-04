package com.example.bloggingapp.NotificationPackage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.example.bloggingapp.R;
import com.example.bloggingapp.Adapter.NotificationAdapter;
import com.example.bloggingapp.Model.Notification;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private String currentUser;
    private FirebaseAuth firebaseAuth;
    private NotificationAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        setUpRecycler();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser().getUid();

        NotificationActivityViewModel notificationActivityViewModel = new ViewModelProvider(this)
                .get(NotificationActivityViewModel.class);
        notificationActivityViewModel.init(currentUser);

        if (firebaseAuth.getCurrentUser() != null) {
            notificationActivityViewModel.getNotification(currentUser)
                    .observe(NotificationActivity.this, new Observer<List<Notification>>() {
                        @Override
                        public void onChanged(List<Notification> notifications) {
                            notificationAdapter.setNotificationList(notifications);
                            notificationAdapter.notifyDataSetChanged();
                        }
                    });
        }

    }

    private void setUpRecycler() {
        RecyclerView rvNotification = findViewById(R.id.rvNotification);
        rvNotification.setLayoutManager(new LinearLayoutManager(NotificationActivity.this));
        notificationAdapter = new NotificationAdapter(NotificationActivity.this);
        rvNotification.setAdapter(notificationAdapter);
        notificationAdapter.notifyDataSetChanged();
    }
}