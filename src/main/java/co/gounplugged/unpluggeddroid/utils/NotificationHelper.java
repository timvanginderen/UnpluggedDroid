package co.gounplugged.unpluggeddroid.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Message;

public class NotificationHelper {

    private NotificationHelper() {}

    public static Notification buildIncomingMessageNotification(Context context, Message message) {
        Contact participant = message.getConversation().getParticipant();
        Drawable drawable = ImageUtil.getDrawableFromUri(context, participant.getImageUri());
        Bitmap bmp = ImageUtil.drawableToBitmap(drawable);
        RoundedBitmapDrawable roundedBitmapDrawable = ImageUtil.getCircularDrawable(context, bmp);
        Bitmap theBitmap = ImageUtil.drawableToBitmap(roundedBitmapDrawable);

//        Bitmap bitmapAvatar = ImageUtil.getBitmapFromUri(context, participant.getImageUri());
//        RoundedBitmapDrawable bitmapDrawable = ImageUtil.getCircularDrawable(context, bitmapAvatar);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(theBitmap)
                .setContentTitle("Message from " + participant.getName())
                .setContentText(message.getText())
//                .setContentIntent(getCallActivityAction())
                .setWhen(message.getTimeStamp());

        return builder.build();
    }
}
