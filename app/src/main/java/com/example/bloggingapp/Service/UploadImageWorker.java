package com.example.bloggingapp.Service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.bloggingapp.Constants;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Random;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UploadImageWorker extends Worker {

    private static final String TAG = "UploadImageWorker";

    private Data data;
    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    public UploadImageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        if (getRunAttemptCount() > 3) {
            return Result.failure();
        }

        try {
            uploadImage();
        } catch (Exception e) {
            return Result.retry();
        }

        return Result.success(data);
    }

    private void uploadImage() {
        final String randomValue = random();
        final StorageReference postImagePath = storageReference.child("post_images").child(randomValue + ".jpg");
        final String postImageUri = getInputData().getString(Constants.IMAGE);

        postImagePath.putFile(Uri.parse(postImageUri)).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
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
                    if (task.getResult() != null) {
                        data = new Data.Builder()
                                .putString(Constants.IMAGE, task.getResult().toString())
                                .putString(Constants.RANDOM, randomValue)
                                .putString(Constants.ORIGINAL_IMAGE, postImageUri)
                                .putString(Constants.DESC, getInputData().getString(Constants.DESC))
                                .build();
                        Log.d(TAG, "onComplete: Image Uploaded");
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onComplete: " + e.getMessage());
            }
        });
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(50);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
