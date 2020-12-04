package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.MainActivityPackage.MainActivity;
import com.example.bloggingapp.Adapter.PersonalPostAdapter;
import com.example.bloggingapp.Model.Follower;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.Model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountSetupActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AccountSetupActivity";

    // instance
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private Uri mainImageURI = null;
    private PersonalPostAdapter personalPostAdapter;
    private List<Post> postList = new ArrayList<>();

    // widgets
    private ProgressBar loading;
    private EditText txtName;
    private CircleImageView profileImage;
    private Button btnSaveSettings;
    private TextView txtPostCount, txtFollowers, txtFollowing;

    // vars
    private String activeUserId;
    private boolean isChanged = false;
    private String otherUserId;

    // Get Date and Time
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_setup);

        profileImage = findViewById(R.id.profileImage);
        txtName = findViewById(R.id.txtName);
        txtPostCount = findViewById(R.id.txtPostCount);
        txtFollowers = findViewById(R.id.txtFollowers);
        txtFollowing = findViewById(R.id.txtFollowing);
        loading = findViewById(R.id.loading);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        activeUserId = firebaseAuth.getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("UserId");

        if (isUserLogin()) {
            if (otherUserId != null) {
                if (!otherUserId.equals(activeUserId)) {
                    retrieveDataFromFireStore(otherUserId);
                    loadDataByUser(otherUserId);
                    countBlogPosted(otherUserId);
                    countItems(firebaseFirestore.collection("Relationship/" + otherUserId + "/Followers"),
                            " Followers", txtFollowers);
                    countItems(firebaseFirestore.collection("Relationship/" + otherUserId + "/Following"),
                            " Following", txtFollowing);
                    txtName.setEnabled(false);
                    firebaseFirestore.collection("Relationship/" + activeUserId + "/Following").document(otherUserId)
                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if (documentSnapshot != null) {
                                        if (documentSnapshot.exists()) {
                                            btnSaveSettings.setText("UnFollow");
                                        } else {
                                            btnSaveSettings.setText("Follow");
                                        }
                                    }
                                }
                            });
                } else {
                    retrieveDataFromFireStore(activeUserId);
                    loadDataByUser(activeUserId);
                    countBlogPosted(activeUserId);
                    countItems(firebaseFirestore.collection("Relationship/" + activeUserId + "/Followers"),
                            " Followers", txtFollowers);
                    countItems(firebaseFirestore.collection("Relationship/" + activeUserId + "/Following"),
                            " Following", txtFollowing);
                }
            } else {
                retrieveDataFromFireStore(activeUserId);
                //loadPostImage(id);
                loadDataByUser(activeUserId);
                countBlogPosted(activeUserId);
                countItems(firebaseFirestore.collection("Relationship/" + activeUserId + "/Followers"),
                        " Followers", txtFollowers);
                countItems(firebaseFirestore.collection("Relationship/" + activeUserId + "/Following"),
                        " Following", txtFollowing);
            }
        }

        setUpRecycler();

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(AccountSetupActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(AccountSetupActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        selectProfileImage();
                    }
                } else {
                    selectProfileImage();
                }
            }
        });

        btnSaveSettings.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String btnText = btnSaveSettings.getText().toString();
        switch (btnText) {
            case "Save Account Settings":
                final String username = txtName.getText().toString();
                if (!TextUtils.isEmpty(username) && mainImageURI != null) {
                    showProgressBar();
                    if (isChanged) {
                        storeUserData(username);
                    } else {
                        storeDataToFireStore(username, mainImageURI);
                    }
                } else {
                    Toast.makeText(AccountSetupActivity.this, "Empty Fields", Toast.LENGTH_SHORT).show();
                }
                break;
            case "Follow":
                if (isUserLogin()) {
                    followUser(otherUserId);
                    addNotification(activeUserId, otherUserId);
                }
                break;
            case "UnFollow":
                if (isUserLogin()) {
                    unFollowUser(otherUserId);
                    deleteNotification(otherUserId, activeUserId);
                }
                break;
        }
    }

    private void unFollowUser(final String userId) {
        showProgressBar();
        firebaseFirestore.collection("Relationship/" + userId + "/Followers").document(activeUserId).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        firebaseFirestore.collection("Relationship/" + activeUserId + "/Following").document(userId).delete();
                        hideProgressBar();
                    }
                });
    }

    private void followUser(final String userId) {
        showProgressBar();
        final Follower follower = new Follower(activeUserId, tempDate, tempTime);
        final Follower following = new Follower(userId, tempDate, tempTime);

        firebaseFirestore.collection("Relationship/" + userId + "/Followers")
                .document(activeUserId).set(follower).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: Follow Successfully");
                    firebaseFirestore.collection("Relationship/" + activeUserId + "/Following").document(userId)
                            .set(following).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            hideProgressBar();
                        }
                    });

                } else {
                    Log.d(TAG, "onComplete: " + task.getException().getMessage());
                    hideProgressBar();
                }
            }
        });

    }

    private void addNotification(String currentUser, String userId) {
        Notification notification = new Notification(currentUser,
                Constants.FOLLOW_NOTIFICATION_TEXT, userId, "", false);
        firebaseFirestore.collection(Constants.NOTIFICATION_COLLECTION_NAME).add(notification);
    }

    private void deleteNotification(String id, String userId) {
        firebaseFirestore.collection(Constants.NOTIFICATION_COLLECTION_NAME).whereEqualTo("ownerId", id)
                .whereEqualTo("currentUser", userId).get()
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

    private Boolean isUserLogin() {
        return (firebaseAuth.getCurrentUser() != null);
    }

    private void selectProfileImage() {
        if (otherUserId != null) {
            if (otherUserId.equals(activeUserId)) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(AccountSetupActivity.this);
            }
        } else {
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(AccountSetupActivity.this);
        }

    }

    private void loadDataByUser(String id) {
        Query firstDate = firebaseFirestore.collection("Posts").whereEqualTo("userId", id)
                .orderBy("timeStamp", Query.Direction.DESCENDING);

        firstDate.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d(TAG, "onEvent: " + "Error: " + e.getMessage());
                } else {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {

                            if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
                                String postId = documentSnapshot.getDocument().getId();
                                Post post = documentSnapshot.getDocument().toObject(Post.class).withId(postId);
                                postList.add(post);
                                personalPostAdapter.notifyDataSetChanged();
                            }
                        }

                    }
                }
            }
        });
    }

    private void countItems(Query query, final String textDisplay, final TextView txt) {
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        int count = queryDocumentSnapshots.size();
                        String text = count + textDisplay;
                        txt.setText(text);
                    } else {
                        String text = 0 + textDisplay;
                        txt.setText(text);
                    }
                }
            }
        });
    }

    private void countBlogPosted(String currentUser) {
        firebaseFirestore.collection("Users").whereEqualTo("userId", currentUser)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (queryDocumentSnapshots != null) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                int count = queryDocumentSnapshots.size();
                                String text = count + " Posts";
                                txtPostCount.setText(text);
                            } else {
                                txtPostCount.setText("0 Post");
                            }
                        }
                    }
                });
    }

    private void setUpRecycler() {
        RecyclerView rvPostImages = findViewById(R.id.rvPostImages);
        personalPostAdapter = new PersonalPostAdapter(postList, AccountSetupActivity.this);
        rvPostImages.setLayoutManager(new GridLayoutManager(AccountSetupActivity.this, 4));
        rvPostImages.setAdapter(personalPostAdapter);
        rvPostImages.hasFixedSize();
    }


    private void storeUserData(final String username) {
        final StorageReference image_path = storageReference.child("profile_images").child(activeUserId + ".jpg");

        Task<Uri> uriTask = image_path.putFile(mainImageURI).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return image_path.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri download_uri = task.getResult();
                    storeDataToFireStore(username, download_uri);
                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(AccountSetupActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
                hideProgressBar();
            }
        });

    }

    private void retrieveDataFromFireStore(String givenId) {
        showProgressBar();
        firebaseFirestore.collection("Users")
                .document(givenId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().exists()) {
                                String name = task.getResult().getString("name");
                                String imageUrl = task.getResult().getString("image");

                                mainImageURI = Uri.parse(imageUrl);

                                RequestOptions placeHolderRequest = new RequestOptions();
                                placeHolderRequest.placeholder(R.drawable.default_image);

                                Glide.with(AccountSetupActivity.this).setDefaultRequestOptions(placeHolderRequest).load(imageUrl).into(profileImage);

                                txtName.setText(name);
                            } else {
                                Log.d(TAG, "onComplete: Data Does not Exist");
                                hideProgressBar();
                            }
                        } else {
                            String error = task.getException().getMessage();
                            Toast.makeText(AccountSetupActivity.this, "FireStore Retrieve Error: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                        hideProgressBar();
                    }
                });
    }

    private void storeDataToFireStore(final String username, Uri uri) {
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", username);
        userMap.put("image", uri.toString());

        User user = new User(username, username.toLowerCase(), uri.toString(), activeUserId);

        firebaseFirestore.collection("Users").document(activeUserId).set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    startActivity(new Intent(AccountSetupActivity.this, MainActivity.class));
                    finish();
                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(AccountSetupActivity.this, "FireStore Error: " + error, Toast.LENGTH_SHORT).show();
                }
                hideProgressBar();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                profileImage.setImageURI(mainImageURI);
                isChanged = true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void showProgressBar() {
        loading.setVisibility(View.VISIBLE);
        btnSaveSettings.setEnabled(false);
    }

    private void hideProgressBar() {
        loading.setVisibility(View.GONE);
        btnSaveSettings.setEnabled(true);
    }
}