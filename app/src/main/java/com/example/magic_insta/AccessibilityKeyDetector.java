package com.example.magic_insta;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AccessibilityKeyDetector - Instagram automation service
 * 
 * Uses randomized delays to make automation more human-like and avoid detection:
 * - getRandomDelay(): 3-6 seconds for main operations
 * - getRandomShortDelay(): 1-2 seconds for retries
 * - getRandomVeryShortDelay(): 0.3-0.8 seconds for quick sequential operations
 */
public class AccessibilityKeyDetector extends AccessibilityService {
    private static final String TAG = "AccessKeyDetector";
    private final Random random = new Random();

    private int indexToClick = 1;

    /**
     * Generates a random delay between 3000-6000 milliseconds (3-6 seconds)
     * to make automation more human-like and avoid detection
     */
    private long getRandomDelay() {
        return 3000 + random.nextInt(3001); // 3000-6000 milliseconds
    }

    /**
     * Generates a shorter random delay between 1000-2000 milliseconds (1-2 seconds)
     * for retry operations
     */
    private long getRandomShortDelay() {
        return 1000 + random.nextInt(1001); // 1000-2000 milliseconds
    }

    /**
     * Generates a very short random delay between 300-800 milliseconds
     * for quick sequential operations
     */
    private long getRandomVeryShortDelay() {
        return 300 + random.nextInt(501); // 300-800 milliseconds
    }

    /**
     * Shows the touch blocking overlay to prevent accidental touches during automation
     */
    private void showTouchBlockingOverlay() {
        Intent intent = new Intent(this, TouchBlockOverlayService.class);
        intent.setAction(TouchBlockOverlayService.ACTION_SHOW_OVERLAY);
        startService(intent);
        Log.d(TAG, "Touch blocking overlay requested");
    }

    /**
     * Hides the touch blocking overlay when automation is complete
     */
    private void hideTouchBlockingOverlay() {
        Intent intent = new Intent(this, TouchBlockOverlayService.class);
        intent.setAction(TouchBlockOverlayService.ACTION_HIDE_OVERLAY);
        startService(intent);
        Log.d(TAG, "Touch blocking overlay hide requested");
    }

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "Accessibility Service Connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (MainActivity.isFakeLockscreenEnabled(this)) {
                    handleVolumeUp();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (MainActivity.isInstagramManagerEnabled(this)) {
                    handleInstagramManager();
                    return true;
                } else if (MainActivity.isFakeLockscreenEnabled(this)) {
                    handleVolumeDown();
                    return true;
                }
            }
        }
        return false;
    }

    private void handleVolumeUp() {
        vibrate();
        Log.d(TAG, "Volume Up pressed - vibration triggered");
        
        // Hide overlay if it's showing (in case user wants to stop automation)
        hideTouchBlockingOverlay();
        
        Intent intent = new Intent(this, FakeLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("exit", true);
        startActivity(intent);
    }

    private void handleVolumeDown() {
        vibrate();
        Log.d(TAG, "Volume Down pressed - vibration triggered");
        
        // Hide overlay when fake lock is triggered (non-Instagram automation)
        hideTouchBlockingOverlay();
        
        Intent intent = new Intent(this, FakeLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void handleInstagramManager() {
        vibrate();
        Log.d(TAG, "Instagram Manager - Volume Down pressed");

        // Show touch blocking overlay at the start of automation
        showTouchBlockingOverlay();

        try {
            boolean launched = launchInstagram();
            if (launched) {
                Log.d(TAG, "Instagram app launched successfully");
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    tryNavigateToInstagramProfile(0);
                }, getRandomDelay()); // Random delay between 3-6 seconds for Instagram to load
            } else {
                Log.e(TAG, "Failed to launch Instagram app");
                // Hide overlay if Instagram launch failed
                hideTouchBlockingOverlay();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching Instagram: " + e.getMessage());
            // Hide overlay on error
            hideTouchBlockingOverlay();
        }
    }

    /**
     * Multiple methods to launch Instagram with fallbacks
     */
    private boolean launchInstagram() {
        // Method 1: Try standard package manager launch intent
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(launchIntent);
                Log.d(TAG, "Instagram launched via package manager");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Method 1 failed: " + e.getMessage());
        }

        // Method 2: Try launching via Intent with Instagram's main activity
        try {
            Intent intent = new Intent();
            intent.setClassName("com.instagram.android", "com.instagram.android.activity.MainTabActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.d(TAG, "Instagram launched via explicit activity");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Method 2 failed: " + e.getMessage());
        }

        // Method 3: Try launching via another known Instagram activity
        try {
            Intent intent = new Intent();
            intent.setClassName("com.instagram.android", "com.instagram.android.activity.UrlHandlerActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.d(TAG, "Instagram launched via URL handler activity");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Method 3 failed: " + e.getMessage());
        }

        // Method 4: Try using ACTION_VIEW with Instagram URL
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.instagram.com/"));
            intent.setPackage("com.instagram.android");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.d(TAG, "Instagram launched via ACTION_VIEW");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Method 4 failed: " + e.getMessage());
        }

        // Method 5: Try launching with custom Instagram URL scheme
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("instagram://camera"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.d(TAG, "Instagram launched via custom URL scheme");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Method 5 failed: " + e.getMessage());
        }

        // Method 6: Try resolving activities that can handle Instagram intent
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            
            for (ResolveInfo activity : activities) {
                if (activity.activityInfo.packageName.equals("com.instagram.android")) {
                    Intent launchIntent = new Intent(Intent.ACTION_MAIN);
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    launchIntent.setClassName(activity.activityInfo.packageName, activity.activityInfo.name);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launchIntent);
                    Log.d(TAG, "Instagram launched via resolved activity: " + activity.activityInfo.name);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Method 6 failed: " + e.getMessage());
        }

        // Method 7: Check if Instagram is actually installed
        try {
            PackageManager pm = getPackageManager();
            pm.getPackageInfo("com.instagram.android", PackageManager.GET_ACTIVITIES);
            Log.e(TAG, "Instagram is installed but all launch methods failed");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Instagram is not installed on this device");
        }

        return false;
    }

    private void tryNavigateToInstagramProfile(int attempt) {
        Log.d(TAG, "Attempting to navigate to Instagram profile (Attempt " + (attempt + 1) + ")");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode == null) {
            Log.e(TAG, "Root node is null");
            if (attempt < 3) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> tryNavigateToInstagramProfile(attempt + 1), getRandomDelay());
            } else {
                // Hide overlay if all attempts failed
                Log.e(TAG, "Failed to navigate to Instagram profile after all attempts");
                hideTouchBlockingOverlay();
            }
            return;
        }

        AccessibilityNodeInfo profileButton = findLastClickableFrameLayout(rootNode);
        if (profileButton != null && profileButton.isClickable()) {
            profileButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked profile button");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickOptionsButton();
            }, getRandomDelay()); // Random delay after clicking profile
        } else {
            Log.d(TAG, "Profile button not found");
        }
    }

    private void clickOptionsButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null - can't click Options button");
            return;
        }

        AccessibilityNodeInfo optionsButton = findNodeByContentDescription(rootNode, "Options");

        if (optionsButton != null && optionsButton.isClickable()) {
            optionsButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked on 'Options' button");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickElementByBounds(0, 784, 720, 875);
            }, getRandomDelay());
        } else {
            Log.d(TAG, "'Options' button not found or not clickable");
        }
    }

    private void clickElementByBounds(int left, int top, int right, int bottom) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null - can't click element by bounds");
            return;
        }

        AccessibilityNodeInfo targetNode = findClickableNodeByBounds(rootNode, left, top, right, bottom);
        if (targetNode != null && targetNode.isClickable()) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked archive element");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickArchivedPhotoByIndex(indexToClick);
            }, getRandomDelay());
        } else {
            Log.d(TAG, "No clickable element found with specified bounds");
        }
    }

    private void clickArchivedPhotoByIndex(int indexToClick) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null - can't extract archived photos");
            return;
        }

        List<AccessibilityNodeInfo> matchingPhotos = new ArrayList<>();
        extractPhotoButtons(rootNode, matchingPhotos);

        Log.d(TAG, "Found " + matchingPhotos.size() + " photo buttons matching 'Photo by Dhia'");

        if (indexToClick < matchingPhotos.size()) {
            AccessibilityNodeInfo target = matchingPhotos.get(indexToClick);
            if (target != null && target.isClickable()) {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Clicked archived photo at index " + indexToClick);

                // Wait longer and verify we're on the photo page before logging elements
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    waitForPhotoPageAndLogElements(0);
                }, getRandomDelay()); // Random initial wait

            } else {
                Log.e(TAG, "Target photo not clickable or null");
            }
        } else {
            Log.e(TAG, "Requested index " + indexToClick + " exceeds available photos");
        }
    }

    private void waitForPhotoPageAndLogElements(int attempt) {
        Log.d(TAG, "Checking if photo page is loaded (Attempt " + (attempt + 1) + ")");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode == null) {
            Log.e(TAG, "Root node is null - cannot check photo page");
            if (attempt < 5) {
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        waitForPhotoPageAndLogElements(attempt + 1), getRandomShortDelay());
            }
            return;
        }

        // Check if we're on the photo page by looking for indicators
        boolean isPhotoPage = isOnPhotoViewPage(rootNode);

        if (isPhotoPage) {
            Log.d(TAG, "Photo page detected! Looking for 'More actions for this post' button...");
            clickMoreActionsButton();
        } else {
            Log.d(TAG, "Still not on photo page, waiting...");
            if (attempt < 5) {
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        waitForPhotoPageAndLogElements(attempt + 1), getRandomShortDelay());
            } else {
                Log.e(TAG, "Timeout waiting for photo page to load");
                // Try to click more actions anyway after timeout
                Log.d(TAG, "=== TIMEOUT - TRYING TO CLICK MORE ACTIONS ANYWAY ===");
                clickMoreActionsButton();
            }
        }
    }

    private void clickMoreActionsButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null - can't click More actions button");
            return;
        }

        AccessibilityNodeInfo moreActionsButton = findNodeByContentDescription(rootNode, "More actions for this post");

        if (moreActionsButton != null && moreActionsButton.isClickable()) {
            moreActionsButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked 'More actions for this post' button");

            // Wait longer for menu to fully load and then try to click "Show on Profile"
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tryClickShowOnProfileButton(0);
            }, getRandomDelay()); // Random wait time for menu to load
        } else {
            Log.e(TAG, "'More actions for this post' button not found or not clickable");
            // Log current elements to see what's available
            Log.d(TAG, "=== CURRENT PAGE ELEMENTS (More actions not found) ===");
            logClickableElementsRecursive(rootNode, 0);
            Log.d(TAG, "=== CURRENT PAGE ELEMENTS END ===");
        }
    }

    private void tryClickShowOnProfileButton(int attempt) {
        Log.d(TAG, "Trying to click 'Show on Profile' button (Attempt " + (attempt + 1) + ")");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode == null) {
            Log.e(TAG, "Root node is null - can't click Show on Profile button");
            if (attempt < 5) {
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        tryClickShowOnProfileButton(attempt + 1), getRandomShortDelay());
            }
            return;
        }

        // Find the "Show on profile" button
        AccessibilityNodeInfo showOnProfileButton = findShowOnProfileButton(rootNode);

        if (showOnProfileButton != null) {
            // Log button details before clicking
            Rect bounds = new Rect();
            showOnProfileButton.getBoundsInScreen(bounds);
            Log.d(TAG, "Found 'Show on Profile' button - Class: " + showOnProfileButton.getClassName() +
                    ", Text: '" + showOnProfileButton.getText() +
                    "', Bounds: " + bounds.flattenToString() +
                    ", Enabled: " + showOnProfileButton.isEnabled() +
                    ", Clickable: " + showOnProfileButton.isClickable());

            // Try improved click method
            boolean clickSuccess = performEnhancedClick(showOnProfileButton);

            if (clickSuccess) {
                Log.d(TAG, "Successfully clicked 'Show on Profile' button");

                // Wait and verify the action was successful
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    verifyShowOnProfileClick();
                }, getRandomShortDelay());

                // wait random time and then close the insta app
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    closeInstagramApp();
                }, getRandomDelay());
            } else {
                Log.e(TAG, "Failed to click 'Show on Profile' button");
                if (attempt < 3) {
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            tryClickShowOnProfileButton(attempt + 1), getRandomShortDelay());
                } else {
                    Log.e(TAG, "All attempts to click 'Show on Profile' failed");
                    // Hide overlay if all attempts failed
                    hideTouchBlockingOverlay();
                }
            }
        } else {
            Log.e(TAG, "'Show on Profile' button not found (Attempt " + (attempt + 1) + ")");
            if (attempt < 3) {
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        tryClickShowOnProfileButton(attempt + 1), getRandomShortDelay());
            } else {
                Log.e(TAG, "Could not find 'Show on Profile' button after all attempts");
                // Hide overlay if all attempts failed
                hideTouchBlockingOverlay();
                // Log current elements for debugging
                Log.d(TAG, "=== FINAL DEBUG - ALL CLICKABLE ELEMENTS ===");
                logClickableElementsRecursive(rootNode, 0);
                Log.d(TAG, "=== FINAL DEBUG END ===");
            }
        }
    }

    private boolean performEnhancedClick(AccessibilityNodeInfo node) {
        if (node == null) {
            Log.e(TAG, "Cannot click null node");
            return false;
        }

        // Method 1: Direct click with focus
        try {
            Log.d(TAG, "Attempting Method 1: Focus + Click");
            boolean focused = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Log.d(TAG, "Focus result: " + focused);

            // Small delay to ensure focus is set
            Thread.sleep(200);

            boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Click result: " + clicked);

            if (clicked) {
                Log.d(TAG, "Method 1 successful");
                return true;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Method 1 interrupted: " + e.getMessage());
        }

        // Method 2: Accessibility focus + click
        try {
            Log.d(TAG, "Attempting Method 2: Accessibility Focus + Click");
            boolean accFocused = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            Log.d(TAG, "Accessibility focus result: " + accFocused);

            Thread.sleep(300);

            boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Click after acc focus result: " + clicked);

            if (clicked) {
                Log.d(TAG, "Method 2 successful");
                return true;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Method 2 interrupted: " + e.getMessage());
        }

        // Method 3: Parent click if available
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null && parent.isClickable()) {
            Log.d(TAG, "Attempting Method 3: Parent Click");
            boolean parentClicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Parent click result: " + parentClicked);
            if (parentClicked) {
                Log.d(TAG, "Method 3 successful");
                return true;
            }
        }

        Log.e(TAG, "All enhanced click methods failed");
        return false;
    }

    private AccessibilityNodeInfo findClickableAtCoordinates(AccessibilityNodeInfo node, int x, int y) {
        if (node == null) return null;

        if (node.isClickable()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.contains(x, y)) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findClickableAtCoordinates(node.getChild(i), x, y);
            if (result != null) return result;
        }

        return null;
    }

    private void verifyShowOnProfileClick() {
        Log.d(TAG, "=== VERIFYING SHOW ON PROFILE CLICK ===");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            // Check if we still see the "Show on profile" button
            AccessibilityNodeInfo showButton = findShowOnProfileButton(rootNode);
            if (showButton != null) {
                Log.e(TAG, "VERIFICATION FAILED: 'Show on profile' button still visible");
                // Try one more time with a different approach
                Log.d(TAG, "Attempting final click with all methods...");
                performEnhancedClick(showButton);
            } else {
                Log.d(TAG, "VERIFICATION SUCCESSFUL: 'Show on profile' button no longer visible");
                // closeInstagramApp();
            }
        }
        Log.d(TAG, "=== VERIFICATION END ===");
    }

    private void closeInstagramApp() {
        Log.d(TAG, "Closing Instagram app...");
        
        // Hide the touch blocking overlay when automation is complete
        hideTouchBlockingOverlay();
        
        try {
            // Method 1: Use home button to go to home screen
            performGlobalAction(GLOBAL_ACTION_HOME);
            Log.d(TAG, "Instagram app closed using HOME action");
        } catch (Exception e) {
            Log.e(TAG, "Error closing Instagram app: " + e.getMessage());

            // Method 2: Alternative - try to go back multiple times
            try {
                performGlobalAction(GLOBAL_ACTION_BACK);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }, getRandomVeryShortDelay());
                Log.d(TAG, "Instagram app closed using BACK actions");
            } catch (Exception e2) {
                Log.e(TAG, "Error with BACK action: " + e2.getMessage());
            }
        }
    }

    private AccessibilityNodeInfo findShowOnProfileButton(AccessibilityNodeInfo node) {
        if (node == null) return null;

        // Search by exact text match first
        AccessibilityNodeInfo byText = findNodeByExactText(node, "Show on profile");
        if (byText != null) {
            Log.d(TAG, "Found 'Show on profile' by exact text");
            return byText;
        }

        // Search by case-insensitive text match
        AccessibilityNodeInfo byCaseInsensitive = findNodeByCaseInsensitiveText(node, "show on profile");
        if (byCaseInsensitive != null) {
            Log.d(TAG, "Found 'Show on profile' by case-insensitive text");
            return byCaseInsensitive;
        }

        // Search by partial text match
        AccessibilityNodeInfo byPartialText = findNodeByPartialText(node, "Show on profile");
        if (byPartialText != null) {
            Log.d(TAG, "Found 'Show on profile' by partial text");
            return byPartialText;
        }

        // Search specifically for Button class with the expected bounds
        AccessibilityNodeInfo byButtonBounds = findButtonByBounds(node, 144, 903, 576, 1001);
        if (byButtonBounds != null) {
            Log.d(TAG, "Found 'Show on profile' by button bounds");
            return byButtonBounds;
        }

        return null;
    }

    private AccessibilityNodeInfo findNodeByExactText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;

        if (node.getText() != null && text.equals(node.getText().toString())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByExactText(node.getChild(i), text);
            if (result != null) return result;
        }

        return null;
    }

    private AccessibilityNodeInfo findNodeByCaseInsensitiveText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;

        if (node.getText() != null && text.equalsIgnoreCase(node.getText().toString())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByCaseInsensitiveText(node.getChild(i), text);
            if (result != null) return result;
        }

        return null;
    }

    private AccessibilityNodeInfo findButtonByBounds(AccessibilityNodeInfo node, int left, int top, int right, int bottom) {
        if (node == null) return null;

        if ("android.widget.Button".equals(node.getClassName()) && node.isClickable()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            if (bounds.left == left && bounds.top == top && bounds.right == right && bounds.bottom == bottom) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findButtonByBounds(node.getChild(i), left, top, right, bottom);
            if (result != null) return result;
        }

        return null;
    }

    private AccessibilityNodeInfo findNodeByPartialText(AccessibilityNodeInfo node, String targetText) {
        if (node == null) return null;

        if (node.getText() != null &&
                node.getText().toString().toLowerCase().contains(targetText.toLowerCase())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByPartialText(node.getChild(i), targetText);
            if (result != null) return result;
        }

        return null;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;

        if (node.getText() != null && node.getText().toString().equals(text)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByText(node.getChild(i), text);
            if (result != null) return result;
        }

        return null;
    }

    private boolean isOnPhotoViewPage(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Look for indicators that we're on a photo view page
        // This could be specific UI elements that only appear on photo pages

        // Check for common photo page indicators
        boolean hasBackButton = findNodeByContentDescription(node, "Back") != null;
        boolean hasMoreOptions = findNodeByContentDescription(node, "More options") != null;
        boolean hasImageView = findNodeByClassName(node, "android.widget.ImageView") != null;

        // Check if we no longer see archive-specific elements
        boolean hasArchiveElements = hasArchivePageElements(node);

        // We're likely on photo page if we have photo indicators and no archive elements
        boolean isPhotoPage = (hasBackButton || hasMoreOptions || hasImageView) && !hasArchiveElements;

        Log.d(TAG, "Photo page check - Back: " + hasBackButton +
                ", More: " + hasMoreOptions +
                ", Image: " + hasImageView +
                ", NoArchive: " + !hasArchiveElements +
                ", IsPhoto: " + isPhotoPage);

        return isPhotoPage;
    }

    private boolean hasArchivePageElements(AccessibilityNodeInfo node) {
        // Check if we still see elements specific to the archive page
        List<AccessibilityNodeInfo> photoButtons = new ArrayList<>();
        extractPhotoButtons(node, photoButtons);

        // If we still see multiple "Photo by Dhia" buttons, we're likely still on archive page
        return photoButtons.size() > 1;
    }

    private AccessibilityNodeInfo findNodeByClassName(AccessibilityNodeInfo node, String className) {
        if (node == null) return null;

        if (className.equals(node.getClassName())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByClassName(node.getChild(i), className);
            if (result != null) return result;
        }

        return null;
    }

    private void extractPhotoButtons(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;

        CharSequence desc = node.getContentDescription();
        if ("android.widget.Button".equals(node.getClassName()) &&
                desc != null && desc.toString().startsWith("Photo by Dhia")) {
            result.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            extractPhotoButtons(node.getChild(i), result);
        }
    }

    private AccessibilityNodeInfo findLastClickableFrameLayout(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo result = null;

        if ("android.widget.FrameLayout".equals(node.getClassName()) && node.isClickable()) {
            result = node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo temp = findLastClickableFrameLayout(node.getChild(i));
            if (temp != null) {
                result = temp;
            }
        }
        return result;
    }

    private AccessibilityNodeInfo findNodeByContentDescription(AccessibilityNodeInfo node, String contentDescription) {
        if (node == null) return null;

        if (node.getContentDescription() != null &&
                node.getContentDescription().toString().equalsIgnoreCase(contentDescription)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByContentDescription(node.getChild(i), contentDescription);
            if (result != null) return result;
        }

        return null;
    }

    private AccessibilityNodeInfo findClickableNodeByBounds(AccessibilityNodeInfo node, int left, int top, int right, int bottom) {
        if (node == null) return null;

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);

        if (node.isClickable() && "android.view.View".equals(node.getClassName())
                && bounds.left == left
                && bounds.top == top
                && bounds.right == right
                && bounds.bottom == bottom) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findClickableNodeByBounds(node.getChild(i), left, top, right, bottom);
            if (found != null) return found;
        }
        return null;
    }

    private void logClickableElementsRecursive(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;

        String indent = "  ".repeat(depth);

        if (node.isClickable() || node.isLongClickable()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            String className = node.getClassName() != null ? node.getClassName().toString() : "null";
            String text = node.getText() != null ? node.getText().toString() : "";
            String contentDesc = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
            String resourceId = node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "";

            Log.d(TAG, indent + "CLICKABLE ELEMENT:");
            Log.d(TAG, indent + "  Class: " + className);
            Log.d(TAG, indent + "  Text: '" + text + "'");
            Log.d(TAG, indent + "  Content Description: '" + contentDesc + "'");
            Log.d(TAG, indent + "  Resource ID: '" + resourceId + "'");
            Log.d(TAG, indent + "  Bounds: " + bounds.flattenToString());
            Log.d(TAG, indent + "  Enabled: " + node.isEnabled());
            Log.d(TAG, indent + "  Child Count: " + node.getChildCount());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            logClickableElementsRecursive(node.getChild(i), depth + 1);
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        // Not used
    }
}