package com.aswin.reelsblocker;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Leaves Instagram Reels as soon as they appear.
 *
 * The service is limited to the Instagram package in accessibility_service_config.xml,
 * so Android only delivers events (and this code only runs) while Instagram is open.
 */
public class ReelsBlockerService extends AccessibilityService {

    static final String PREFS = "reels_blocker";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_BLOCKED_COUNT = "blocked_count";

    private static final String IG = "com.instagram.android";
    private static final String ID = IG + ":id/";

    /** Bottom-bar Reels tab. */
    private static final String[] REELS_TAB_IDS = {"clips_tab"};
    /** Views that exist only while a reel is playing full screen. */
    private static final String[] REELS_VIEWER_IDS = {
            "clips_viewer_view_pager", "clips_viewer_container", "clips_video_container"
    };
    /** Tabs we can safely jump to instead. */
    private static final String[] HOME_TAB_IDS = {"feed_tab"};

    private static final long ACTION_GAP_MS = 600;
    private static final long TOAST_GAP_MS = 4000;

    private long lastActionAt;
    private long lastToastAt;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!IG.contentEquals(event.getPackageName())) return;

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, true)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !IG.contentEquals(root.getPackageName())) return;

        boolean reelsTabOpen = isReelsTabSelected(root);
        boolean reelPlaying = hasVisibleNode(root, REELS_VIEWER_IDS);
        if (!reelsTabOpen && !reelPlaying) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastActionAt < ACTION_GAP_MS) return;
        lastActionAt = now;

        // Inside the Reels tab: switch to Home. Anywhere else (a reel opened from the
        // feed, Explore or a DM): go back to where the user came from.
        boolean handled = reelsTabOpen && clickFirst(root, HOME_TAB_IDS);
        if (!handled) handled = performGlobalAction(GLOBAL_ACTION_BACK);

        if (handled) {
            prefs.edit().putInt(KEY_BLOCKED_COUNT, prefs.getInt(KEY_BLOCKED_COUNT, 0) + 1).apply();
            if (now - lastToastAt > TOAST_GAP_MS) {
                lastToastAt = now;
                Toast.makeText(this, "Reels blocked", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInterrupt() {
        // Nothing to interrupt.
    }

    private boolean isReelsTabSelected(AccessibilityNodeInfo root) {
        for (String id : REELS_TAB_IDS) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(ID + id);
            if (nodes == null) continue;
            for (AccessibilityNodeInfo n : nodes) {
                if (n != null && n.isSelected()) return true;
            }
        }
        // Fallback for Instagram builds where the tab id changed: a selected node labelled "Reels".
        List<AccessibilityNodeInfo> byText = root.findAccessibilityNodeInfosByText("Reels");
        if (byText != null) {
            for (AccessibilityNodeInfo n : byText) {
                if (n == null || !n.isSelected()) continue;
                CharSequence d = n.getContentDescription();
                if (d != null && "reels".equalsIgnoreCase(d.toString().trim())) return true;
            }
        }
        return false;
    }

    private boolean hasVisibleNode(AccessibilityNodeInfo root, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(ID + id);
            if (nodes == null) continue;
            for (AccessibilityNodeInfo n : nodes) {
                if (n != null && n.isVisibleToUser()) return true;
            }
        }
        return false;
    }

    private boolean clickFirst(AccessibilityNodeInfo root, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(ID + id);
            if (nodes == null) continue;
            for (AccessibilityNodeInfo n : nodes) {
                // The tab itself may not be clickable; walk up to the first clickable parent.
                AccessibilityNodeInfo target = n;
                for (int depth = 0; target != null && depth < 4; depth++) {
                    if (target.isClickable()
                            && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true;
                    }
                    target = target.getParent();
                }
            }
        }
        return false;
    }
}
