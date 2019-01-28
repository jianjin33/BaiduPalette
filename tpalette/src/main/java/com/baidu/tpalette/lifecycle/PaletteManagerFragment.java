package com.baidu.tpalette.lifecycle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.baidu.tpalette.PaletteManager;
import com.baidu.tpalette.TPalette;

import java.util.HashSet;
import java.util.Set;

public class PaletteManagerFragment extends Fragment {
    private static final String TAG = "RMFragment";
    private final ActivityFragmentLifecycle lifecycle;
    private final Set<PaletteManagerFragment> childPaletteManagerFragments = new HashSet<>();

    private PaletteManager paletteManager;
    private PaletteManagerFragment rootPaletteManagerFragment;
    private Fragment parentFragmentHint;

    public PaletteManagerFragment() {
        this(new ActivityFragmentLifecycle());
    }

    @VisibleForTesting
    @SuppressLint("ValidFragment")
    PaletteManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }


    public void setPaletteManager(@Nullable PaletteManager paletteManager) {
        this.paletteManager = paletteManager;
    }

    @NonNull
    ActivityFragmentLifecycle getPaletteLifecycle() {
        return lifecycle;
    }

    @Nullable
    public PaletteManager getPaletteManager() {
        return paletteManager;
    }


    private void addChildPaletteManagerFragment(PaletteManagerFragment child) {
        childPaletteManagerFragments.add(child);
    }

    private void removeChildPaletteManagerFragment(PaletteManagerFragment child) {
        childPaletteManagerFragments.remove(child);
    }

    void setParentFragmentHint(@Nullable Fragment parentFragmentHint) {
        this.parentFragmentHint = parentFragmentHint;
        if (parentFragmentHint != null && parentFragmentHint.getActivity() != null) {
            registerFragmentWithRoot(parentFragmentHint.getActivity());
        }
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Fragment getParentFragmentUsingHint() {
        final Fragment fragment;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fragment = getParentFragment();
        } else {
            fragment = null;
        }
        return fragment != null ? fragment : parentFragmentHint;
    }

    private void registerFragmentWithRoot(@NonNull Activity activity) {
        unregisterFragmentWithRoot();
        rootPaletteManagerFragment = TPalette.get().getPaletteManagerRetriever()
                .getPaletteManagerFragment(activity.getFragmentManager(), null);
        if (!equals(rootPaletteManagerFragment)) {
            rootPaletteManagerFragment.addChildPaletteManagerFragment(this);
        }
    }

    private void unregisterFragmentWithRoot() {
        if (rootPaletteManagerFragment != null) {
            rootPaletteManagerFragment.removeChildPaletteManagerFragment(this);
            rootPaletteManagerFragment = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            registerFragmentWithRoot(activity);
        } catch (IllegalStateException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to register fragment with root", e);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterFragmentWithRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycle.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        lifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();
        unregisterFragmentWithRoot();
    }

    @Override
    public String toString() {
        return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
    }
}
