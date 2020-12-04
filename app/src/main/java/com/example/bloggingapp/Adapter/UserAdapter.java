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
import com.example.bloggingapp.Model.User;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MainViewHolder> {

    private Context context;
    private List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.user_item_template, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.txtUsername.setText(userList.get(position).getName());
        String imgUri = userList.get(position).getImage();

        holder.setImage(imgUri, "", holder.imgProfilePic, R.drawable.empty_profile);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {

        private TextView txtUsername;
        private ImageView imgProfilePic;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);

            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgProfilePic = itemView.findViewById(R.id.imgProfilePic);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String userId = userList.get(getAdapterPosition()).getUserId();
                    Intent intent = new Intent(context, AccountSetupActivity.class);
                    intent.putExtra("UserId", userId);
                    context.startActivity(intent);
                }
            });
        }

        private void setImage(String imageUri, String thumbnailUri, ImageView img, int placeholder) {
            RequestOptions requestOptions = new RequestOptions();
            Glide.with(context).applyDefaultRequestOptions(requestOptions.placeholder(placeholder)).load(imageUri)
                    .thumbnail(Glide.with(context).load(thumbnailUri)).into(img);
        }
    }
}
