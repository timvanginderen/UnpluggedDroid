package co.gounplugged.unpluggeddroid.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Message;

public class NotificationHelper {

    private NotificationHelper() {}

    public static Notification buildIncomingMessageNotification(Context context, Message message) {
        Contact participant = message.getConversation().getParticipant();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setLargeIcon(getLargeIcon(call))
                .setContentTitle("Message from " + participant.getName())
                .setContentText(message.getText())
//                .setContentIntent(getCallActivityAction())
                .setWhen(message.getTimeStamp());

        return builder.build();
    }
}
