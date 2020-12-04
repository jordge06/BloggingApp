package com.example.bloggingapp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Interface.MainAppInterface;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.R;
import com.example.bloggingapp.Service.InputWorker;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;

public class NewPostFragment extends Fragment{

    private static final String TAG = "NewPostActivity";

    // widgets
    private static final int MAX_LENGTH = 50;
    private ImageView imgNewPost;
    private Button btnPost;
    private TextView txtPostDesc;
    private Uri postImageUri = null;
    private ProgressBar loading;

    // instance
    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private Context context;
    private MainAppInterface mainAppInterface;
    private WorkManager workManager;

    // vars
    private String userId;
    private Bitmap compressedImage;

    // instance
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainAppInterface = (MainAppInterface) context;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.getArguments() != null) {
            postImageUri = Uri.parse(getArguments().getString("ImageUri"));
        }
        imgNewPost.setImageURI(postImageUri);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_new_post, container, false);

        context = getContext();
        imgNewPost = view.findViewById(R.id.imgNewPost);
        btnPost = view.findViewById(R.id.btnPost);
        txtPostDesc = view.findViewById(R.id.txtPostDesc);
        loading = view.findViewById(R.id.loading);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        workManager = WorkManager.getInstance(context);
        userId = firebaseAuth.getCurrentUser().getUid();

        imgNewPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512)
                        .setAspectRatio(1, 1)
                        .start(context, NewPostFragment.this);
            }
        });

        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();
                final String desc = txtPostDesc.getText().toString();
                if (!TextUtils.isEmpty(desc) && postImageUri != null) {
                    showLoading();
                    //startChainWorks(desc, constraints);
                    startWorkers(desc, constraints);
                } else {
                    Toast.makeText(context, "Empty Fields", Toast.LENGTH_SHORT).show();
                    hideLoading();
                }
            }
        });

        return view;
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
        workManager.getWorkInfoByIdLiveData(workId).observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                String status = workInfo.getState().name();
                Toast.makeText(context, "" + status, Toast.LENGTH_SHORT).show();

                hideLoading();

                if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                    String enqueuedWarning = "Network is currently not available. Your Image will be posted ounce the Network is Available";
                    mainAppInterface.gotoHome(TAG, enqueuedWarning);
                }
                mainAppInterface.gotoHome(TAG, "Successfully Posted!");
            }
        });
    }

//    private void storeUserData(final String desc) {
//        final String randomValue = random();
//        putFileToStorage(randomValue, desc);
//    }
//
//    private void putFileToStorage(final String randomValue, final String desc) {
//        final StorageReference postImagePath = storageReference.child("post_images").child(randomValue + ".jpg");
//
//        final Task<Uri> uriTask = postImagePath.putFile(postImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//            @Override
//            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                if (!task.isSuccessful()) {
//                    throw task.getException();
//                }
//                return postImagePath.getDownloadUrl();
//            }
//        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//            @Override
//            public void onComplete(@NonNull Task<Uri> task) {
//                if (task.isSuccessful()) {
//                    putCompressByteToStorage(randomValue, task.getResult(), desc);
//                    Toast.makeText(context, "Image Uploaded", Toast.LENGTH_SHORT).show();
//                } else {
//                    String error = "Error: " + task.getException().getMessage();
//                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
//                    hideLoading();
//                }
//            }
//        });
//    }
//
//    private void putCompressByteToStorage(final String randomValue, final Uri imageUri, final String desc) {
//
//        final StorageReference compressedPostImagePath = storageReference.child("post_images/thumbs").child(randomValue + ".jpg");
//
//        Task<Uri> uri = compressedPostImagePath.putBytes(compressImageToThumbnail()).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//            @Override
//            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                if (!task.isSuccessful()) {
//                    throw task.getException();
//                }
//                return compressedPostImagePath.getDownloadUrl();
//            }
//        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//            @Override
//            public void onComplete(@NonNull Task<Uri> task) {
//                if (task.isSuccessful()) {
//                    storeDataToFireStore(desc, imageUri, task.getResult());
//                } else {
//                    String error = "Error: " + task.getException().getMessage();
//                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
//                    hideLoading();
//                }
//            }
//        });
//    }
//
//    private byte[] compressImageToThumbnail() {
//        File imageFile = new File(postImageUri.getPath());
//        try {
//            compressedImage = new Compressor(context)
//                    .setMaxHeight(100)
//                    .setMaxWidth(100)
//                    .setQuality(3)
//                    .compressToBitmap(imageFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        compressedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//
//        return byteArrayOutputStream.toByteArray();
//    }
//
//    private void storeDataToFireStore(String desc, Uri download_uri, Uri thumbnailUri) {
//
//        final Post post = new Post(download_uri.toString(),
//                thumbnailUri.toString(),
//                desc,
//                userId,
//                tempDate);
//
//        firebaseFirestore.collection("Posts").add(post).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentReference> task) {
//                if (task.isSuccessful()) {
//                    Log.d(TAG, "onComplete: Post Added");
//                    hideLoading();
//                    mainAppInterface.gotoHome(TAG, "");
//
//                } else {
//                    String error = task.getException().getMessage();
//                    Toast.makeText(context, "FireStore Error: " + error, Toast.LENGTH_SHORT).show();
//                }
//                hideLoading();
//            }
//        });
//    }
//
//    public static String random() {
//        Random generator = new Random();
//        StringBuilder randomStringBuilder = new StringBuilder();
//        int randomLength = generator.nextInt(MAX_LENGTH);
//        char tempChar;
//        for (int i = 0; i < randomLength; i++) {
//            tempChar = (char) (generator.nextInt(96) + 32);
//            randomStringBuilder.append(tempChar);
//        }
//        return randomStringBuilder.toString();
//    }

    private void showLoading() {
        loading.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);
    }

    private void hideLoading() {
        loading.setVisibility(View.GONE);
        btnPost.setEnabled(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
