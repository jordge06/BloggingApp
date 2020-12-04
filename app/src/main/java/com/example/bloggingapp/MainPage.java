package com.example.bloggingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.bloggingapp.Fragments.NewPostFragment;
import com.example.bloggingapp.Fragments.ProfileFragment;
import com.example.bloggingapp.Interface.MainAppInterface;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.theartofdev.edmodo.cropper.CropImage;

public class MainPage extends AppCompatActivity implements MainAppInterface {

    private NavController navController;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setItemIconTintList(null);
        navController = Navigation.findNavController(this, R.id.navHostFragment);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    @Override
    public void gotoProfile(Bundle bundle) {
        navController.navigate(R.id.action_nav_home_to_nav_profile, bundle);
    }

    @Override
    public void gotoComment(Bundle bundle) {
        navController.navigate(R.id.action_nav_home_to_commentFragment, bundle);
    }

    @Override
    public void gotoHome(final String tag, final String warningMessage) {
        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_home, true).build();
        if (tag.equals(ProfileFragment.TAG)) {
            navController.navigate(R.id.action_nav_profile_to_nav_home, null, navOptions);
        }
        NavDestination destination = navController.getCurrentDestination();
        if (destination != null && destination.getId() == R.id.nav_new_post) {
            Bundle bundle = new Bundle();
            bundle.putString("POST_MESSAGE", warningMessage);
            navController.navigate(R.id.action_nav_new_post_to_nav_home, bundle, navOptions);
        }
    }

    @Override
    public void openEdit() {
        navController.navigate(R.id.action_nav_profile_to_editProfileFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return false;
    }

    private void logout() {
        firebaseAuth.signOut();
        openLogin();
    }

    private void openLogin() {
        startActivity(new Intent(MainPage.this, LoginActivity.class));
        finish();
    }

    public interface ImageUriInterface {
        void uri(Uri imageUri);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                NewPostFragment newPostFragment = new NewPostFragment();
                Bundle bundle = new Bundle();
                bundle.putString("ImageUri", result.getUri().toString());
                newPostFragment.setArguments(bundle);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result != null ? result.getError() : null;
                Log.d("MainApp", "onActivityResult: " + error);
            }
        }
    }
}