package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import id.zelory.compressor.Compressor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bloggingapp.MainActivityPackage.MainActivity;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.Service.InputWorker;
import com.example.bloggingapp.Service.SaveToDatabase;
import com.example.bloggingapp.Service.UploadImageWorker;
import com.example.bloggingapp.Service.UploadThumbnailWorker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class NewPostActivity extends AppCompatActivity {

    private static final String TAG = "NewPostActivity";

    private static final int MAX_LENGTH = 50;
    private ImageView imgNewPost;
    private Button btnPost;
    private TextView txtPostDesc;
    private Uri postImageUri = null;
    private ProgressBar loading;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private WorkManager workManager;

    private String userId;
    private Bitmap compressedImage;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        imgNewPost = findViewById(R.id.imgNewPost);
        btnPost = findViewById(R.id.btnPost);
        txtPostDesc = findViewById(R.id.txtPostDesc);
        loading = findViewById(R.id.loading);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();
        workManager = WorkManager.getInstance(this);

        imgNewPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512)
                        .setAspectRatio(1, 1)
                        .start(NewPostActivity.this);
            }
        });

        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String desc = txtPostDesc.getText().toString();
                if (!TextUtils.isEmpty(desc) && postImageUri != null) {
                    showLoading();
                    //startChainWorks(desc, constraints);
                    startWorkers(desc, constraints);
                } else {
                    Toast.makeText(NewPostActivity.this, "Empty Fields", Toast.LENGTH_SHORT).show();
                    hideLoading();
                }
            }
        });


    }

    private void startChainWorks(String desc, Constraints constraints) {
        final Data data = new Data.Builder()
                .putString(Constants.USER_ID, userId)
                .putString(Constants.DESC, desc)
                .putString(Constants.IMAGE, postImageUri.toString())
                .build();

        final OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(UploadImageWorker.class)
                .setInputData(data)
                .setConstraints(constraints)
                .build();

        final OneTimeWorkRequest uploadThumbnailTask = new OneTimeWorkRequest.Builder(UploadThumbnailWorker.class)
                .setConstraints(constraints)
                .build();

        final OneTimeWorkRequest saveToDBTask = new OneTimeWorkRequest.Builder(SaveToDatabase.class)
                .setConstraints(constraints)
                .build();

        workManager.beginWith(oneTimeWorkRequest)
                .then(uploadThumbnailTask)
                .then(saveToDBTask)
                .enqueue();

        checkWorkState(saveToDBTask.getId());
    }

    private void startWorkers(String desc, Constraints constraints) {
        final Data data = new Data.Builder()
                .putString(Constants.USER_ID, userId)
                .putString(Constants.DESC, desc)
                .putString(Constants.IMAGE, postImageUri.toString())
                .build();

        final OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(InputWorker.class)
                .setInputData(data)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(oneTimeWorkRequest);
        checkWorkState(oneTimeWorkRequest.getId());
    }

    private void checkWorkState(final UUID workId) {
        workManager.getWorkInfoByIdLiveData(workId).observe(NewPostActivity.this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                String status = workInfo.getState().name();
                Toast.makeText(NewPostActivity.this, "" + status, Toast.LENGTH_SHORT).show();

                hideLoading();
                Intent intent = new Intent(NewPostActivity.this, MainActivity.class);
                if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                    intent.putExtra("POST_MESSAGE",
                            "Network is currently not available. Your Image will be posted ounce the Network is Available");
                }
                startActivity(intent);
            }
        });
    }

    private void storeUserData(final String desc) {
        final String randomValue = random();
        putFileToStorage(randomValue, desc);
    }

    private void putFileToStorage(final String randomValue, final String desc) {
        final StorageReference postImagePath = storageReference.child("post_images").child(randomValue + ".jpg");

        final Task<Uri> uriTask = postImagePath.putFile(postImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return postImagePath.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    putCompressByteToStorage(randomValue, task.getResult(), desc);
                    Toast.makeText(NewPostActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                } else {
                    String error = "Error: " + task.getException().getMessage();
                    Toast.makeText(NewPostActivity.this, error, Toast.LENGTH_SHORT).show();
                    hideLoading();
                }
            }
        });
    }

    private void putCompressByteToStorage(final String randomValue, final Uri imageUri, final String desc) {

        final StorageReference compressedPostImagePath = storageReference.child("post_images/thumbs").child(randomValue + ".jpg");

        Task<Uri> uri = compressedPostImagePath.putBytes(compressImageToThumbnail()).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return compressedPostImagePath.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    storeDataToFireStore(desc, imageUri, task.getResult());
                } else {
                    String error = "Error: " + task.getException().getMessage();
                    Toast.makeText(NewPostActivity.this, error, Toast.LENGTH_SHORT).show();
                    hideLoading();
                }
            }
        });
    }

    private byte[] compressImageToThumbnail() {
        File imageFile = new File(postImageUri.getPath());
        try {
            compressedImage = new Compressor(NewPostActivity.this)
                    .setMaxHeight(100)
                    .setMaxWidth(100)
                    .setQuality(3)
                    .compressToBitmap(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        compressedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private void storeDataToFireStore(String desc, Uri download_uri, Uri thumbnailUri) {

        final Post post = new Post(download_uri.toString(),
                thumbnailUri.toString(),
                desc,
                userId,
                tempDate);

        firebaseFirestore.collection("Posts").add(post).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: Post Added");
                    hideLoading();
                    startActivity(new Intent(NewPostActivity.this, MainActivity.class));
                    finish();
                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(NewPostActivity.this, "FireStore Error: " + error, Toast.LENGTH_SHORT).show();
                }
                hideLoading();
            }
        });
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void showLoading() {
        loading.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);
        txtPostDesc.setEnabled(false);
    }

    private void hideLoading() {
        loading.setVisibility(View.GONE);
        btnPost.setEnabled(true);
        txtPostDesc.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                postImageUri = result.getUri();
                imgNewPost.setImageURI(postImageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}