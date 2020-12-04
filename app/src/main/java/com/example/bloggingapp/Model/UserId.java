package com.example.bloggingapp.Model;

import com.google.firebase.firestore.Exclude;

import androidx.annotation.NonNull;

public class UserId {
    @Exclude
    public String userId;

    public <T extends UserId> T withId(@NonNull final String id) {
        this.userId = id;
        return (T) this;
    }
}
