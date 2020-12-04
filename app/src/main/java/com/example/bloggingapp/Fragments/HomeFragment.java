package com.example.bloggingapp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.example.bloggingapp.Adapter.BlogAdapter;
import com.example.bloggingapp.Adapter.BlogFirebaseAdapter;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Interface.BlogAdapterInterface;
import com.example.bloggingapp.Interface.MainAppInterface;
import com.example.bloggingapp.LoginActivity;
import com.example.bloggingapp.MainActivityPackage.MainActivity;
import com.example.bloggingapp.MainActivityPackage.MainActivityViewModel;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment implements BlogFirebaseAdapter.BlogFirebaseRecyclerInterface {

    private static final String TAG = "HomeFragment";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private BlogAdapter blogAdapter;
    private BlogFirebaseAdapter blogFirebaseAdapter;
    private LinearLayoutManager linearLayoutManager;

    private RecyclerView rvPost;

    private List<Post> postList = new ArrayList<>();
    private String id;
    Parcelable state;

    private Context context;
    private MainAppInterface mainAppInterface;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainAppInterface = (MainAppInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvPost = view.findViewById(R.id.rvPost);

        context = getContext();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        CoordinatorLayout coordinatorLayout = view.findViewById(R.id.parentHolder);
        getArgs(coordinatorLayout);
        setUpRecycler();

        if (firebaseAuth.getCurrentUser() != null) {
            id = firebaseAuth.getCurrentUser().getUid();
        }

        return view;
    }

    private void getViewModelData() {
        MainActivityViewModel mainViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mainViewModel.init();

        if (firebaseAuth.getCurrentUser() != null) {
            id = firebaseAuth.getCurrentUser().getUid();

            mainViewModel.getPosts().observe(getViewLifecycleOwner(), new Observer<List<Post>>() {
                @Override
                public void onChanged(List<Post> posts) {
                    postList.addAll(posts);
                    blogAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void getArgs(CoordinatorLayout coordinatorLayout) {
        if (this.getArguments() != null) {
            String warningMessage = getArguments().getString("POST_MESSAGE");
            if (warningMessage != null) {
                Snackbar.make(coordinatorLayout,
                        warningMessage,
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setUpRecycler() {
        Query firstDate = firebaseFirestore.collection("Posts")
                .orderBy("timeStamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(firstDate, Post.class)
                .build();

        linearLayoutManager = new LinearLayoutManager(context);
        blogFirebaseAdapter = new BlogFirebaseAdapter(options, context);
        blogFirebaseAdapter.setBlogFirebaseRecyclerInterface(this);
        rvPost.setHasFixedSize(true);
        rvPost.setLayoutManager(linearLayoutManager);
        rvPost.setAdapter(blogFirebaseAdapter);
        linearLayoutManager.onRestoreInstanceState(state);
        rvPost.setSaveEnabled(true);
    }

    @Override
    public void likePost(final String postId, final String currentUser, final String userId, final ImageView btnLike) {

        firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" + postId + "/" + Constants.LIKE_COLLECTION_NAME)
                .document(currentUser).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult() != null && task.getResult().exists()) {
                    firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" +
                            postId + "/" + Constants.LIKE_COLLECTION_NAME)
                            .document(currentUser).delete();
                    deleteNotification(userId, postId);
                } else {
                    Map<String, Object> likesMap = new HashMap<>();
                    likesMap.put("timeStamp", FieldValue.serverTimestamp());

                    firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" +
                            postId + "/" + Constants.LIKE_COLLECTION_NAME)
                            .document(currentUser).set(likesMap);

                    addNotification(currentUser, userId, postId);
                }
            }
        });
    }

    private void addNotification(String currentUser, String userId, String blogId) {
        if (!userId.equals(id)) {
            Notification notification = new Notification(currentUser, Constants.LIKE_NOTIFICATION_TEXT,
                    userId, blogId, true);
            firebaseFirestore.collection("Notification").add(notification);
        }
    }

    private void deleteNotification(String userId, String blogId) {
        if (!userId.equals(id)) {
            firebaseFirestore.collection(Constants.NOTIFICATION_COLLECTION_NAME).whereEqualTo("currentUser", id)
                    .whereEqualTo("blogId", blogId).whereEqualTo("text", Constants.LIKE_NOTIFICATION_TEXT).get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    document.getReference().delete();
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void openProfile(String id) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.USER_ID_STRING_EXTRA, id);
        mainAppInterface.gotoProfile(bundle);
    }

    @Override
    public void option(final int position, ImageView view) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.post_option_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        blogFirebaseAdapter.deletePost(position);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();

    }

    private void deleteOtherCollection(String postId, String item) {
        firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +"/" + postId + "/" + item).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete();
                            }
                        }
                    }
                });
    }

    @Override
    public void openComments(String postId, String userId) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BLOG_ID_STRING_TEXT, postId);
        bundle.putString(Constants.USER_ID_STRING_EXTRA, userId);
        mainAppInterface.gotoComment(bundle);
        //navController.navigate(R.id.action_nav_home_to_commentFragment, bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        blogFirebaseAdapter.startListening();
        if (firebaseAuth.getCurrentUser() == null) {
            openLogin();
        } else {
            String userId = firebaseAuth.getCurrentUser().getUid();

            firebaseFirestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            //navController.navigate(R.id.action_nav_home_to_nav_profile);
                        }
                    } else {
                        String error = task.getException().getMessage();
                        Log.d(TAG, "onComplete: " + error);
                    }
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        blogFirebaseAdapter.stopListening();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        state = linearLayoutManager.onSaveInstanceState();
    }

    private void logout() {
        firebaseAuth.signOut();
        openLogin();
    }

    private void openLogin() {
        startActivity(new Intent(context, LoginActivity.class));
    }
}
