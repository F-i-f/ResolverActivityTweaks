package com.fifsource.android.resolveractivitytweaks;

/**
 * Created by phil on 10/1/15.
 */
class Const {
    @SuppressWarnings("SpellCheckingInspection")
    public static final String[] XPOSED_INSTALLER_PACKAGE_NAMES = new String[] {
            "com.solohsu.android.edxp.manager",
            "de.robv.android.xposed.installer"
    };
    public static final String XPOSED_INSTALLER_OPEN_SECTION = ".OPEN_SECTION";

    public static final String  PREFERENCES_NAME = "prefs";
    public static final String  PREF_RAT_COPYRIGHT = "rat_copyright";
    public static final String  PREF_RAT_CATEGORY_NO_XPOSED = "rat_category_no_xposed";
    public static final String  PREF_RAT_NO_XPOSED_UNINSTALL = "rat_no_xposed_uninstall";
    public static final String  PREF_RAT_CATEGORY_XPOSED_INACTIVE = "rat_category_xposed_inactive";
    public static final String  PREF_RAT_XPOSED_INACTIVE_ACTIVATE = "rat_xposed_inactive_activate";
    public static final String  PREF_RAT_CATEGORY_XPOSED_MISMATCH = "rat_category_xposed_mismatch";
    public static final String  PREF_RAT_CATEGORY_XPOSED = "rat_category_xposed";
    public static final String  PREF_RAT_ENABLE = "rat_enable";
    public static final boolean PREF_RAT_ENABLE_DEFAULT = false;
    public static final String  PREF_RAT_HIDE_ONCE_ALWAYS = "rat_hideOnceAlways";
    public static final boolean PREF_RAT_HIDE_ONCE_ALWAYS_DEFAULT = false;
    public static final String  PREF_RAT_CATEGORY_LAUNCHER = "rat_category_launcher";
    public static final String  PREF_RAT_SHOW_LAUNCHER_ICON = "rat_showLauncherIcon";
    public static final boolean PREF_RAT_SHOW_LAUNCHER_ICON_DEFAULT = true;
    public static final String  PREF_RAT_BUILD_CODE = "rat_build_code";
    public static final String  PREF_RAT_GIT_REVISION = "rat_git_revision";

}
