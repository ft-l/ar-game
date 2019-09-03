package com.example.argame;

import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.util.Collections;
import java.util.Set;

public class MyArFragment extends MyBaseArFragment {
    private static final String TAG = "MyArFragment";

    @Override
    public boolean isArRequired() {
        return true;
    }

    @Override
    public String[] getAdditionalPermissions() {
        return new String[0];
    }

    @Override
    protected void handleSessionException(UnavailableException sessionException) {

        String message;
        if (sessionException instanceof UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore";
        } else if (sessionException instanceof UnavailableApkTooOldException) {
            message = "Please update ARCore";
        } else if (sessionException instanceof UnavailableSdkTooOldException) {
            message = "Please update this app";
        } else if (sessionException instanceof UnavailableDeviceNotCompatibleException) {
            message = "This device does not support AR";
        } else {
            message = "Failed to create AR session";
        }
        Log.e(TAG, "Error: " + message, sessionException);
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        return new Config(session);
    }


    @Override
    protected Set<Session.Feature> getSessionFeatures() {
        return Collections.emptySet();
    }
}
