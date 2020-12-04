package com.example.bloggingapp.MainActivityPackage;

import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.Repository.Repository;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainActivityViewModel extends ViewModel {

    private MutableLiveData<List<Post>> postList;

    public void init() {
        if (postList != null) {
            return;
        }
        postList = Repository.getInstance().getPostList();
    }

    public LiveData<List<Post>> getPosts() {
        return postList;
    }

}
