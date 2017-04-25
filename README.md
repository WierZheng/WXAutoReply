# WXAutoReply
使用AccessibilityService 实现微信自动回复 功能。

来源于： http://www.open-open.com/lib/view/open1468638472552.html 
<br/> 文章已经给出了 很详细的教程和代码，我只不过是整理了过来。 
* * *


AccessibilityService是Android系统框架提供给安装在设备上应用的一个可选的导航反馈特性。
AccessibilityService 可以替代应用与用户交流反馈，比如将文本转化为语音提示，
或是用户的手指悬停在屏幕上一个较重要的区域时的触摸反馈等。
<br/><br/> 首先我们在res文件夹下创建xml文件夹，然后创建一个名为auto_reply_service_config的文件，一会我们会在清单文件中引用它。
``` 
<?xml version="1.0" encoding="utf-8"?>
 <accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeNotificationStateChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_description"
    android:notificationTimeout="100"
    android:packageNames="com.tencent.mm" />
```
 这个文件表示我们对AccessibilityService服务未来侦听的行为做了一些配置，比如 typeNotificationStateChanged 和 typeWindowStateChanged 表示我们需要侦听通知栏的状态变化和窗体状态改变。
 `android:packageNames="com.tencent.mm" ` 这是微信的包名，表示我们只关心微信这一个应用。
