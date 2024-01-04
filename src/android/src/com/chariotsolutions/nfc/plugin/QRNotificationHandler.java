package com.chariotsolutions.nfc.plugin;

import static android.app.Notification.VISIBILITY_PUBLIC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import org.json.JSONException;
import org.json.JSONObject;

import com.onesignal.notifications.IActionButton;
import com.onesignal.notifications.IDisplayableMutableNotification;
import com.onesignal.notifications.INotificationReceivedEvent;
import com.onesignal.notifications.INotificationServiceExtension;

import com.chariotsolutions.nfc.plugin.IncomingCallActivity;
import com.onesignal.notifications.internal.NotificationReceivedEvent;

import java.math.BigInteger;

public class QRNotificationHandler extends BroadcastReceiver implements INotificationServiceExtension{


    private Vibrator vibrator = null;
    private Context context;
    @Override
    public void onNotificationReceived(INotificationReceivedEvent notificationReceivedEvent) {
        context = notificationReceivedEvent.getContext();
        IDisplayableMutableNotification notification = notificationReceivedEvent.getNotification();

        JSONObject data = notification.getAdditionalData();
        Log.i("OneSignalExample", "Received Notification Data: " + data);

        try {
            if (data.getString("type").equals("video_stream")) {
                if (data.has("event") && (data.getString("event").equals("answered") || data.getString("event").equals("ended"))) {
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (data.has("notif_id")) {
                        notificationManager.cancel(data.getInt("notif_id"));
                    }
                    if (data.getString("event").equals("answered")) {
                        Notification.Builder notifBuilder = new Notification.Builder(context)
                                .setSmallIcon(_getResource(context, "ic_launcher", "mipmap"))
                                .setContentTitle("Call Answered")
                                .setContentText("Call answered by "+ data.getString("answered_by_username"))
                                .setOngoing(true)
                                .setCategory(NotificationCompat.CATEGORY_CALL)
                                .setAutoCancel(true);
                        Notification mNotification = notifBuilder.build();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            mNotification.flags |= Notification.FLAG_INSISTENT;
                        }
//                        notificationManager.notify((int) (System.currentTimeMillis() & 0xfffffff) + 22, mNotification);
                        // notificationReceivedEvent.complete(notification);
                    }
                    else {
//                        notificationReceivedEvent.complete(null);
                    }
                } else {

                    int NOTIFICATION_ID = data.getInt("notif_id");

                    Person incomingCaller = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        incomingCaller = new Person.Builder()
                                .setName("Visitor")
                                .setImportant(true)
                                .build();
                    } else {
//                        notificationReceivedEvent.complete(notification);
                        return;
                    }

                    Intent notificationIntent = new Intent(context.getApplicationContext(), IncomingCallActivity.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // the activity from a service
                    notificationIntent.setAction(Intent.ACTION_MAIN);
                    notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);


                    Intent answerMainIntent = new Intent(context, NfcActivity.class);
                    answerMainIntent.putExtra("notification_id", NOTIFICATION_ID);
                    answerMainIntent.putExtra("data", data.toString());


                    Intent contentMainIntent = new Intent(context, IncomingCallActivity.class);
                    contentMainIntent.putExtra("notification_id", NOTIFICATION_ID);
                    contentMainIntent.putExtra("data", data.toString());


                    Intent declineMainIntent = new Intent(context, QRNotificationHandler.class);
                    declineMainIntent.putExtra("notification_id", NOTIFICATION_ID);

                    PendingIntent declineIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID, declineMainIntent,
                            PendingIntent.FLAG_MUTABLE);
                    PendingIntent answerIntent = PendingIntent.getActivity(context, NOTIFICATION_ID - 20, answerMainIntent, PendingIntent.FLAG_MUTABLE);
//                PendingIntent answerIntent = PendingIntent.getBroadcast(context, 0, answerMainIntent, PendingIntent.FLAG_MUTABLE);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_ID - 35, contentMainIntent, PendingIntent.FLAG_IMMUTABLE);


                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


                    Notification.Builder notifBuilder = new Notification.Builder(context, "qr_video")
//                            .setFullScreenIntent(contentIntent, true)
                            .setContentIntent(contentIntent)
                            .setSmallIcon(_getResource(context, "ic_launcher", "mipmap"))
                            .setContentTitle("Incoming Call")
                            .setContentText("Answer the call to see who")
                            .setOngoing(true)
                            .setCategory(NotificationCompat.CATEGORY_CALL)
                            .setTimeoutAfter(45000)
                            .setAutoCancel(true)
                            .setVisibility(VISIBILITY_PUBLIC)
                            .addPerson(incomingCaller);


//                PendingIntent askUserIntent = askUserIntent(context, topicName, 0, false);
                    // Set notification content intent to take user to fullscreen UI if user taps on the
                    // notification body.
//                builder.setContentIntent(askUserIntent);
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//                    notifBuilder.setStyle(
//                            Notification.CallStyle.forIncomingCall(incomingCaller, declineIntent, answerIntent));
//                } else {
                    notifBuilder.addAction(android.R.drawable.sym_call_missed, "Decline", declineIntent);
                    notifBuilder.addAction(android.R.drawable.sym_action_call, "Answer", answerIntent);
//                }
                    Notification newNotification = notifBuilder.build();
                    notificationManager.cancel(notification.getAndroidNotificationId());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        newNotification.flags |= Notification.FLAG_INSISTENT;
                    }

                    notificationManager.notify(NOTIFICATION_ID, newNotification);

//                startForeground(202, newNotification);
//                notificationReceivedEvent.complete();
//                    startForeground(1124, notifBuilder.build());
//                    notificationReceivedEvent.complete(null);
                }

            } else {
                // notificationReceivedEvent.complete(notification);
            }
        } catch (Exception ex) {
            // dont do anything
            Log.e("chudu", ex.toString());
//            notificationReceivedEvent.complete(notification);
        }

    }


    private int _getResource(Context ctx, String name, String type) {
        String package_name = ctx.getPackageName();
        Resources resources = ctx.getResources();
        return resources.getIdentifier(name, type, package_name);
    }

//    private static PendingIntent askUserIntent(Context context, String topicName, int seq, boolean audioOnly) {
//        Intent intent = new Intent(CallActivity.INTENT_ACTION_CALL_INCOMING, null);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
//                | Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra(Const.INTENT_EXTRA_TOPIC, topicName)
//                .putExtra(Const.INTENT_EXTRA_SEQ, seq)
//                .putExtra(Const.INTENT_EXTRA_CALL_AUDIO_ONLY, audioOnly);
//        intent.setClass(context, CallActivity.class);
//        return PendingIntent.getActivity(context, 101, intent,
//                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//    }



    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int noti_id = intent.getIntExtra("notification_id", -1);

            if (noti_id > 0) {
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.cancel(noti_id);
            }

        }
    }
}
