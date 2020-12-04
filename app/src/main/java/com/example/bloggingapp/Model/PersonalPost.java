package com.example.bloggingapp.Model;

public class PersonalPost {

    private String postId;
    private String postImageUri;
    private String postThumbnailUri;

    public PersonalPost() {
    }

    public PersonalPost(String postId, String postImageUri, String postThumbnailUri) {
        this.postId = postId;
        this.postImageUri = postImageUri;
        this.postThumbnailUri = postThumbnailUri;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostImageUri() {
        return postImageUri;
    }

    public void setPostImageUri(String postImageUri) {
        this.postImageUri = postImageUri;
    }

    public String getPostThumbnailUri() {
        return postThumbnailUri;
    }

    public void setPostThumbnailUri(String postThumbnailUri) {
        this.postThumbnailUri = postThumbnailUri;
    }
}
