package com.example.bloggingapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.R;
import com.example.bloggingapp.Interface.BlogAdapterInterface;
import com.example.bloggingapp.Model.Post;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;


import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    private List<Post> posts;
    private Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private BlogAdapterInterface blogAdapterInterface;

    public BlogAdapter(List<Post> posts, Context context) {
        this.posts = posts;
        this.context = context;
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        //blogAdapterInterface = (BlogAdapterInterface) context;
    }

    public void setBlogAdapterInterface(BlogAdapterInterface blogAdapterInterface) {
        this.blogAdapterInterface = blogAdapterInterface;
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BlogViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.blog_item_template, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final BlogViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        holder.txtDesc.setText(posts.get(position).getPostDesc());
        holder.txtUsername.setText(posts.get(position).getUserId());

        String imgUri = posts.get(position).getImageUrl();
        String thumbnailUrl = posts.get(position).getThumbnailUrl();
        holder.setImage(imgUri, thumbnailUrl, holder.imgPostImage, R.drawable.empty_background);

        final String userId = posts.get(position).getUserId();
        holder.loadUserData(userId, holder.txtUsername, holder.imgProfilePic);

        holder.txtDate.setText(posts.get(position).getTimeStamp());

        final String postId = posts.get(position).postId;

        if (firebaseAuth.getCurrentUser() != null) {

            if (userId.equals(firebaseAuth.getCurrentUser().getUid())) {
                holder.btnOption.setVisibility(View.VISIBLE);
                holder.btnOption.setEnabled(true);
            }

            final String currentUser = firebaseAuth.getCurrentUser().getUid();

            // Count like
            firebaseFirestore.collection("Posts/" + postId + "/Likes")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            if (queryDocumentSnapshots != null) {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    int count = queryDocumentSnapshots.size();
                                    holder.updateLikeCount(count, holder.txtLikes);
                                } else {
                                    holder.updateLikeCount(0, holder.txtLikes);
                                }
                            }
                        }
                    });

            // Count Comments
            firebaseFirestore.collection("Posts/" + postId + "/Comments")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            if (queryDocumentSnapshots != null) {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    int count = queryDocumentSnapshots.size();
                                    holder.updateCommentCount(count, holder.txtComment);
                                } else {
                                    holder.updateCommentCount(0, holder.txtComment);
                                }
                            }
                        }
                    });

            // Change image when like
            firebaseFirestore.collection("Posts/" + postId + "/Likes")
                    .document(currentUser).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (documentSnapshot != null) {
                        if (documentSnapshot.exists()) {
                            holder.btnLike.setBackgroundResource(R.drawable.ic_like);
                        } else {
                            holder.btnLike.setBackgroundResource(R.drawable.ic_unlike);
                        }
                    }
                }
            });
        }


    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public class BlogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView txtUsername, txtDate, txtDesc, txtLikes, txtComment;
        private ImageView imgPostImage, imgProfilePic, btnLike, btnComment, btnOption;

        public BlogViewHolder(@NonNull View itemView) {
            super(itemView);

            txtDate = itemView.findViewById(R.id.txtDate);
            txtDesc = itemView.findViewById(R.id.txtDesc);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);
            btnLike = itemView.findViewById(R.id.btnLike);
            txtLikes = itemView.findViewById(R.id.txtLikes);
            btnComment = itemView.findViewById(R.id.btnComment);
            txtComment = itemView.findViewById(R.id.txtComment);
            btnOption = itemView.findViewById(R.id.btnOption);


            if (firebaseAuth.getCurrentUser() != null) {

                // Open Comment Section
                txtComment.setOnClickListener(this);
                btnComment.setOnClickListener(this);
                imgPostImage.setOnClickListener(this);

                // Delete Post
                btnOption.setOnClickListener(this);

                // Like Post
                btnLike.setOnClickListener(this);

                // Open Profile
                imgProfilePic.setOnClickListener(this);
                txtUsername.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            final String postId = posts.get(getAdapterPosition()).postId;
            final String currentUser = firebaseAuth.getCurrentUser().getUid();
            final String userId = posts.get(getAdapterPosition()).getUserId();
            switch (view.getId()) {
                case R.id.txtComment:
                case R.id.btnComment:
                case R.id.imgPostImage:
                    blogAdapterInterface.openComments(postId, userId);
                    break;
                case R.id.btnLike:
                    blogAdapterInterface.likePost(postId, currentUser, userId, btnLike);
                    break;
                case R.id.imgProfilePic:
                case R.id.txtUsername:
                    blogAdapterInterface.openProfile(userId);
                    break;
                case R.id.btnOption:
                    blogAdapterInterface.option(getAdapterPosition(), postId, userId, btnOption);
                    break;
            }
        }

        private void updateCommentCount(int count, TextView txt) {
            String text = count == 1 ? " Comment" : " Comments";
            String commentText = count + text;
            txt.setText(commentText);
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

        private void updateLikeCount(int count, TextView txtLike) {
            String text = count == 1 ? " like" : " likes";
            String likeText = count + text;
            txtLike.setText(likeText);
        }
    }
}
