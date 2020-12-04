package com.example.bloggingapp.Model;

public class Notification {

    private String text, ownerId, blogId;
    private String currentUser;
    private boolean isPost;

    public Notification() {
    }

    public Notification(String currentUser, String text, String ownerId, String blogId, boolean isPost) {
        this.text = text;
        this.ownerId = ownerId;
        this.blogId = blogId;
        this.isPost = isPost;
        this.currentUser = currentUser;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isPost() {
        return isPost;
    }

    public void setPost(boolean post) {
        isPost = post;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getBlogId() {
        return blogId;
    }

    public void setBlogId(String blogId) {
        this.blogId = blogId;
    }
}
