package org.fifi.android.resolveractivitytweaks;

/**
 * Created by phil on 9/30/15.
 */

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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class XposedModule implements IXposedHookLoadPackage {

    private XSharedPreferences mXprefs;

    private void reloadPrefs() {
        if (mXprefs.hasFileChanged()) {
            mXprefs.reload();
        }
    }

    public boolean isEnabled() {
        reloadPrefs();
        return mXprefs.getBoolean(Const.PREF_RAT_ENABLE, Const.PREF_RAT_ENABLE_DEFAULT);
    }

    public boolean shouldHideAlwaysOnce() {
        reloadPrefs();
        return mXprefs.getBoolean(Const.PREF_RAT_HIDE_ONCE_ALWAYS, Const.PREF_RAT_HIDE_ONCE_ALWAYS_DEFAULT);
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            final Class prefActivityClass = XposedHelpers.findClass(BuildConfig.APPLICATION_ID+".RATSettings", lpparam.classLoader);
            final Field mBuildCodeFromXposed = XposedHelpers.findField(prefActivityClass, "mBuildCodeFromXposed");
            XposedBridge.hookAllConstructors(prefActivityClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    mBuildCodeFromXposed.set(param.thisObject, BuildConfig.RANDOM_BUILD_CODE);
                }
            });
        }

        mXprefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, Const.PREFERENCES_NAME);
        mXprefs.makeWorldReadable();

        final Class rlaClass = XposedHelpers.findClass("com.android.internal.app.ResolverActivity$ResolveListAdapter", lpparam.classLoader);
        final Field mListField = XposedHelpers.findField(rlaClass, "mList");
        final Field mFilterLastUsedField = XposedHelpers.findField(rlaClass, "mFilterLastUsed");
        final Field mLastChosenPositionField = XposedHelpers.findField(rlaClass, "mLastChosenPosition");

        XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(rlaClass, "rebuildList"), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isEnabled() && (Boolean)mFilterLastUsedField.get(param.thisObject)) {
                            mFilterLastUsedField.set(param.thisObject, false);
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
        );


        final Class resActClass = XposedHelpers.findClass("com.android.internal.app.ResolverActivity", lpparam.classLoader);
        final Field mAlwaysButtonField = XposedHelpers.findField(resActClass, "mAlwaysButton");
        final Field mOnceButtonField = XposedHelpers.findField(resActClass, "mOnceButton");
        XposedBridge.hookMethod(XposedHelpers.findMethodBestMatch(resActClass, "onCreate", Bundle.class, Intent.class, CharSequence.class,int.class, Intent[].class, List.class, boolean.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isEnabled() && shouldHideAlwaysOnce()) {
                            Button mAlwaysButton = (Button) mAlwaysButtonField.get(param.thisObject);
                            Button mOnceButton = (Button) mOnceButtonField.get(param.thisObject);
                            mAlwaysButton.setVisibility(View.GONE);
                            mOnceButton.setVisibility(View.GONE);
                            //ListView listView = (ListView) XposedHelpers.findField(klass, "mListView").get(param.thisObject);
                            //ArrayList<ListView.FixedViewInfo> listViewInfo = (ArrayList<ListView.FixedViewInfo>) XposedHelpers.findField(ListView.class, "mHeaderViewInfos").get(listView);
                            //listViewInfo.get(0).isSelectable = true;
                        }
                    }
                }
        );


        final Field mListViewField = XposedHelpers.findField(resActClass, "mListView");
        final Field mLastSelectedField = XposedHelpers.findField(resActClass, "mLastSelected");
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
    }
}
