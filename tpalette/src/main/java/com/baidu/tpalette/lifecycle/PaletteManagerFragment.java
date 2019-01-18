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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PaletteManagerFragment extends Fragment {
    private static final String TAG = "RMFragment";
    private final ActivityFragmentLifecycle lifecycle;
    private final PaletteManagerTreeNode requestManagerTreeNode =
            new FragmentPaletteManagerTreeNode();
    private final Set<PaletteManagerFragment> childPaletteManagerFragments = new HashSet<>();

    @Nullable
    private PaletteManager paletteManager;
    @Nullable
    private PaletteManagerFragment rootPaletteManagerFragment;
    @Nullable
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


    @NonNull
    public PaletteManagerTreeNode getRequestManagerTreeNode() {
        return requestManagerTreeNode;
    }

    private void addChildRequestManagerFragment(PaletteManagerFragment child) {
        childPaletteManagerFragments.add(child);
    }

    private void removeChildRequestManagerFragment(PaletteManagerFragment child) {
        childPaletteManagerFragments.remove(child);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @NonNull
    Set<PaletteManagerFragment> getDescendantRequestManagerFragments() {
        if (equals(rootPaletteManagerFragment)) {
            return Collections.unmodifiableSet(childPaletteManagerFragments);
        } else if (rootPaletteManagerFragment == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Pre JB MR1 doesn't allow us to get the parent fragment so we can't introspect hierarchy,
            // so just return an empty set.
            return Collections.emptySet();
        } else {
            Set<PaletteManagerFragment> descendants = new HashSet<>();
            for (PaletteManagerFragment fragment : rootPaletteManagerFragment
                    .getDescendantRequestManagerFragments()) {
                if (isDescendant(fragment.getParentFragment())) {
                    descendants.add(fragment);
                }
            }
            return Collections.unmodifiableSet(descendants);
        }
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

    /**
     * Returns true if the fragment is a descendant of our parent.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private boolean isDescendant(@NonNull Fragment fragment) {
        Fragment root = getParentFragment();
        Fragment parentFragment;
        while ((parentFragment = fragment.getParentFragment()) != null) {
            if (parentFragment.equals(root)) {
                return true;
            }
            fragment = fragment.getParentFragment();
        }
        return false;
    }

    private void registerFragmentWithRoot(@NonNull Activity activity) {
        unregisterFragmentWithRoot();
        rootPaletteManagerFragment = TPalette.get(activity).getPaletteManagerRetriever()
                .getRequestManagerFragment(activity.getFragmentManager(), null);
        if (!equals(rootPaletteManagerFragment)) {
            rootPaletteManagerFragment.addChildRequestManagerFragment(this);
        }
    }

    private void unregisterFragmentWithRoot() {
        if (rootPaletteManagerFragment != null) {
            rootPaletteManagerFragment.removeChildRequestManagerFragment(this);
            rootPaletteManagerFragment = null;
        }
    }

    @SuppressWarnings("deprecation")
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

    private class FragmentPaletteManagerTreeNode implements PaletteManagerTreeNode {

        FragmentPaletteManagerTreeNode() {
        }

        @NonNull
        @Override
        public Set<PaletteManager> getDescendants() {
            Set<PaletteManagerFragment> descendantFragments = getDescendantRequestManagerFragments();
            Set<PaletteManager> descendants = new HashSet<>(descendantFragments.size());
            for (PaletteManagerFragment fragment : descendantFragments) {
                if (fragment.getPaletteManager() != null) {
                    descendants.add(fragment.getPaletteManager());
                }
            }
            return descendants;
        }

        @Override
        public String toString() {
            return super.toString() + "{fragment=" + PaletteManagerFragment.this + "}";
        }
    }
}
