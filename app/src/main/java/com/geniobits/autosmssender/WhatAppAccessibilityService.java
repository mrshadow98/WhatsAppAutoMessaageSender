package com.geniobits.autosmssender;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.List;

import static android.content.ContentValues.TAG;

public class WhatAppAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event
                .getEventType()) {
            Log.e(TAG, "ACC::onAccessibilityEvent: event=" + event);


            AccessibilityNodeInfo nodeInfo = event.getSource();
            Log.i(TAG, "ACC::onAccessibilityEvent: nodeInfo=" + nodeInfo);
            if (nodeInfo == null) {
                return;
            }

            // get whatsapp send message button node list
            List<AccessibilityNodeInfo> sendMessageNodeList = nodeInfo.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send");
            for (AccessibilityNodeInfo node : sendMessageNodeList) {
                Log.e(TAG, "ACC::onAccessibilityEvent: send_button " + node);
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                try {
                    Thread.sleep(2000); // some devices cant handle instant back click
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    Thread.sleep(2000);
                    performGlobalAction(GLOBAL_ACTION_BACK);
                } catch (InterruptedException ignored) {
                }
            }

        }
    }


    @Override
    public void onInterrupt() {

    }
}
