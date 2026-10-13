package io.github.watchesim.helper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

/** Delegates only to the original companion. No local eUICC API or implicit fallback. */
public final class WatchHandoff {
    public static final String PACKAGE = "com.google.android.apps.wear.companion";
    public static final String ACTIVITY = PACKAGE + ".core.application.RootActivity";
    private WatchHandoff() {}

    public static Intent createIntent(ActivationCode code) {
        if (code.needsConfirmationCode) {
            throw new IllegalArgumentException("confirmation required");
        }
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("madeby.google.com")
                .path("/wear-companion/navigation")
                .appendQueryParameter("dest", "esim_download")
                .appendQueryParameter("activationCode", code.payload)
                .appendQueryParameter("carrier", "Watch plan")
                .build();
        return new Intent(Intent.ACTION_VIEW, uri)
                .setComponent(new ComponentName(PACKAGE, ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static boolean isAvailable(PackageManager manager) {
        try {
            ActivityInfo info = manager.getActivityInfo(new ComponentName(PACKAGE, ACTIVITY), 0);
            int activityState = manager.getComponentEnabledSetting(new ComponentName(PACKAGE, ACTIVITY));
            int appState = manager.getApplicationEnabledSetting(PACKAGE);
            return info.exported && info.enabled && info.applicationInfo.enabled
                    && !disabled(activityState) && !disabled(appState);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean disabled(int state) {
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }
}
