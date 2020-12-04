package com.example.bloggingapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bloggingapp.AccountSetupActivity;
import com.example.bloggingapp.Constants;
import com.example.bloggingapp.PostActivity;
import com.example.bloggingapp.R;
import com.example.bloggingapp.Model.Notification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MainViewHolder> {

    private List<Notification> notificationList = new ArrayList<>();
    private Context context;
    private FirebaseFirestore firebaseFirestore;

    public NotificationAdapter(Context context) {
        this.context = context;
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    public void setNotificationList(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.notification_item_template, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        String currentUser = notificationList.get(position).getCurrentUser();
        holder.loadUserData(currentUser, holder.imgProfilePic, notificationList.get(position).getText());
        String blogId = notificationList.get(position).getBlogId();
        if (notificationList.get(position).isPost()) {
            holder.loadPostImage(blogId, holder.imgPostImage);
        } else {
            holder.imgPostImage.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {

        CircleImageView imgProfilePic;
        ImageView imgPostImage;
        TextView txtNotificationText;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);
            txtNotificationText = itemView.findViewById(R.id.txtNotificationText);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String postId = notificationList.get(getAdapterPosition()).getBlogId();
                    String currentId = notificationList.get(getAdapterPosition()).getCurrentUser();
                    boolean isPost = notificationList.get(getAdapterPosition()).isPost();

                    if (postId == null) {
                        Toast.makeText(context, "Data don't exist anymore!", Toast.LENGTH_SHORT).show();
                    } else {
                        if (isPost) {
                            Intent intent = new Intent(context, PostActivity.class);
                            intent.putExtra(Constants.BLOG_ID_STRING_TEXT, postId);
                            context.startActivity(intent);
                        } else {
                            Intent intent = new Intent(context, AccountSetupActivity.class);
                            intent.putExtra(Constants.USER_ID_STRING_EXTRA, currentId);
                            context.startActivity(intent);
                        }
                    }

                }
            });
        }

        private void setImage(String imageUri, String thumbnailUri, ImageView img, int placeholder) {
            RequestOptions requestOptions = new RequestOptions();
            Glide.with(context).applyDefaultRequestOptions(requestOptions.placeholder(placeholder)).load(imageUri)
                    .thumbnail(Glide.with(context).load(thumbnailUri)).into(img);
        }

        private void loadUserData(String id, final CircleImageView img, final String txt) {
            firebaseFirestore.collection("Users").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        String profileImage = task.getResult().getString("image");
                        String thumbnail = task.getResult().getString("thumbnail");
                        setImage(profileImage, thumbnail, img, R.drawable.empty_profile);

                        String name = task.getResult().getString("name");
                        String boldUserName = "<b>" + name + "</b> " + txt;
                        txtNotificationText.setText(Html.fromHtml(boldUserName));
                    }
                }
            });
        }

        private void loadPostImage(String id, final ImageView img) {
            firebaseFirestore.collection("Posts").document(id).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                String postImage = task.getResult().getString("imageUrl");
                                String thumbnail = task.getResult().getString("thumbnailUrl");
                                setImage(postImage, thumbnail, img, R.drawable.empty_background);
                            }
                        }
                    });
        }

    }
}
