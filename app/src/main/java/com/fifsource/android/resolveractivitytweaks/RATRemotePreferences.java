package com.fifsource.android.resolveractivitytweaks;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class RATRemotePreferences extends RemotePreferenceProvider {
    public RATRemotePreferences() {
        super(Const.REMOTE_PREFERENCE_AUTHORITY,
                new String[] { Const.PREFERENCES_NAME });
    }

    @Override
    protected boolean checkAccess(String prefFileName, String prefKey, boolean write) {
        // Only allow read access
        return !write;
    }
}
