package com.example.bloggingapp.Model;

public class User {

    private String name, image;
    private String userId;
    private String lowerCaseName;

    public User() {
    }

    public User(String name, String lowerCaseName, String image, String userId) {
        this.name = name;
        this.image = image;
        this.userId = userId;
        this.lowerCaseName = lowerCaseName;
    }

    public String getLowerCaseName() {
        return lowerCaseName;
    }

    public void setLowerCaseName(String lowerCaseName) {
        this.lowerCaseName = lowerCaseName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
