package com.fifsource.android.resolveractivitytweaks;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class RATSettings extends PreferenceActivity {

    public String mBuildCodeFromXposed = null;

    public RATSettings() {
        // Empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (onIsMultiPane()) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        if (!onIsMultiPane()) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new GeneralPreferenceFragment()).commit();
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static class ReflectInDescriptionBooleanPrefChangeListener implements Preference.OnPreferenceChangeListener {
        private Activity mActivity;
        private boolean mDelayedAfterReboot;
        private int mOnDescrResId;
        private int mOffDescrResId;

        public ReflectInDescriptionBooleanPrefChangeListener(Activity activity, boolean delayedAfterReboot, int onDescrResId, int offDescrResId) {
            mActivity = activity;
            mDelayedAfterReboot = delayedAfterReboot;
            mOnDescrResId = onDescrResId;
            mOffDescrResId = offDescrResId;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            setDescriptionString(preference, value);
            if (mDelayedAfterReboot && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Toast.makeText(mActivity, mActivity.getString(R.string.rat_warning_setting_delayed_after_reboot), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        public void setDescriptionString(Preference preference, Object value) {
            if ((Boolean) value) {
                preference.setSummary(mOnDescrResId);
            } else {
                preference.setSummary(mOffDescrResId);
            }
        }
    }

    private static class ReflectInDescriptionListChangedListener implements Preference.OnPreferenceChangeListener {
        private String[] mEntryValuesArray;
        private String[] mEntryDescriptionArray;

        public ReflectInDescriptionListChangedListener(RATSettings activity, int entryValuesArrayId, int entryDescriptionArrayId) {
            Resources rsrc = activity.getResources();
            mEntryValuesArray = rsrc.getStringArray(entryValuesArrayId);
            mEntryDescriptionArray = rsrc.getStringArray(entryDescriptionArrayId);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            setDescriptionString(preference, value);
            return true;
        }

        public void setDescriptionString(Preference preference, Object value) {
            String stringVal = (String) value;
            int sz = mEntryValuesArray.length;
            int idx;
            for (idx = 0; idx < sz; ++idx) {
                if (stringVal.equals(mEntryValuesArray[idx])) {
                    preference.setSummary(mEntryDescriptionArray[idx]);
                    return;
                }
            }
        }
    }

    private static class ToggleHideOnceAlwaysListener extends ReflectInDescriptionBooleanPrefChangeListener {
        private Preference mDependentPreference;

        public ToggleHideOnceAlwaysListener(Activity activity, boolean delayedAfterReboot, int onDescrResId, int offDescrResId, Preference dependentPreference) {
            super(activity, delayedAfterReboot, onDescrResId, offDescrResId);
            mDependentPreference = dependentPreference;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            setDependentPreference((Boolean)value);
            return super.onPreferenceChange(preference, value);
        }

        public void setDependentPreference(boolean value) {
            mDependentPreference.setEnabled(value);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        public GeneralPreferenceFragment() {
            // Empty
        }

        @SuppressWarnings("deprecation")
        private void makePrefWorldReadable(PreferenceManager prefMgr) {
            prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final RATSettings activity = (RATSettings) getActivity();

            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(Const.PREFERENCES_NAME);
            makePrefWorldReadable(prefMgr);
            addPreferencesFromResource(R.xml.pref_general);

            Preference ratCopyright = findPreference(Const.PREF_RAT_COPYRIGHT);
            ratCopyright.setTitle(ratCopyright.getTitle() + " " + BuildConfig.VERSION_NAME);
            ratCopyright.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.xda_support_thread_link))));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, activity.getString(R.string.rat_error_browser_not_found), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });

            Preference ratBuildCode = findPreference(Const.PREF_RAT_BUILD_CODE);
            ratBuildCode.setSummary(BuildConfig.RANDOM_BUILD_CODE);

            Preference ratGitRevision = findPreference(Const.PREF_RAT_GIT_REVISION);
            ratGitRevision.setSummary(BuildConfig.GIT_REVISION);

            final PackageManager pm = activity.getPackageManager();
            PackageInfo pkg = null;
            try {
                pkg = pm.getPackageInfo(Const.XPOSED_INSTALLER_PACKAGE_NAME, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Nothing
            }
            ArrayList<Preference> sectionsToRemove = new ArrayList<Preference>();

            if (pkg == null) {
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDINACT));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDMISMATCH));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSED));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_LAUNCHER));

                Preference uninstallPref = findPreference(Const.PREF_RAT_NOXPOSED_UNINSTALL);
                uninstallPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri packageURI = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                        try {
                            startActivity(uninstallIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(activity, activity.getString(R.string.rat_error_pm_not_found), Toast.LENGTH_LONG).show();
                        }
                        return true;
                    }
                });
            } else if (activity.mBuildCodeFromXposed == null) {
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_NOXPOSED));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDMISMATCH));

                Preference activateModulePref = findPreference(Const.PREF_RAT_XPOSEDINACT_ACTIVATE);
                activateModulePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            Intent intent = new Intent(Const.XPOSED_INSTALLER_OPEN_SECTION);
                            intent.putExtra("section", "modules");
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            try {
                                Intent intent = pm.getLaunchIntentForPackage(Const.XPOSED_INSTALLER_PACKAGE_NAME);
                                startActivity(intent);
                            } catch (ActivityNotFoundException e2) {
                                Toast.makeText(activity, activity.getString(R.string.rat_error_xposed_installer_not_found), Toast.LENGTH_LONG).show();
                            }
                        }
                        return true;
                    }
                });

            } else if (!activity.mBuildCodeFromXposed.equals(BuildConfig.RANDOM_BUILD_CODE)) {
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_NOXPOSED));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDINACT));
            } else {
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_NOXPOSED));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDINACT));
                sectionsToRemove.add(findPreference(Const.PREF_RAT_CATEGORY_XPOSEDMISMATCH));
            }

            Preference ratEnabledPref = findPreference(Const.PREF_RAT_ENABLE);
            Preference hideOnceAlwaysPref = findPreference(Const.PREF_RAT_HIDE_ONCE_ALWAYS);
            ToggleHideOnceAlwaysListener thoal = new ToggleHideOnceAlwaysListener(activity, true, R.string.rat_enable_description_on, R.string.rat_enable_description_off, hideOnceAlwaysPref);
            ratEnabledPref.setOnPreferenceChangeListener(thoal);
            boolean enableVal = ratEnabledPref.getSharedPreferences().getBoolean(Const.PREF_RAT_ENABLE, Const.PREF_RAT_ENABLE_DEFAULT);
            thoal.setDescriptionString(ratEnabledPref, enableVal);
            thoal.setDependentPreference(enableVal);

            ReflectInDescriptionBooleanPrefChangeListener hideOnceAlwaysChangeListener = new ReflectInDescriptionBooleanPrefChangeListener(activity, true, R.string.rat_hideAlwaysOnce_description_on, R.string.rat_hideAlwaysOnce_description_off);
            hideOnceAlwaysPref.setOnPreferenceChangeListener(hideOnceAlwaysChangeListener);
            hideOnceAlwaysChangeListener.setDescriptionString(hideOnceAlwaysPref, ratEnabledPref.getSharedPreferences().getBoolean(Const.PREF_RAT_HIDE_ONCE_ALWAYS, Const.PREF_RAT_HIDE_ONCE_ALWAYS_DEFAULT));

            Preference showInLauncherPref = findPreference(Const.PREF_RAT_SHOW_LAUNCHER_ICON);
            ReflectInDescriptionBooleanPrefChangeListener showInLauncherPrefChangeListener = new ReflectInDescriptionBooleanPrefChangeListener(activity, false, R.string.rat_showLauncher_description_on, R.string.rat_showLauncher_description_off) {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    RATSettings activity = (RATSettings)getActivity();
                    PackageManager pm = activity.getPackageManager();
                    int val;
                    if ((Boolean)value) {
                        val = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    } else {
                        val = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                    }
                    pm.setComponentEnabledSetting(new ComponentName(activity, BuildConfig.APPLICATION_ID+".RATSettingsAlias"), val, PackageManager.DONT_KILL_APP);
                    return super.onPreferenceChange(preference, value);
                }
            };
            showInLauncherPref.setOnPreferenceChangeListener(showInLauncherPrefChangeListener);
            showInLauncherPrefChangeListener.setDescriptionString(showInLauncherPref, showInLauncherPref.getSharedPreferences().getBoolean(Const.PREF_RAT_SHOW_LAUNCHER_ICON, Const.PREF_RAT_SHOW_LAUNCHER_ICON_DEFAULT));

            PreferenceScreen screen = getPreferenceScreen();
            for (Iterator<Preference> i = sectionsToRemove.iterator(); i.hasNext();) {
                screen.removePreference(i.next());
            }
        }
    }
}
