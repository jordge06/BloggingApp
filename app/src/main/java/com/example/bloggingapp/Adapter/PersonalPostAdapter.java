package com.example.bloggingapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.PostActivity;
import com.example.bloggingapp.R;
import com.example.bloggingapp.Model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PersonalPostAdapter extends RecyclerView.Adapter<PersonalPostAdapter.MainViewHolder> {

    private List<Post> postList;
    private Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public PersonalPostAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PersonalPostAdapter.MainViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.post_image_item_template, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.setImage(postList.get(position).getImageUrl(), postList.get(position).getThumbnailUrl());

        String postId = postList.get(position).postId;
        String userId= postList.get(position).getUserId();

        holder.openPost(postId, userId);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPostImage;
        public MainViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPostImage = itemView.findViewById(R.id.imgPostImage);
        }

        private void setImage(String imageUri, String thumbnailUri) {
            RequestOptions requestOptions = new RequestOptions();
            Glide.with(context).applyDefaultRequestOptions(requestOptions.placeholder(R.drawable.empty_background))
                    .load(imageUri)
                    .thumbnail(Glide.with(context).load(thumbnailUri)).into(imgPostImage);
        }

        private void openPost(final String postId, final String userId) {
            imgPostImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, PostActivity.class);
                    intent.putExtra(Constants.BLOG_ID_STRING_TEXT, postId);
                    intent.putExtra(Constants.USER_ID_STRING_EXTRA, userId);
                    context.startActivity(intent);
                }
            });
        }


    }
}
