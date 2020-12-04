package com.example.bloggingapp.NotificationPackage;

import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Repository.Repository;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationActivityViewModel extends ViewModel {

    private MutableLiveData<List<Notification>> notificationList;

    public void init(String id) {
        if (notificationList != null) {
            return;
        }
        notificationList = Repository.getInstance().getNotificationList(id);
    }

    public LiveData<List<Notification>> getNotification(String id) {
        return notificationList;
    }
}
