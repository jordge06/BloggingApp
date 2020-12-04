package com.example.bloggingapp.Repository;

import android.util.Log;

import com.example.bloggingapp.Model.Comment;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Model.Post;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import androidx.lifecycle.MutableLiveData;

public class Repository {

    private static final String TAG = "Repository";

    static Repository instance;

    private List<Post> postModel = new ArrayList<>();
    private List<Notification> notificationModel = new ArrayList<>();
    private List<Post> postByUser = new ArrayList<>();

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private MutableLiveData<List<Post>> postList = new MutableLiveData<>();
    private MutableLiveData<List<Notification>> notificationList = new MutableLiveData<>();
    private MutableLiveData<List<Post>> postByUserList = new MutableLiveData<>();

    public static Repository getInstance() {

        if (instance == null) {
            instance = new Repository();
        }
        return instance;
    }

    public MutableLiveData<List<Post>> getPostList() {
        if (postModel.size() == 0) {
            loadData();
        }
        return postList;
    }

    public MutableLiveData<List<Notification>> getNotificationList(String id) {
        if (notificationModel.size() == 0) {
            loadNotification(id);
        }
        return notificationList;
    }

    public MutableLiveData<List<Comment>> getCommentList(String blogPostId) {
        if (commentList.size() == 0) {
            loadComments(blogPostId);
        }
        return comments;
    }

    public MutableLiveData<List<Post>> getPostByUserList(String id) {
        if (postByUser.size() == 0) {
            loadNotification(id);
        }
        return postByUserList;
    }

    private void loadData() {
        Query firstDate = firebaseFirestore.collection("Posts")
                .orderBy("timeStamp", Query.Direction.DESCENDING);

        firstDate.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                } else {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                            if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                String postId = documentSnapshot.getDocument().getId();
                                Post post = documentSnapshot.getDocument().toObject(Post.class).withId(postId);
                                postModel.add(post);

                            }
                        }
                        postList.postValue(postModel);

                    }
                }
            }
        });
    }

    private void loadDataByUser(String id) {
        Query firstDate = firebaseFirestore.collection("Posts").whereEqualTo("userId", id)
                .orderBy("timeStamp", Query.Direction.DESCENDING);

        firstDate.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                } else {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                            if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                String postId = documentSnapshot.getDocument().getId();
                                Post post = documentSnapshot.getDocument().toObject(Post.class).withId(postId);
                                postByUser.add(post);

                            }
                        }
                        postByUserList.postValue(postByUser);

                    }
                }
            }
        });
    }

    private MutableLiveData<List<Comment>> comments = new MutableLiveData<>();
    private List<Comment> commentList = new ArrayList<>();

    private void loadComments(String blogPostId) {
        firebaseFirestore.collection("Posts/" + blogPostId + "/Comments")
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                        @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (!queryDocumentSnapshots.isEmpty()) {

                            for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                                if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                    String commentId = documentSnapshot.getDocument().getId();
                                    Comment comment = documentSnapshot.getDocument().toObject(Comment.class);
                                    commentList.add(comment);
                                }
                                comments.postValue(commentList);
                            }
                        }
                    }
                });
    }


    private void loadNotification(String id) {
        Query query = firebaseFirestore.collection("Notification").whereEqualTo("ownerId", id);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                } else {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        if (!notificationModel.isEmpty()) notificationModel.clear();

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                            if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                Notification notification = documentSnapshot.getDocument().toObject(Notification.class);
                                notificationModel.add(notification);

                            }
                        }
                        notificationList.postValue(notificationModel);

                    }
                }
            }
        });
    }




}
