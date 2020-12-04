package com.example.bloggingapp.Model;

public class Post extends PostId {

    private String imageUrl, thumbnailUrl;
    private String postDesc;
    private String userId;
    private String timeStamp;

    public Post() {
    }

    public Post(String imageUrl, String thumbnailUrl, String postDesc, String userId, String timeStamp) {
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.postDesc = postDesc;
        this.userId = userId;
        this.timeStamp = timeStamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPostDesc() {
        return postDesc;
    }

    public void setPostDesc(String postDesc) {
        this.postDesc = postDesc;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
