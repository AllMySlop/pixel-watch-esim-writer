package io.github.watchesim.helper;

import static org.junit.Assert.*;
import android.content.Intent;
import android.app.Activity;
import android.content.pm.*;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import com.google.zxing.integration.android.IntentIntegrator;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35})
public class MainActivityTest {
    private void enter(MainActivity activity, String code) {
        ((EditText) activity.findViewById(R.id.code)).setText(code);
        activity.findViewById(R.id.review).performClick();
    }
    @Test public void requiresReviewAndConfirmationAndInvalidatesOnEdit() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            enter(activity, "LPA:1$smdp.example.com$ABC");
            assertEquals(View.VISIBLE, activity.findViewById(R.id.review_card).getVisibility());
            assertFalse(activity.findViewById(R.id.continue_button).isEnabled());
            ((CheckBox) activity.findViewById(R.id.confirm)).setChecked(true);
            assertTrue(activity.findViewById(R.id.continue_button).isEnabled());
            ((EditText) activity.findViewById(R.id.code)).setText("LPA:1$other.example.com$DEF");
            assertFalse(activity.findViewById(R.id.continue_button).isEnabled());
            assertEquals(View.GONE, activity.findViewById(R.id.review_card).getVisibility());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void missingCompanionDoesNotLaunchBrowserOrPhoneInstaller() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            enter(activity, "LPA:1$smdp.example.com$ABC");
            ((CheckBox) activity.findViewById(R.id.confirm)).setChecked(true);
            activity.findViewById(R.id.continue_button).performClick();
            assertEquals(activity.getString(R.string.missing_companion),
                    ((TextView) activity.findViewById(R.id.status)).getText().toString());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void delegatesToOriginalAppAndClearsCredential() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            PackageInfo info = new PackageInfo();
            info.packageName = WatchHandoff.PACKAGE;
            info.applicationInfo = new ApplicationInfo();
            info.applicationInfo.packageName = info.packageName;
            info.applicationInfo.enabled = true;
            ActivityInfo target = new ActivityInfo();
            target.packageName = info.packageName;
            target.name = WatchHandoff.ACTIVITY;
            target.applicationInfo = info.applicationInfo;
            target.enabled = true;
            target.exported = true;
            info.activities = new ActivityInfo[] {target};
            Shadows.shadowOf(activity.getPackageManager()).installPackage(info);
            String code = "LPA:1$smdp.example.com$ABC-123";
            enter(activity, code);
            ((CheckBox) activity.findViewById(R.id.confirm)).setChecked(true);
            activity.findViewById(R.id.continue_button).performClick();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertNotNull(intent);
            assertEquals(code, intent.getData().getQueryParameter("activationCode"));
            assertEquals(WatchHandoff.PACKAGE, intent.getComponent().getPackageName());
            assertEquals("", ((EditText) activity.findViewById(R.id.code)).getText().toString());
            assertFalse(activity.findViewById(R.id.continue_button).isEnabled());
        }
    }
    @Test public void recreationAndBackgroundingDoNotRetainCredential() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            enter(controller.get(), "LPA:1$smdp.example.com$SECRET");
            controller.recreate();
            assertEquals("", ((EditText) controller.get().findViewById(R.id.code)).getText().toString());
            enter(controller.get(), "LPA:1$smdp.example.com$SECRET");
            controller.pause().stop();
            assertEquals("", ((EditText) controller.get().findViewById(R.id.code)).getText().toString());
        }
    }
    @Test public void rejectsInvalidAndConfirmationRequiredCodes() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            enter(activity, "https://example.com");
            assertEquals(View.GONE, activity.findViewById(R.id.review_card).getVisibility());
            enter(activity, "LPA:1$smdp.example.com$ABC$$1");
            assertEquals(activity.getString(R.string.confirmation_required),
                    ((TextView) activity.findViewById(R.id.status)).getText().toString());
            assertFalse(activity.findViewById(R.id.continue_button).isEnabled());
        }
    }
    @Test public void scannedQrRequiresConfirmationAndDoesNotAutomaticallyLaunchCompanion() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Intent result = new Intent().putExtra("SCAN_RESULT", "LPA:1$smdp.example.com$ABC")
                    .putExtra("SCAN_RESULT_FORMAT", "QR_CODE");
            activity.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, result);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.review_card).getVisibility());
            assertFalse(activity.findViewById(R.id.continue_button).isEnabled());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void cancelledScanDoesNotLaunchAnythingOrShowReview() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_CANCELED, null);
            assertEquals(View.GONE, activity.findViewById(R.id.review_card).getVisibility());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
}
