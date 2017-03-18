package com.hgil.contactbackup.util.ui;

import android.os.Build;
import android.support.design.R;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/**
 * Created by mohan.giri on 27-02-2017.
 */

public class SnackbarUtil {

    public static void showSnackbar(View view, String message) {
        // make snackbar
        Snackbar mSnackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        // get snackbar view
        View mView = mSnackbar.getView();
        // get textview inside snackbar view
        TextView mTextView = (TextView) mView.findViewById(R.id.snackbar_text);
        // set text to center
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            mTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        else
            mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        // show the snackbar
        mSnackbar.show();
    }
}
