package com.example.bloggingapp.Interface;

import android.widget.ImageView;

public interface BlogAdapterInterface {

    void openComments(String postId, String userId);

    void likePost(final String postId, final String currentUser, final String userId, final ImageView btnLike);

    void openProfile(String id);

    void option(int position, String postId, String userId, ImageView view);

}
