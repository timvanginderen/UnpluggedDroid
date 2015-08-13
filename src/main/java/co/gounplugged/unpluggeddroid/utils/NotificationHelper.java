package co.gounplugged.unpluggeddroid.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.activities.ChatActivity;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Message;

public class NotificationHelper {

    private NotificationHelper() {}

    public static Notification buildIncomingMessageNotification(Context context, Message message) {
        Contact participant = message.getConversation().getParticipant();

        //build icon
//        Drawable drawable = ImageUtil.getDrawableFromUri(context, participant.getImageUri());
//        Bitmap bmp = ImageUtil.drawableToBitmap(drawable);
//        RoundedBitmapDrawable roundedBitmapDrawable = ImageUtil.getCircularDrawable(context, bmp);
//        Bitmap theBitmap = ImageUtil.drawableToBitmap(roundedBitmapDrawable);


//        Bitmap bmp = ImageUtil.getRoundedContactBitmap(context, participant);

        Bitmap bmp = ImageUtil.getContactAvatar(context, participant.getLookupKey());

//        Bitmap bitmap = ImageUtil.getCircularBitmap(context, bmp);


//        Bitmap bmp = null;
//        try {
//            bmp = MediaStore.Images.Media.getBitmap(context.getContentResolver(), participant.getImageUri());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(Contact_Id));
//        InputStream photoStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri);
//        BufferedInputStream buf = new BufferedInputStream(photoStream);
//        Bitmap my_btmp = BitmapFactory.decodeStream(buf);

        Intent resultIntent = ChatActivity.newLaunchIntent(context);
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
                .setAutoCancel(true)
                .setWhen(message.getTimeStamp());

        return builder.build();
    }



}
