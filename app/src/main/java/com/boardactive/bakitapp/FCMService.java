/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boardactive.bakitapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.boardactive.bakit.BoardActive;
import com.boardactive.bakitapp.utils.Tools;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 *
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 *
 * <intent-filter>
 *   <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String EXTRA_OBJECT = "key.EXTRA_OBJECT";
    public static final String EXTRA_MESSADE_ID = "key.EXTRA_MESSADE_ID";
    private static final String MESSAGE_ID = "MESSAGE_ID";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]



        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "[BAKitApp] From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "[BAKitApp] MessageModel data payload: " + remoteMessage.getData());
            sendNotification(remoteMessage);

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "[BAKitApp] MessageModel Notification Body: " + remoteMessage);
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "[BAKitApp] Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     * Set notification's tap action.
     *
     * @param remoteNotification FCM notification.
     */
    private void sendNotification(final RemoteMessage remoteNotification) {
        Long currentDateandTime = System.currentTimeMillis();
        MessageModel obj = new MessageModel();

        int id = 1;
        obj.setId(id);
        obj.setMessageId(remoteNotification.getData().get("messageId"));
        obj.setNotificationId(remoteNotification.getMessageId());
        obj.setTitle(remoteNotification.getData().get("title"));
        obj.setBody(remoteNotification.getData().get("body"));
        obj.setImageUrl(remoteNotification.getData().get("imageUrl"));
        obj.setLatitude(remoteNotification.getData().get("latitude"));
        obj.setLongitude(remoteNotification.getData().get("longitude"));
        obj.setMessageData(remoteNotification.getData().get("messageData"));
        obj.setIsTestMessage(remoteNotification.getData().get("isTestMessage"));
        obj.setDateCreated(currentDateandTime);
        obj.setDateLastUpdated(currentDateandTime);
        obj.setIsRead(false);

        BoardActive mBoardActive = new BoardActive(getApplicationContext());
        mBoardActive.postEvent(new BoardActive.PostEventCallback() {
            @Override
            public void onResponse(Object value) {
                Log.d(TAG, "[BAKitApp] Received Event: " + value.toString());
            }
        }, "received", obj.getMessageId(), obj.getNotificationId());

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSADE_ID, id);
//        intent.putExtra(EXTRA_OBJECT, obj);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT   
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_HISTORY
        );
        intent.setAction(Intent.ACTION_VIEW);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);


        int notificationType = Tools.getSharedPrecerenceInt(this, NotificationBuilder.NOTIFICATION_KEY);
        new NotificationBuilder(this,pendingIntent, obj, notificationType).execute();

    }



}