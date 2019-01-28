package com.baidu.tpalette.lifecycle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.view.View;

import com.baidu.tpalette.PaletteManager;
import com.baidu.tpalette.TPalette;
import com.baidu.tpalette.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author JianZuming
 * @description 控制PaletteManager的初始化
 * @date 2019/1/21
 */
public class PaletteManagerRetriever implements Handler.Callback {
    static final String FRAGMENT_TAG = "com.baidu.music.common.palette.lifecycle";
    private static final String TAG = "RMRetriever";
    private static final String FRAGMENT_INDEX_KEY = "key";

    /**
     * The top application level RequestManager.
     */
    private volatile PaletteManager applicationManager;

    private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

    @VisibleForTesting
    final Map<android.app.FragmentManager, PaletteManagerFragment> pendingRequestManagerFragments =
            new HashMap<>();

    @VisibleForTesting
    final Map<FragmentManager, SupportPaletteManagerFragment> pendingSupportRequestManagerFragments =
            new HashMap<>();

    // 用于在主线程清理Map中的待定的Fragment
    private final Handler handler;
    private final PaletteManagerFactory factory;

    // 用于记录当前Fragments或Activities包含的views.
    private final ArrayMap<View, Fragment> tempViewToSupportFragment = new ArrayMap<>();
    private final ArrayMap<View, android.app.Fragment> tempViewToFragment = new ArrayMap<>();
    private final Bundle tempBundle = new Bundle();

    public PaletteManagerRetriever(@Nullable PaletteManagerFactory factory) {
        handler = new Handler(Looper.getMainLooper(), this /* Callback */);
        this.factory = factory != null ? factory : DEFAULT_FACTORY;
    }

    private PaletteManager getApplicationManager() {
        // Either an application context or we're on a background thread.
        if (applicationManager == null) {
            synchronized (this) {
                if (applicationManager == null) {
                    TPalette tPalette = TPalette.get();
                    applicationManager =
                            factory.build(tPalette, new ApplicationLifecycle());
                }
            }
        }

        return applicationManager;
    }

    public PaletteManager get(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        } else if (Utils.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context);
            } else if (context instanceof Activity) {
                return get((Activity) context);
            } else if (context instanceof ContextWrapper) {
                return get(((ContextWrapper) context).getBaseContext());
            }
        }

        return getApplicationManager();
    }

    public PaletteManager get(@NonNull FragmentActivity activity) {
        if (Utils.isOnBackgroundThread()) {
            // 如果在子线程中，则认为生命周期为Application
            return get(activity.getApplicationContext());
        } else {
            assertNotDestroyed(activity);
            FragmentManager fm = activity.getSupportFragmentManager();
            // 获取fragment对象，并返回RequestManager对象
            return supportFragmentGet(fm, null /*parentHint*/);
        }
    }

    @SuppressLint("RestrictedApi")
    public PaletteManager get(@NonNull Fragment fragment) {
        Preconditions.checkNotNull(fragment.getActivity(),
                "You cannot start a load on a fragment before it is attached or after it is destroyed");
        if (Utils.isOnBackgroundThread()) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            FragmentManager fm = fragment.getChildFragmentManager();
            return supportFragmentGet(fm, fragment);
        }
    }

    public PaletteManager get(@NonNull Activity activity) {
        if (Utils.isOnBackgroundThread()) { // 子线程中则不管理生命周期，使用整个应用的生命周期
            return get(activity.getApplicationContext());
        } else {
            assertNotDestroyed(activity);
            android.app.FragmentManager fm = activity.getFragmentManager();
            return fragmentGet(fm, null);
        }
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    public PaletteManager get(@NonNull View view) {
        if (Utils.isOnBackgroundThread()) {
            return get(view.getContext().getApplicationContext());
        }

        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(view.getContext(),
                "Unable to obtain a request manager for a view without a Context");
        Activity activity = findActivity(view.getContext());
        // The view might be somewhere else, like a service.
        if (activity == null) {
            return get(view.getContext().getApplicationContext());
        }

        // Support Fragments.
        // Although the user might have non-support Fragments attached to FragmentActivity, searching
        // for non-support Fragments is so expensive pre O and that should be rare enough that we
        // prefer to just fall back to the Activity directly.
        if (activity instanceof FragmentActivity) {
            Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
            return fragment != null ? get(fragment) : get(activity);
        }

        // Standard Fragments.
        android.app.Fragment fragment = findFragment(view, activity);
        if (fragment == null) {
            return get(activity);
        }
        return get(fragment);
    }


    private static void findAllSupportFragmentsWithViews(
            @Nullable Collection<Fragment> topLevelFragments,
            @NonNull Map<View, Fragment> result) {
        if (topLevelFragments == null) {
            return;
        }
        for (Fragment fragment : topLevelFragments) {
            // getFragment()s in the support FragmentManager may contain null values, see #1991.
            if (fragment == null || fragment.getView() == null) {
                continue;
            }
            result.put(fragment.getView(), fragment);
            findAllSupportFragmentsWithViews(fragment.getChildFragmentManager().getFragments(), result);
        }
    }

    private Fragment findSupportFragment(View target, FragmentActivity activity) {
        tempViewToSupportFragment.clear();
        findAllSupportFragmentsWithViews(
                activity.getSupportFragmentManager().getFragments(), tempViewToSupportFragment);
        Fragment result = null;
        View activityRoot = activity.findViewById(android.R.id.content);
        View current = target;
        while (!current.equals(activityRoot)) {
            result = tempViewToSupportFragment.get(current);
            if (result != null) {
                break;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }

        tempViewToSupportFragment.clear();
        return result;
    }

    private android.app.Fragment findFragment(View target, Activity activity) {
        tempViewToFragment.clear();
        findAllFragmentsWithViews(activity.getFragmentManager(), tempViewToFragment);

        android.app.Fragment result = null;

        View activityRoot = activity.findViewById(android.R.id.content);
        View current = target;
        while (!current.equals(activityRoot)) {
            result = tempViewToFragment.get(current);
            if (result != null) {
                break;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
        tempViewToFragment.clear();
        return result;
    }

    // non-support Fragments.
    @TargetApi(Build.VERSION_CODES.O)
    private void findAllFragmentsWithViews(
            @NonNull android.app.FragmentManager fragmentManager,
            @NonNull ArrayMap<View, android.app.Fragment> result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (android.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment.getView() != null) {
                    result.put(fragment.getView(), fragment);
                    findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
                }
            }
        } else {
            findAllFragmentsWithViewsPreO(fragmentManager, result);
        }
    }

    private void findAllFragmentsWithViewsPreO(
            @NonNull android.app.FragmentManager fragmentManager,
            @NonNull ArrayMap<View, android.app.Fragment> result) {
        int index = 0;
        while (true) {
            tempBundle.putInt(FRAGMENT_INDEX_KEY, index++);
            android.app.Fragment fragment = null;
            try {
                fragment = fragmentManager.getFragment(tempBundle, FRAGMENT_INDEX_KEY);
            } catch (Exception e) {
                // This generates log spam from FragmentManager anyway.
            }
            if (fragment == null) {
                break;
            }
            if (fragment.getView() != null) {
                result.put(fragment.getView(), fragment);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
                }
            }
        }
    }

    @Nullable
    private Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static void assertNotDestroyed(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public PaletteManager get(@NonNull android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (Utils.isOnBackgroundThread() || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            android.app.FragmentManager fm = fragment.getChildFragmentManager();
            return fragmentGet(fm, fragment);
        }
    }

    PaletteManagerFragment getPaletteManagerFragment(@NonNull final android.app.FragmentManager fm,
                                                     @Nullable android.app.Fragment parentHint) {
        // 尝试根据id去找到此前创建的RequestManagerFragment
        PaletteManagerFragment current = (PaletteManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (current == null) {
            // 如果没有找到，那么从临时存储中寻找
            current = pendingRequestManagerFragments.get(fm);
            if (current == null) {
                // 如果仍然没有找到，那么新建一个，并添加到临时存储中。
                // 然后开启事务绑定fragment并使用handler发送消息来将临时存储的fragment移除。
                current = new PaletteManagerFragment();
                current.setParentFragmentHint(parentHint);
                pendingRequestManagerFragments.put(fm, current);
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
                handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    private PaletteManager fragmentGet(@NonNull android.app.FragmentManager fm,
                                       @Nullable android.app.Fragment parentHint) {
        // PaletteManagerFragment，并获取绑定到这个fragment的PaletteManager
        PaletteManagerFragment current = getPaletteManagerFragment(fm, parentHint);
        PaletteManager paletteManager = current.getPaletteManager();
        if (paletteManager == null) {
            // 如果获取PaletteManagerFragment还没有绑定过RequestManager，那么就创建RequestManager并绑定到PaletteManagerFragment
            TPalette tPalette = TPalette.get();
            paletteManager = factory.build(tPalette, current.getPaletteLifecycle());
            current.setPaletteManager(paletteManager);
        }
        return paletteManager;
    }

    private PaletteManager supportFragmentGet(@NonNull FragmentManager fm, @Nullable Fragment parentHint) {
        SupportPaletteManagerFragment current = getSupportPaletteManagerFragment(fm, parentHint);
        PaletteManager requestManager = current.getPaletteManager();
        if (requestManager == null) {
            TPalette tPalette = TPalette.get();
            requestManager =
                    factory.build(tPalette, current.getPaletteLifecycle());
            current.setPaletteManager(requestManager);
        }
        return requestManager;
    }

    /**
     * 获取或者创建Fragment对象，避免重复创建Fragment
     *
     * @param fm
     * @param parentHint
     * @return
     */
    SupportPaletteManagerFragment getSupportPaletteManagerFragment(
            @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
        // 查找tag为FRAGMENT_TAG的fragment
        SupportPaletteManagerFragment current =
                (SupportPaletteManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (current == null) {
            // 没有则从HashMap中取出fm
            current = pendingSupportRequestManagerFragments.get(fm);
            if (current == null) {
                // 再没有就创建一个
                current = new SupportPaletteManagerFragment();
                // 当fragment嵌套fragment时才会使用，否则parentHint是null
                current.setParentFragmentHint(parentHint);
                // 将fm添加到HashMap中，防止fragment的重复创建
                pendingSupportRequestManagerFragments.put(fm, current);
                // 添加fragment
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
                // 从HashMap移除fm
                handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean handled = true;
        Object removed = null;
        Object key = null;
        switch (msg.what) {
            case ID_REMOVE_FRAGMENT_MANAGER:
                android.app.FragmentManager fm = (android.app.FragmentManager) msg.obj;
                key = fm;
                removed = pendingRequestManagerFragments.remove(fm);
                break;
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
                FragmentManager supportFm = (FragmentManager) msg.obj;
                key = supportFm;
                removed = pendingSupportRequestManagerFragments.remove(supportFm);
                break;
            default:
                handled = false;
                break;
        }
        if (handled && removed == null && Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
        }
        return handled;
    }

    public interface PaletteManagerFactory {
        PaletteManager build(@NonNull TPalette tPalette, @NonNull Lifecycle lifecycle);
    }

    private static final PaletteManagerFactory DEFAULT_FACTORY = new PaletteManagerFactory() {
        @NonNull
        @Override
        public PaletteManager build(@NonNull TPalette tPalette, @NonNull Lifecycle lifecycle) {
            return new PaletteManager(tPalette, lifecycle);
        }
    };

}
