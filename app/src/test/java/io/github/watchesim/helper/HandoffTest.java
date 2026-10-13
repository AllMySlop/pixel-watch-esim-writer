package io.github.watchesim.helper;

import static org.junit.Assert.*;
import android.app.Application;
import android.content.Intent;
import android.content.pm.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35})
public class HandoffTest {
    @Test public void pinsComponentAndEncodesPayloadExactlyOnce() {
        String raw = "LPA:1$smdp.example.com:443$ABC-123$1.2.3";
        Intent intent = WatchHandoff.createIntent(ActivationCode.parse(raw));
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(WatchHandoff.PACKAGE, intent.getComponent().getPackageName());
        assertEquals(WatchHandoff.ACTIVITY, intent.getComponent().getClassName());
        assertEquals("https", intent.getData().getScheme());
        assertEquals("madeby.google.com", intent.getData().getHost());
        assertEquals("/wear-companion/navigation", intent.getData().getPath());
        assertEquals("esim_download", intent.getData().getQueryParameter("dest"));
        assertEquals(raw, intent.getData().getQueryParameter("activationCode"));
        assertEquals(3, intent.getData().getQueryParameterNames().size());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        assertNull(intent.getExtras());
    }
    @Test public void refusesConfirmationCodeProfiles() {
        assertThrows(IllegalArgumentException.class, () -> WatchHandoff.createIntent(
                ActivationCode.parse("LPA:1$smdp.example.com$ABC$$1")));
    }
    @Test public void requiresAnExportedEnabledCompanionActivity() {
        Application app = RuntimeEnvironment.getApplication();
        PackageManager manager = app.getPackageManager();
        assertFalse(WatchHandoff.isAvailable(manager));
        PackageInfo info = new PackageInfo();
        info.packageName = WatchHandoff.PACKAGE;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = info.packageName;
        info.applicationInfo.enabled = true;
        ActivityInfo activity = new ActivityInfo();
        activity.packageName = info.packageName;
        activity.name = WatchHandoff.ACTIVITY;
        activity.applicationInfo = info.applicationInfo;
        activity.enabled = true;
        activity.exported = true;
        info.activities = new ActivityInfo[] {activity};
        Shadows.shadowOf(manager).installPackage(info);
        assertTrue(WatchHandoff.isAvailable(manager));
        manager.setApplicationEnabledSetting(info.packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
        assertFalse(WatchHandoff.isAvailable(manager));
    }
}
