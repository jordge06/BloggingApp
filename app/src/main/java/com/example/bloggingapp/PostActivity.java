package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import javax.annotation.Nullable;

public class PostActivity extends AppCompatActivity {

    // vars
    private String postId, userId, activeUserId;

    // instance
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    // widgets
    private ImageView imgPostImage, imgProfilePic, btnLike;
    private TextView txtUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        postId = getIntent().getStringExtra(Constants.BLOG_ID_STRING_TEXT);
        userId = getIntent().getStringExtra("UserId");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        activeUserId = firebaseAuth.getUid();

        imgPostImage = findViewById(R.id.imgPostImage);
        imgProfilePic = findViewById(R.id.imgProfilePic);
        txtUsername = findViewById(R.id.txtUsername);

        ImageView btnComment = findViewById(R.id.btnComment);
        btnLike = findViewById(R.id.btnLike);

        if (firebaseAuth.getCurrentUser() != null) {
            loadPost(postId);
            loadUserData(getUserId());
            changeLikeImage(postId, activeUserId);
        }

        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PostActivity.this, CommentActivity.class);
                intent.putExtra(Constants.BLOG_ID_STRING_TEXT, postId);
                startActivity(intent);
            }
        });
    }

    private String getUserId() {
        if (userId != null) {
            return userId;
        } else return activeUserId;
    }

    private void loadPost(String postId) {
        firebaseFirestore.collection("Posts")
                .document(postId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    String thumbnailUri = task.getResult().getString("thumbnailUrl");
                    String imageUri = task.getResult().getString("imageUrl");
                    setImage(imageUri, thumbnailUri, imgPostImage);
                }
            }
        });
    }

    private void loadUserData(String userId) {
        firebaseFirestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    String profileImage = task.getResult().getString("image");
                    String thumbnail = task.getResult().getString("thumbnail");
                    String name = task.getResult().getString("name");
                    txtUsername.setText(name);
                    setImage(profileImage, thumbnail, imgProfilePic);
                }
            }
        });
    }

    private void changeLikeImage(String postId, String userId) {
        firebaseFirestore.collection("Posts/" + postId + "/Likes")
                .document(userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null) {
                    if (documentSnapshot.exists()) {
                        btnLike.setBackgroundResource(R.drawable.ic_like);
                    } else {
                        btnLike.setBackgroundResource(R.drawable.ic_unlike);
                    }
                }
            }
        });
    }

    private void setImage(String imageUri, String thumbnailUri, ImageView img) {
        RequestOptions requestOptions = new RequestOptions();
        Glide.with(PostActivity.this).applyDefaultRequestOptions(requestOptions.placeholder(R.drawable.empty_background)).load(imageUri)
                .thumbnail(Glide.with(PostActivity.this).load(thumbnailUri)).into(img);
    }
}