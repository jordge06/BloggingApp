package com.example.bloggingapp.Service;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.example.bloggingapp.Constants;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import id.zelory.compressor.Compressor;

public class UploadThumbnailWorker extends Worker {

    private static final String TAG = "UploadThumbnailWorker";
    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    private Bitmap compressedImage;
    private Data data;

    public UploadThumbnailWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String randomValue = getInputData().getString(Constants.RANDOM);
        try {
            uploadThumbnail(randomValue);
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success(data);
    }

    private void uploadThumbnail(final String randomValue) {

        final StorageReference compressedPostImagePath = storageReference.child("post_images/thumbs").child(randomValue + ".jpg");

        compressedPostImagePath.putBytes(compressImageToThumbnail()).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
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
                    if (task.getResult() != null) {
                        data = new Data.Builder()
                                .putString(Constants.USER_ID, getInputData().getString(Constants.USER_ID))
                                .putString(Constants.DESC, getInputData().getString(Constants.DESC))
                                .putString(Constants.IMAGE, getInputData().getString(Constants.IMAGE))
                                .putString(Constants.THUMBNAIL, task.getResult().toString())
                                .build();
                        Log.d(TAG, "onComplete: Thumbnail Uploaded");
                    }
                } else {
                    String error = "Error: " + task.getException().getMessage();
                    Log.d(TAG, "onComplete: " + error);
                }
            }
        });
    }

    private byte[] compressImageToThumbnail() {
        final String postImageUri = getInputData().getString(Constants.ORIGINAL_IMAGE);
        File imageFile = new File(Uri.parse(postImageUri).getPath());
        try {
            compressedImage = new Compressor(getApplicationContext())
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
}
