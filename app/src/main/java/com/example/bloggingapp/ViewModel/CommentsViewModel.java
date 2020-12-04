package com.example.bloggingapp.ViewModel;

import com.example.bloggingapp.Model.Comment;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Repository.Repository;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CommentsViewModel extends ViewModel {
    private MutableLiveData<List<Comment>> commentList;

    public void init(String blogPostId) {
        if (commentList != null) {
            return;
        }
        commentList = Repository.getInstance().getCommentList(blogPostId);
    }

    public LiveData<List<Comment>> getComments(String bloPostId) {
        return commentList;
    }
}
