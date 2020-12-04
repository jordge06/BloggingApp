package com.example.bloggingapp.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bloggingapp.Adapter.CommentAdapter;
import com.example.bloggingapp.CommentActivity;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.Model.Comment;
import com.example.bloggingapp.Model.Notification;
import com.example.bloggingapp.NotificationPackage.NotificationActivityViewModel;
import com.example.bloggingapp.R;
import com.example.bloggingapp.ViewModel.CommentsViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CommentFragment extends Fragment {

    private String blogPostId;
    private EditText txtComment;
    private RecyclerView rvComments;

    private FirebaseFirestore firebaseFirestore;
    private CommentAdapter commentAdapter;
    private CommentsViewModel commentsViewModel;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private String tempTime = simpleDateFormat.format(calendar.getTime());
    private String tempDate = new SimpleDateFormat("MMM dd, YYYY", Locale.getDefault()).format(calendar.getTime());
    private String currentUser;
    private String postOwnerId;
    private List<Comment> commentList = new ArrayList<>();


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_comment, container, false);

        getArgs();

        txtComment = view.findViewById(R.id.txtComment);
        rvComments = view.findViewById(R.id.rvComments);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        currentUser = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setUpRecycler();

        view.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = txtComment.getText().toString();

                if (!TextUtils.isEmpty(comment)) {
                    postComment(comment);
                    addNotification();
                } else {
                    Toast.makeText(getContext(), "Empty Field", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (firebaseAuth.getCurrentUser() != null) showComments();

        return view;
    }

    private void getArgs() {
        if (this.getArguments() != null) {
            blogPostId = getArguments().getString(Constants.BLOG_ID_STRING_TEXT);
            postOwnerId = getArguments().getString(Constants.USER_ID_STRING_EXTRA);
        }
    }

    private void showComments() {
        commentsViewModel.init(blogPostId);
        commentsViewModel.getComments(blogPostId).observe(getViewLifecycleOwner(), new Observer<List<Comment>>() {
            @Override
            public void onChanged(List<Comment> comments) {
                if (commentList.size() != 0) {
                    commentList.clear();
                }
                commentList.addAll(comments);
                commentAdapter.notifyDataSetChanged();

            }
        });

//        firebaseFirestore.collection("Posts/" + blogPostId + "/Comments")
//                .orderBy("time", Query.Direction.ASCENDING)
//                .addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
//                    @Override
//                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
//                                        @javax.annotation.Nullable FirebaseFirestoreException e) {
//                        if (!queryDocumentSnapshots.isEmpty()) {
//
//                            for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
//
//                                if (documentSnapshot.getType() == DocumentChange.Type.ADDED) {
//                                    String commentId = documentSnapshot.getDocument().getId();
//                                    Comment comment = documentSnapshot.getDocument().toObject(Comment.class);
//                                    commentList.add(comment);
//                                }
//                                commentAdapter.notifyDataSetChanged();
//                            }
//                        }
//                    }
//                });
    }

    private void setUpRecycler() {
        commentAdapter = new CommentAdapter(commentList, getContext());
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
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
                    Toast.makeText(getContext(),
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
