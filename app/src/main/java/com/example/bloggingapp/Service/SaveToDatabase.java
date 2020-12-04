package com.example.bloggingapp.Service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.util.LogTime;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Model.Post;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SaveToDatabase extends Worker {

    private static final String TAG = "SaveToDatabase";

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());

    public SaveToDatabase(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getRunAttemptCount() > 3) {
            return Result.failure();
        }
        try {
            saveDataToFireStore();
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success();
    }

    private void saveDataToFireStore() {
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final String thumbnail = getInputData().getString(Constants.THUMBNAIL);
        final String image = getInputData().getString(Constants.IMAGE);
        final String desc = getInputData().getString(Constants.DESC);

        final Post post = new Post(image,
                thumbnail,
                desc,
                userId,
                tempDate);

        firebaseFirestore.collection("Posts").add(post).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: Post Added");
                    Toast.makeText(getApplicationContext(), "Process Successful", Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onComplete: " + e.getMessage());
            }
        });
    }
}
