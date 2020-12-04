package com.example.bloggingapp.Model;

import com.google.firebase.firestore.Exclude;

import androidx.annotation.NonNull;

public class PostId {

    @Exclude
    public String postId;

    public <T extends PostId> T withId(@NonNull final String id) {
        this.postId = id;
        return (T) this;
    }
}
