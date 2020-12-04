package com.example.bloggingapp.Service;

import android.content.Context;
import android.util.Log;

import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Model.Notification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LikePostWorker extends Worker {

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private final String id = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public LikePostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getRunAttemptCount() > 3) return Result.failure();
        try {
            likePost();
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success();
    }

    private void likePost() {
        final String postId = getInputData().getString("POST_ID");
        final String currentUser = getInputData().getString("CURRENT_USER");
        final String userId = getInputData().getString("USER_ID");

        firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" + postId + "/" + Constants.LIKE_COLLECTION_NAME)
                .document(currentUser).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" +
                            postId + "/" + Constants.LIKE_COLLECTION_NAME)
                            .document(currentUser).delete();
                    deleteNotification(userId, postId);
                } else {
                    Map<String, Object> likesMap = new HashMap<>();
                    likesMap.put("timeStamp", FieldValue.serverTimestamp());

                    firebaseFirestore.collection(Constants.POST_COLLECTION_NAME +  "/" +
                            postId + "/" + Constants.LIKE_COLLECTION_NAME)
                            .document(currentUser).set(likesMap);

                    addNotification(currentUser, userId, postId);
                }
            }
        });
    }

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
}
