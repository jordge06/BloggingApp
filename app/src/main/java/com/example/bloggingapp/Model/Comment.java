package com.example.bloggingapp.Model;


public class Comment {

    private String comment, userId;
    private String date, time;

    public Comment() { }

    public Comment(String comment, String userId, String date, String time) {
        this.comment = comment;
        this.userId = userId;
        this.date = date;
        this.time = time;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
