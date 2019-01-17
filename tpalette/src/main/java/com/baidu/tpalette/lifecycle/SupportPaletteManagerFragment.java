package com.baidu.tpalette.lifecycle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.baidu.tpalette.PaletteManager;
import com.baidu.tpalette.TPalette;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link android.support.v4.app.Fragment}
 */
public class SupportPaletteManagerFragment extends Fragment {
  private static final String TAG = "SupportRMFragment";
  private final ActivityFragmentLifecycle lifecycle;
  private final PaletteManagerTreeNode requestManagerTreeNode =
      new SupportFragmentPaletteManagerTreeNode();
  private final Set<SupportPaletteManagerFragment> childRequestManagerFragments = new HashSet<>();

  @Nullable
  private SupportPaletteManagerFragment rootRequestManagerFragment;
  @Nullable
  private PaletteManager paletteManager;
  @Nullable
  private Fragment parentFragmentHint;

  public SupportPaletteManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  @VisibleForTesting
  @SuppressLint("ValidFragment")
  public SupportPaletteManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }


  public void setPaletteManager(@Nullable PaletteManager paletteManager) {
    this.paletteManager = paletteManager;
  }

  @NonNull
  ActivityFragmentLifecycle getGlideLifecycle() {
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

  private void addChildRequestManagerFragment(SupportPaletteManagerFragment child) {
    childRequestManagerFragments.add(child);
  }

  private void removeChildRequestManagerFragment(SupportPaletteManagerFragment child) {
    childRequestManagerFragments.remove(child);
  }

  @NonNull
  Set<SupportPaletteManagerFragment> getDescendantRequestManagerFragments() {
    if (rootRequestManagerFragment == null) {
      return Collections.emptySet();
    } else if (equals(rootRequestManagerFragment)) {
      return Collections.unmodifiableSet(childRequestManagerFragments);
    } else {
      Set<SupportPaletteManagerFragment> descendants = new HashSet<>();
      for (SupportPaletteManagerFragment fragment : rootRequestManagerFragment
          .getDescendantRequestManagerFragments()) {
        if (isDescendant(fragment.getParentFragmentUsingHint())) {
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
  private Fragment getParentFragmentUsingHint() {
    Fragment fragment = getParentFragment();
    return fragment != null ? fragment : parentFragmentHint;
  }

  /**
   * Returns true if the fragment is a descendant of our parent.
   */
  private boolean isDescendant(@NonNull Fragment fragment) {
    Fragment root = getParentFragmentUsingHint();
    Fragment parentFragment;
    while ((parentFragment = fragment.getParentFragment()) != null) {
      if (parentFragment.equals(root)) {
        return true;
      }
      fragment = fragment.getParentFragment();
    }
    return false;
  }

  private void registerFragmentWithRoot(@NonNull FragmentActivity activity) {
    unregisterFragmentWithRoot();
    rootRequestManagerFragment = TPalette.get(activity).getRequestManagerRetriever()
        .getSupportRequestManagerFragment(activity.getSupportFragmentManager(), null);
    if (!equals(rootRequestManagerFragment)) {
      rootRequestManagerFragment.addChildRequestManagerFragment(this);
    }
  }

  private void unregisterFragmentWithRoot() {
    if (rootRequestManagerFragment != null) {
      rootRequestManagerFragment.removeChildRequestManagerFragment(this);
      rootRequestManagerFragment = null;
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      registerFragmentWithRoot(getActivity());
    } catch (IllegalStateException e) {
      // OnAttach can be called after the activity is destroyed, see #497.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Unable to register fragment with root", e);
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    parentFragmentHint = null;
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

  private class SupportFragmentPaletteManagerTreeNode implements PaletteManagerTreeNode {

    SupportFragmentPaletteManagerTreeNode() { }

    @NonNull
    @Override
    public Set<PaletteManager> getDescendants() {
      Set<SupportPaletteManagerFragment> descendantFragments =
          getDescendantRequestManagerFragments();
      Set<PaletteManager> descendants = new HashSet<>(descendantFragments.size());
      for (SupportPaletteManagerFragment fragment : descendantFragments) {
        if (fragment.getPaletteManager() != null) {
          descendants.add(fragment.getPaletteManager());
        }
      }
      return descendants;
    }

    @Override
    public String toString() {
      return super.toString() + "{fragment=" + SupportPaletteManagerFragment.this + "}";
    }
  }
}
