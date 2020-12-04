package com.example.bloggingapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.AccountSetupActivity;
import com.example.bloggingapp.R;
import com.example.bloggingapp.Model.Comment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.MainViewHolder> {

    private List<Comment> commentList;
    private Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public CommentAdapter(List<Comment> commentList, Context context) {
        this.commentList = commentList;
        this.context = context;
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CommentAdapter.MainViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item_template, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final MainViewHolder holder, int position) {
        holder.txtComment.setText(commentList.get(position).getComment());

        final String userId = commentList.get(position).getUserId();
        if (firebaseAuth.getCurrentUser() != null) {
            holder.loadUserData(userId, holder.txtUsername, holder.imgProfilePic);
        }

        holder.txtUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.openProfile(userId);
            }
        });

        holder.imgProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.openProfile(userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {

        private TextView txtComment, txtUsername;
        private ImageView imgProfilePic, btnLike;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);

            txtComment = itemView.findViewById(R.id.txtComment);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);
            btnLike = itemView.findViewById(R.id.btnLike);
        }

        private void openProfile(String userId) {
            Intent intent = new Intent(context, AccountSetupActivity.class);
            intent.putExtra("UserId", userId);
            context.startActivity(intent);
        }

        private void setImage(String imageUri, String thumbnailUri, ImageView img, int placeholder) {
            RequestOptions requestOptions = new RequestOptions();
            Glide.with(context).applyDefaultRequestOptions(requestOptions.placeholder(placeholder)).load(imageUri)
                    .thumbnail(Glide.with(context).load(thumbnailUri)).into(img);
        }

        private void loadUserData(String id, final TextView txtName, final ImageView img) {
            firebaseFirestore.collection("Users").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        String username = task.getResult().getString("name");
                        String profileImage = task.getResult().getString("image");
                        String thumbnail = task.getResult().getString("thumbnail");
                        txtName.setText(username);
                        setImage(profileImage, thumbnail, img, R.drawable.empty_profile);

                    }
                }
            });
        }
    }
}
