// Resolver Activity Tweaks - Xposed module to tweak the Android
// Resolver Activity
// Copyright (C) 2015-2019 Philippe Troin (F-i-f on Github)
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.fifsource.android.resolveractivitytweaks;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.CharSequence;
import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.crossbowffs.remotepreferences.RemotePreferences;

public class XposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private boolean mInSettings;
    private SharedPreferences mPreferences;

    public XposedModule() {
        mInSettings = false;
        mPreferences = null;
    }

    private SharedPreferences getPreferences(XC_MethodHook.MethodHookParam param) throws Throwable {
        if ( mPreferences == null ) {
            Context ctx = getContext(param.thisObject);
            if (mInSettings) {
                mPreferences = ctx.getSharedPreferences(Const.PREFERENCES_NAME, Context.MODE_PRIVATE);
            } else {
                mPreferences = new RemotePreferences(ctx, Const.REMOTE_PREFERENCE_AUTHORITY, Const.PREFERENCES_NAME);
            }
        }
        return mPreferences;
    }

    private boolean isEnabled(XC_MethodHook.MethodHookParam param) throws Throwable {
        return getPreferences(param).getBoolean(Const.PREF_RAT_ENABLE, Const.PREF_RAT_ENABLE_DEFAULT);
    }

    private boolean shouldHideAlwaysOnce(XC_MethodHook.MethodHookParam param) throws Throwable {
        return getPreferences(param).getBoolean(Const.PREF_RAT_HIDE_ONCE_ALWAYS, Const.PREF_RAT_HIDE_ONCE_ALWAYS_DEFAULT);
    }

    // Lifted from https://github.com/M66B/XPrivacyLua/blob/410ae46a051be7d7d8d87fab7c13b0680a6d22e2/app/src/main/java/eu/faircode/xlua/XLua.java
    // Method of the same name in XPrivacyLua by @M66B.
    @NonNull
    private Context getContext(@NonNull Object am) throws Throwable {
        // Searching for context
        Context context = null;
        Class<?> cAm = am.getClass();
        while (cAm != null && context == null) {
            for (Field field : cAm.getDeclaredFields())
                if (field.getType().equals(Context.class)) {
                    field.setAccessible(true);
                    context = (Context) field.get(am);
                    break;
                }
            cAm = cAm.getSuperclass();
        }
        if (context == null)
            throw new Throwable("Context not found");

        return context;
    }

    @SuppressWarnings("RedundantThrows")
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("RAT: Starting ResolverActivityTweaks v. " + BuildConfig.VERSION_NAME + " (" + BuildConfig.RANDOM_BUILD_CODE + ")");
    }

    @SuppressWarnings("RedundantThrows")
    public void handleLoadPackage(final LoadPackageParam param) throws Throwable {

        if (param.packageName.equals(BuildConfig.APPLICATION_ID)) {
            mInSettings = true;
            final Class prefActivityClass = XposedHelpers.findClass(BuildConfig.APPLICATION_ID+".RATSettings", param.classLoader);
            final Field mBuildCodeFromXposed = XposedHelpers.findField(prefActivityClass, "mBuildCodeFromXposed");
            XposedBridge.hookAllConstructors(prefActivityClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    mBuildCodeFromXposed.set(param.thisObject, BuildConfig.RANDOM_BUILD_CODE);
                }
            });
        }

        final Class rlaClass = XposedHelpers.findClass("com.android.internal.app.ResolverActivity$ResolveListAdapter", param.classLoader);
        Field mListField_;
        try {
            // LP
            mListField_ = XposedHelpers.findField(rlaClass, "mList");
        } catch (java.lang.NoSuchFieldError ex) {
            // MM
            mListField_ = XposedHelpers.findField(rlaClass, "mDisplayList");
        }
        final Field mListField = mListField_;
        final Field mFilterLastUsedField = XposedHelpers.findField(rlaClass, "mFilterLastUsed");
        final Field mLastChosenPositionField = XposedHelpers.findField(rlaClass, "mLastChosenPosition");

        XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(rlaClass, "rebuildList"), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isEnabled(param) && (Boolean)mFilterLastUsedField.get(param.thisObject)) {
                            mFilterLastUsedField.set(param.thisObject, false);
                            if (mListField != null) {
                                @SuppressWarnings("unchecked")
                                List<Object> mList = (List<Object>) mListField.get(param.thisObject);
                                int mLastChosenPosition = (Integer) mLastChosenPositionField.get(param.thisObject);
                                if (mLastChosenPosition > 0) {
                                    Object t = mList.get(0);
                                    //noinspection
                                    mList.set(0, mList.get(mLastChosenPosition));
                                    mList.set(mLastChosenPosition, t);
                                }
                            }
                        }
                    }
                }
        );

        final Class resActClass = XposedHelpers.findClass("com.android.internal.app.ResolverActivity", param.classLoader);
        XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(resActClass, "onCreate", Bundle.class, Intent.class, CharSequence.class, int.class, Intent[].class, List.class, boolean.class),
                new XC_MethodHook() {
                    @SuppressWarnings("RedundantThrows")
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isEnabled(param) && shouldHideAlwaysOnce(param)) {
                            param.args[6] = false;
                        }
                    }
                }
        );

        Field mListViewField_;
        try {
            // LP
            mListViewField_ = XposedHelpers.findField(resActClass, "mListView");
        } catch (java.lang.NoSuchFieldError ex) {
            // MM
            mListViewField_ = null;
        }
        final Field mLastSelectedField = XposedHelpers.findField(resActClass, "mLastSelected");
        if (mListViewField_ != null) {
            // LP
            final Field mListViewField = mListViewField_;
            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(resActClass, "onItemClick", AdapterView.class, View.class, int.class, long.class),
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (isEnabled(param) && shouldHideAlwaysOnce(param)) {
                                    mLastSelectedField.set(param.thisObject,
                                            ((ListView) mListViewField.get(param.thisObject)).getCheckedItemPosition());

                                }
                            }
                        }
                );
        } else {
            // MM
            final Class itemClickClass = XposedHelpers.findClass("com.android.internal.app.ResolverActivity$ItemClickListener", param.classLoader);
            final Field mAdapterViewField = XposedHelpers.findField(resActClass, "mAdapterView");
            final Field itemClickClassParentField = XposedHelpers.findField(itemClickClass, "this$0"); // From http://c2.com/cgi/wiki?ReflectionOnInnerClasses
            XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(itemClickClass, "onItemClick", AdapterView.class, View.class, int.class, long.class),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (isEnabled(param) && shouldHideAlwaysOnce(param)) {
                                Object parentInstance = itemClickClassParentField.get(param.thisObject);
                                mLastSelectedField.set(parentInstance,
                                        ((AbsListView) mAdapterViewField.get(parentInstance)).getCheckedItemPosition());
                            }
                        }
                    }
            );
        }
    }
}
