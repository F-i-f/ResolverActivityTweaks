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
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.CharSequence;
import java.lang.reflect.Field;
import java.util.List;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

public class XposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private XSharedPreferences mPreferences;

    private void reloadPrefs() {
        //noinspection ConstantConditions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                && mPreferences.hasFileChanged()) {
            mPreferences.reload();
        }
    }

    private boolean isEnabled() {
        reloadPrefs();
        return mPreferences.getBoolean(Const.PREF_RAT_ENABLE, Const.PREF_RAT_ENABLE_DEFAULT);
    }

    private boolean shouldHideAlwaysOnce() {
        reloadPrefs();
        return mPreferences.getBoolean(Const.PREF_RAT_HIDE_ONCE_ALWAYS, Const.PREF_RAT_HIDE_ONCE_ALWAYS_DEFAULT);
    }

    @SuppressWarnings("RedundantThrows")
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("RAT: Starting ResolverActivityTweaks v. " + BuildConfig.VERSION_NAME + " (" + BuildConfig.RANDOM_BUILD_CODE + ")");
        mPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, Const.PREFERENCES_NAME);
        mPreferences.makeWorldReadable();
    }

    @SuppressWarnings("RedundantThrows")
    public void handleLoadPackage(final LoadPackageParam param) throws Throwable {

        if (param.packageName.equals(BuildConfig.APPLICATION_ID)) {
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
                        if (isEnabled() && (Boolean)mFilterLastUsedField.get(param.thisObject)) {
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
                        if (isEnabled() && shouldHideAlwaysOnce()) {
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
                                if (isEnabled() && shouldHideAlwaysOnce()) {
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
                            if (isEnabled() && shouldHideAlwaysOnce()) {
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
