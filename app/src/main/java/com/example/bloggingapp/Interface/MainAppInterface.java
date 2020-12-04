package com.example.bloggingapp.Interface;

import android.os.Bundle;

public interface MainAppInterface {

    void gotoProfile(Bundle bundle);

    void gotoComment(Bundle bundle);

    void gotoHome(final String tag, final String warningMessage);

    void openEdit();
}
