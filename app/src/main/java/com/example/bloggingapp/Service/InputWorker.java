package com.example.bloggingapp.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import id.zelory.compressor.Compressor;

public class InputWorker extends Worker {

    // constants
    private static final String TAG = "InputWorker";
    public static final String IMAGE_URI = "POST_IMAGE_URI";
    public static final String DESC = "POST_DESC";
    public static final String USER_ID  = "USER_ID";

    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private Bitmap compressedImage;

    // instance
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());

    public InputWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        if (getRunAttemptCount() > 3) {
            return Result.failure();
        }

        try {
            putFileToStorage();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
        return Result.success();

    }

    private void displayNotification(String task, String desc) {
        NotificationManager manager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("sample", "sample", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "sample")
                .setContentTitle(task)
                .setContentText(desc)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        manager.notify(1, builder.build());
    }

    private void putFileToStorage() {
        final String randomValue = random();
        final StorageReference postImagePath = storageReference.child("post_images").child(randomValue + ".jpg");
        final String postImageUri = getInputData().getString(IMAGE_URI);
        final String desc = getInputData().getString(DESC);

        final Task<Uri> uriTask = postImagePath.putFile(Uri.parse(postImageUri)).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
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
                    Log.d(TAG, "onComplete: Image Uploaded");
                } else {
                    String error = "Error: " + task.getException().getMessage();
                    Log.d(TAG, "onComplete: " + error);
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
                    Log.d(TAG, "onComplete: " + error);
                }
            }
        });
    }

    private void storeDataToFireStore(String desc, Uri download_uri, Uri thumbnailUri) {
        final String userId = getInputData().getString(Constants.USER_ID);

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
                } else {
                    String error = task.getException().getMessage();
                    Log.d(TAG, "onComplete: " + error);
                }

            }
        });
    }


    // Return Types

    private byte[] compressImageToThumbnail() {
        final String postImageUri = getInputData().getString(IMAGE_URI);

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
