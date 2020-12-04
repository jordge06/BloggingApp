package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bloggingapp.Adapter.CommentAdapter;
import com.example.bloggingapp.Model.Comment;
import com.example.bloggingapp.Model.Notification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class CommentActivity extends AppCompatActivity {

    private String blogPostId;
    private EditText txtComment;

    private FirebaseFirestore firebaseFirestore;
    private CommentAdapter commentAdapter;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());
    private String currentUser;
    private String postOwnerId;

    private List<Comment> commentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        blogPostId = getIntent().getStringExtra(Constants.BLOG_ID_STRING_TEXT);
        postOwnerId = getIntent().getStringExtra(Constants.USER_ID_STRING_EXTRA);

        Button btnSend = findViewById(R.id.btnSend);
        txtComment = findViewById(R.id.txtComment);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        currentUser = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setUpRecycler();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = txtComment.getText().toString();

                if (!TextUtils.isEmpty(comment)) {
                    postComment(comment);
                    addNotification();
                } else {
                    Toast.makeText(CommentActivity.this, "Empty Field", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (firebaseAuth.getCurrentUser() != null) showComments();
    }

    private void showPostImage() {
        firebaseFirestore.collection("Posts").document(blogPostId)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    String postImageUri = task.getResult().getString("imageUrl");
                    String thumbnailImageUrl = task.getResult().getString("thumbnailUrl");
                    setImage(postImageUri, thumbnailImageUrl);
                }
            }
        });
    }

    private void setImage(String imageUri, String thumbnailUri) {
//        ImageView imgPost = findViewById(R.id.imgPost);
//        RequestOptions requestOptions = new RequestOptions();
//        Glide.with(CommentActivity.this)
//                .applyDefaultRequestOptions(requestOptions.placeholder(R.drawable.default_image))
//                .load(imageUri)
//                .thumbnail(Glide.with(CommentActivity.this).load(thumbnailUri)).into(imgPost);
    }

    private void showComments() {
        firebaseFirestore.collection("Posts/" + blogPostId + "/Comments")
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener(CommentActivity.this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (!queryDocumentSnapshots.isEmpty()) {

                            for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                                if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                    String commentId = documentSnapshot.getDocument().getId();
                                    Comment comment = documentSnapshot.getDocument().toObject(Comment.class);
                                    commentList.add(comment);
                                }
                                commentAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    private void setUpRecycler() {
        RecyclerView rvComments = findViewById(R.id.rvComments);
        commentAdapter = new CommentAdapter(commentList, CommentActivity.this);
        rvComments.setLayoutManager(new LinearLayoutManager(CommentActivity.this));
        rvComments.setAdapter(commentAdapter);
        rvComments.hasFixedSize();
    }

    private void postComment(String comment) {
        Comment commentObj = new Comment(comment, currentUser, tempDate, tempTime);
        firebaseFirestore.collection("Posts/" + blogPostId + "/Comments")
                .add(commentObj).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {

            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(CommentActivity.this,
                            "Error: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                } else txtComment.setText("");
            }
        });
    }

    private void addNotification() {
        if (!postOwnerId.equals(currentUser)) {
            Notification notification = new Notification(currentUser,
                    Constants.COMMENT_NOTIFICATION_TEXT, "" + postOwnerId, blogPostId, true);
            firebaseFirestore.collection("Notification").add(notification);
        }
    }

    private void deleteNotification(String userId, String blogId) {
        if (!userId.equals(currentUser)) {
            firebaseFirestore.collection("Notification").whereEqualTo("ownerId", userId)
                    .whereEqualTo("blogId", blogId)
                    .whereEqualTo("text", Constants.COMMENT_NOTIFICATION_TEXT)
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
    }
}