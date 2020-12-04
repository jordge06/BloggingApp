package com.example.bloggingapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.Model.Post;
import com.example.bloggingapp.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BlogFirebaseAdapter extends FirestoreRecyclerAdapter<Post, BlogFirebaseAdapter.BlogViewHolder> {

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private BlogFirebaseRecyclerInterface blogFirebaseRecyclerInterface;
    private Context context;

    public void setBlogFirebaseRecyclerInterface(BlogFirebaseRecyclerInterface blogFirebaseRecyclerInterface) {
        this.blogFirebaseRecyclerInterface = blogFirebaseRecyclerInterface;
    }

    public BlogFirebaseAdapter(@NonNull FirestoreRecyclerOptions<Post> options, Context context) {
        super(options);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        this.context = context;
    }

    public void deletePost(int pos) {
        getSnapshots().getSnapshot(pos).getReference().delete();
    }

    @Override
    protected void onBindViewHolder(@NonNull final BlogViewHolder holder, int position, @NonNull Post model) {
        holder.txtDesc.setText(model.getPostDesc());

        String imgUri = model.getImageUrl();
        String thumbnailUrl = model.getThumbnailUrl();
        holder.setImage(imgUri, thumbnailUrl, holder.imgPostImage, R.drawable.empty_background);

        final String userId = model.getUserId();
        holder.loadUserData(userId, holder.txtUsername, holder.imgProfilePic);

        holder.txtDate.setText(model.getTimeStamp());

        final String postId = getSnapshots().getSnapshot(position).getId();

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
                            holder.btnLike.setBackgroundResource(R.drawable.ic_favorite_inactive);
                        }
                    }
                }
            });
        }

    }

    public interface BlogFirebaseRecyclerInterface {
        void openComments(String postId, String userId);

        void likePost(final String postId, final String currentUser, final String userId, final ImageView btnLike);

        void openProfile(String id);

        void option(final int position, ImageView view);
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BlogFirebaseAdapter.BlogViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.blog_item_template, parent, false));
    }

    public class BlogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView txtUsername, txtDate, txtDesc, txtLikes, txtComment;
        private ImageView imgPostImage, imgProfilePic, btnLike, btnOption;

        public BlogViewHolder(@NonNull View itemView) {
            super(itemView);

            txtDate = itemView.findViewById(R.id.txtDate);
            txtDesc = itemView.findViewById(R.id.txtDesc);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);
            btnLike = itemView.findViewById(R.id.btnLike);
            txtLikes = itemView.findViewById(R.id.txtLikes);
            //btnComment = itemView.findViewById(R.id.btnComment);
            txtComment = itemView.findViewById(R.id.txtComment);
            btnOption = itemView.findViewById(R.id.btnOption);


            if (firebaseAuth.getCurrentUser() != null) {

                // Open Comment Section
                txtComment.setOnClickListener(this);
                itemView.findViewById(R.id.btnComment).setOnClickListener(this);
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
            final int position = getAdapterPosition();
            if (getAdapterPosition() != RecyclerView.NO_POSITION && blogFirebaseRecyclerInterface != null) {
                final DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(position);
                final String userId = documentSnapshot.getString("userId");
                final String postId = documentSnapshot.getId();
                final String currentUser = firebaseAuth.getCurrentUser().getUid();
                switch (view.getId()) {
                    case R.id.txtComment:
                    case R.id.btnComment:
                    case R.id.imgPostImage:
                        blogFirebaseRecyclerInterface.openComments(postId, userId);
                        break;
                    case R.id.btnLike:
                        blogFirebaseRecyclerInterface.likePost(postId, currentUser, userId, btnLike);
                        break;
                    case R.id.imgProfilePic:
                    case R.id.txtUsername:
                        blogFirebaseRecyclerInterface.openProfile(userId);
                        break;
                    case R.id.btnOption:
                        blogFirebaseRecyclerInterface.option(getAdapterPosition(), btnOption);
                        break;
                }
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
                        if (task.getResult() != null) {
                            String username = task.getResult().getString("name");
                            String profileImage = task.getResult().getString("image");
                            String thumbnail = task.getResult().getString("thumbnail");
                            txtName.setText(username);
                            setImage(profileImage, thumbnail, img, R.drawable.empty_profile);
                        }
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
