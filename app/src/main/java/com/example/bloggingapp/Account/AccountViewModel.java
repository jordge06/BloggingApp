package com.example.bloggingapp.Account;

import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.Repository.Repository;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AccountViewModel extends ViewModel {

    private MutableLiveData<List<Post>> postList;

    public void init(String id) {
        if (postList != null) {
            return;
        }
        postList = Repository.getInstance().getPostByUserList(id);
    }

    public LiveData<List<Post>> getPostByUser() {
        return postList;
    }
}
