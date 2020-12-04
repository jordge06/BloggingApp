package com.example.bloggingapp.MainActivityPackage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.example.bloggingapp.AccountSetupActivity;
import com.example.bloggingapp.Adapter.BlogFirebaseAdapter;
import com.example.bloggingapp.CommentActivity;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.LoginActivity;
import com.example.bloggingapp.Model.Token;
import com.example.bloggingapp.NewPostActivity;
import com.example.bloggingapp.NotificationPackage.NotificationActivity;
import com.example.bloggingapp.R;
import com.example.bloggingapp.SearchActivity;
import com.example.bloggingapp.Adapter.BlogAdapter;
import com.example.bloggingapp.Interface.BlogAdapterInterface;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.Service.LikePostWorker;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements BlogFirebaseAdapter.BlogFirebaseRecyclerInterface {

    private static final String TAG = "MainActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private BlogAdapter blogAdapter;
    private BlogFirebaseAdapter blogFirebaseAdapter;

    private List<Post> postList = new ArrayList<>();
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton btnAddPost = findViewById(R.id.btnAddPost);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        String pendingMessage = getIntent().getStringExtra("POST_MESSAGE");
        if (pendingMessage != null) {
            Snackbar.make(btnAddPost,
                    pendingMessage,
                    Snackbar.LENGTH_LONG).show();
        }

        // Get Token then save in Firebase
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        String token = task.getResult().getToken();
                        saveToken(token);
                    }
                }
            }
        });

        btnAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NewPostActivity.class));
            }
        });

        //setUpRecycler();
        setUpFirebaseRecycler();

        if (firebaseAuth.getCurrentUser() != null) {
            id = firebaseAuth.getCurrentUser().getUid();
        }
    }


    private void viewModelData() {
        MainActivityViewModel mainViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mainViewModel.init();

        mainViewModel.getPosts().observe(this, new Observer<List<Post>>() {
            @Override
            public void onChanged(List<Post> posts) {
                if (!postList.isEmpty()) postList.clear();

                postList.addAll(posts);
                blogAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void option(final int position, ImageView view) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
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

    // Save Token to Firebase
    private void saveToken(final String token) {
        Token token1 = new Token(token);
        firebaseFirestore.collection("Tokens").document(id).set(token1)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "onComplete: Token Saved");
                    }
                });
    }

    private void setUpFirebaseRecycler() {
        Query firstDate = firebaseFirestore.collection("Posts")
                .orderBy("timeStamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(firstDate, Post.class)
                .build();

        blogFirebaseAdapter = new BlogFirebaseAdapter(options, MainActivity.this);
        blogFirebaseAdapter.setBlogFirebaseRecyclerInterface(this);
        RecyclerView rvPost = findViewById(R.id.rvPost);
        rvPost.setHasFixedSize(true);
        rvPost.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvPost.setAdapter(blogFirebaseAdapter);

    }

    private void setUpRecycler() {
        RecyclerView rvPost = findViewById(R.id.rvPost);
        blogAdapter = new BlogAdapter(postList, MainActivity.this);
        rvPost.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvPost.setAdapter(blogAdapter);
    }

    @Override
    public void likePost(final String postId, final String currentUser, final String userId, final ImageView btnLike) {

        final Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        final Data data = new Data.Builder()
                .putString("POST_ID", postId)
                .putString("USER_ID", userId)
                .putString("CURRENT_USER", currentUser)
                .build();

        final OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(LikePostWorker.class)
                .setConstraints(constraints)
                .setInputData(data)
                .build();

        WorkManager.getInstance(MainActivity.this).enqueue(oneTimeWorkRequest);

    }

//    private void checkLikeStatus(ImageView imgLike) {
//
//        if (Objects.equals(imgLike.getBackground().getConstantState(),
//                MainActivity.this.getResources().getDrawable(R.drawable.ic_unlike).getConstantState())) {
//            imgLike.setBackgroundResource(R.drawable.ic_like);
//        } else {
//            imgLike.setBackgroundResource(R.drawable.ic_unlike);
//        }
//
//    }

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
                            if (task.isSuccessful()) {
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
        Intent intent = new Intent(MainActivity.this, AccountSetupActivity.class);
        intent.putExtra(Constants.USER_ID_STRING_EXTRA, id);
        startActivity(intent);
    }

    private void deletePost(final int position, String postId) {
        firebaseFirestore.collection(Constants.POST_COLLECTION_NAME).document(postId).delete();

        deleteOtherCollection(postId, Constants.COMMENT_COLLECTION_NAME);
        deleteOtherCollection(postId, Constants.LIKE_COLLECTION_NAME);
        deleteAllNotification(postId);

        postList.remove(position);
        blogAdapter.notifyItemRemoved(position);
        blogAdapter.notifyItemRangeChanged(position, postList.size());
    }

    private void deleteAllNotification(String postId) {
        firebaseFirestore.collection(Constants.NOTIFICATION_COLLECTION_NAME).whereEqualTo("blogId", postId)
                .get()
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

    private void deleteOtherCollection(String postId, String item) {
        firebaseFirestore.collection(Constants.POST_COLLECTION_NAME + "/" + postId + "/" + item).get()
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
        Intent intent = new Intent(MainActivity.this, CommentActivity.class);
        intent.putExtra(Constants.BLOG_ID_STRING_TEXT, postId);
        intent.putExtra(Constants.USER_ID_STRING_EXTRA, userId);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
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
                            startActivity(new Intent(MainActivity.this, AccountSetupActivity.class));
                            finish();
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
    protected void onStop() {
        super.onStop();
        blogFirebaseAdapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_account_settings:
                startActivity(new Intent(MainActivity.this, AccountSetupActivity.class));
                return true;
            case R.id.action_notification:
                startActivity(new Intent(MainActivity.this, NotificationActivity.class));
                return true;
            case R.id.action_search:
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
                return true;
            default:
                return false;
        }
    }

    private void logout() {
        firebaseAuth.signOut();
        openLogin();
    }

    private void openLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}

