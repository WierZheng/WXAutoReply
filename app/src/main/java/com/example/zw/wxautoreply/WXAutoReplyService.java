package com.example.zw.wxautoreply;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.List;

/**
 * Created by wierZ on 2017/4/24 0024.
 * Email:  wier_zheng@163.com
 */

public class WXAutoReplyService extends AccessibilityService {

    private final static String MM_PNAME = "com.tencent.mm";
    private final static String TAG = "WXAutoReplyService";
    boolean hasAction = false;
    boolean locked = false;
    boolean background = false;
    private String name;
    private String scontent;
    AccessibilityNodeInfo itemNodeinfo;
    private KeyguardManager.KeyguardLock kl;
    private Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent accessibilityEvent) {

        int eventType = accessibilityEvent.getEventType();
        Log.d(TAG,"eventType =" + eventType);
        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.d(TAG, "eventType: TYPE_NOTIFICATION_STATE_CHANGED");
                List<CharSequence> texts = accessibilityEvent.getText();
                if (!texts.isEmpty()){
                    for (CharSequence text :texts){
                        String content =text.toString();
                        if (!TextUtils.isEmpty(content)){
                            if (isScreenLocked()){
                                locked=true;
                                Log.d(TAG, "onAccessibilityEvent: the screen is locked");
                                wakeAndUnlock();
                                if (isAppForeground(MM_PNAME)){
                                    background =false;
                                    Log.d(TAG, "onAccessibilityEvent:mm WX in foreground");
                                    sendNotifacationReply(accessibilityEvent);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                       sendNotifacationReply(accessibilityEvent);
                                        if (fill()){
                                            send();
                                        }

                                        }
                                    },1000);
                                }else {
                                    background=true;
                                    Log.d(TAG, "onAccessibilityEvent: mm WX in background");
                                    sendNotifacationReply(accessibilityEvent);

                                }
                            }else {
                                locked =false;
                                Log.d(TAG, "onAccessibilityEvent: the screen is unlocked");
                                if (isAppForeground(MM_PNAME)) {
                                    background = false;
                                    Log.d(TAG, " mm WX in foreground");
                                    sendNotifacationReply(accessibilityEvent);
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (fill()) {
                                                send();
                                            }
                                        }
                                    }, 1000);
                                } else {
                                    background = true;
                                    android.util.Log.d(TAG, "mm WX in background");
                                    sendNotifacationReply(accessibilityEvent);
                                }
                            }
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                android.util.Log.d(TAG, "get type window down event");
                if (!hasAction) break;
                itemNodeinfo = null;
                String className = accessibilityEvent.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    if (fill()) {
                        send();
                    }else {
                        if(itemNodeinfo != null){
                            itemNodeinfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (fill()) {
                                        send();
                                    }
                                    back2Home();
                                    release();
                                    hasAction = false;
                                }
                            }, 1000);
                            break;
                        }
                    }
                }

                //bring2Front();
                back2Home();
                release();
                hasAction = false;
                break;
        }
    }
    /**寻找 “发送” 按钮，并且点击*/
    private void send() {
        AccessibilityNodeInfo nodeInfo =getRootInActiveWindow();
        if (nodeInfo!=null){
            List<AccessibilityNodeInfo> list=nodeInfo.findAccessibilityNodeInfosByText("发送");
            if(list!=null && list.size() > 0){
                for (AccessibilityNodeInfo info :list){
                    if (info.getClassName().equals("android.widget.Button") && info.isEnabled()){
                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo
                        .findAccessibilityNodeInfosByText("Send");
                if (liste != null && liste.size() > 0) {
                    for (AccessibilityNodeInfo n : liste) {
                        if(n.getClassName().equals("android.widget.Button") && n.isEnabled())
                        {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);}
                    }
                }
            }
            pressBackButton();
        }
       
    }

    /**
     *模拟back按键*/
    private void pressBackButton() {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendNotifacationReply(AccessibilityEvent event) {
        hasAction = true;
        if (event.getParcelableData() != null
                && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event
                    .getParcelableData();
            String content = notification.tickerText.toString();
            String[] cc = content.split(":");
            name = cc[0].trim();
            scontent = cc[1].trim();

            Log.i(TAG, "sender name =" + name);
            Log.i(TAG, "sender content =" + scontent);


            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean fill() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            //要自动回复的话
            return findEditText(rootNode, "正在忙,稍后回复你");
        }
        return false;
    }
    /**找到输入文本框 并自动回复*/
    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();

        android.util.Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                android.util.Log.d("maptrix", "nodeinfo = null");
                continue;
            }

            android.util.Log.d("maptrix", "class=" + nodeInfo.getClassName());
            android.util.Log.e("maptrix", "ds=" + nodeInfo.getContentDescription());
            if(nodeInfo.getContentDescription() != null){
                int nindex = nodeInfo.getContentDescription().toString().indexOf(name);
                int cindex = nodeInfo.getContentDescription().toString().indexOf(scontent);
                android.util.Log.e("maptrix", "nindex=" + nindex + " cindex=" +cindex);
                if(nindex != -1){
                    itemNodeinfo = nodeInfo;
                    android.util.Log.i("maptrix", "find node info");
                }
            }
            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                android.util.Log.i("maptrix", "==================");
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }

            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }

        return false;
    }


    private void wakeAndUnlock() {
        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");

        //点亮屏幕
        wl.acquire(1000);

        //得到键盘锁管理器对象
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("unLock");

        //解锁
        kl.disableKeyguard();

    }

    private void release() {

        if (locked && kl != null) {
            android.util.Log.d("maptrix", "release the lock");
            //得到键盘锁管理器对象
            kl.reenableKeyguard();
            locked = false;
        }
    }



    private boolean isScreenLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.inKeyguardRestrictedInputMode();
    }

    /**判断指定的应用是否在前台运行*/
    private boolean isAppForeground(String str) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(MM_PNAME)) {
            return true;
        }

        return false;
    }

    /**
     * 将当前应用运行到前台
     */
    private void bring2Front() {
        ActivityManager activtyManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
            if (this.getPackageName().equals(runningTaskInfo.topActivity.getPackageName())) {
                activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }
    }

    /**
     * 回到系统桌面
     */
    private void back2Home() {
        Intent home = new Intent(Intent.ACTION_MAIN);

        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);

        startActivity(home);
    }


    @Override
    public void onInterrupt() {

    }
}
