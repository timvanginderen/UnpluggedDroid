package co.gounplugged.unpluggeddroid.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.activities.ChatActivity;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Message;

public class NotificationHelper {

    private NotificationHelper() {}

    public static Notification buildIncomingMessageNotification(Context context, Message message) {
        Contact participant = message.getConversation().getParticipant();

        //build icon
        Drawable drawable = ImageUtil.getDrawableFromUri(context, participant.getImageUri());
        Bitmap bmp = ImageUtil.drawableToBitmap(drawable);
//        RoundedBitmapDrawable roundedBitmapDrawable = ImageUtil.getCircularDrawable(context, bmp);
//        Bitmap theBitmap = ImageUtil.drawableToBitmap(roundedBitmapDrawable);


//        Bitmap bmp = ImageUtil.getRoundedContactBitmap(context, participant);

        Intent resultIntent = new Intent(context, ChatActivity.class);
        resultIntent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, message.getConversation().getId());
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(bmp)
                .setContentTitle("Message from " + participant.getName())
                .setContentText(message.getText())
                .setContentIntent(resultPendingIntent)
                .setWhen(message.getTimeStamp());

        return builder.build();
    }
}
