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

        Bitmap bmp = getAvatar(context, participant.getLookupKey());

        Bitmap bitmap = ImageUtil.getCircularBitmap(context, bmp);


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
                .setLargeIcon(bitmap)
                .setContentTitle("Message from " + participant.getName())
                .setContentText(message.getText())
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setWhen(message.getTimeStamp());

        return builder.build();
    }


    public static Bitmap getAvatar(Context context, String lookupKey){
        Uri uri = getDataUri(context, lookupKey);
        if (uri == null){
            return null;
        }
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] {ContactsContract.Data.DATA15},
                null,
                null,
                null
        );
        if (cursor == null){
            return null;
        }
        try{
            if (cursor.moveToFirst()){
                byte [] bytes = cursor.getBlob(0);
                InputStream inputStream = new ByteArrayInputStream(bytes);
                return BitmapFactory.decodeStream(inputStream);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static Uri getDataUri(Context context, String lookupKey){
        Cursor cursor = context.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey),
                new String[] {ContactsContract.Contacts.PHOTO_ID},
                null,
                null,
                null
        );
        if (cursor == null){
            return null;
        }
        try {
            if (cursor.moveToFirst()){
                long id = cursor.getLong(0);

                /**http://developer.android.com/reference/android/provider/ContactsContract.ContactsColumns.html#PHOTO_ID
                 * If PHOTO_ID is null, consult PHOTO_URI or PHOTO_THUMBNAIL_URI,
                 * which is a more generic mechanism for referencing the contact photo,
                 * especially for contacts returned by non-local directories (see ContactsContract.Directory).
                 */

                if (id == 0){
                    if (Build.VERSION.SDK_INT < 11){
                        return null;
                    }
                    return getPhotoThumbnailUri(context, lookupKey);
                }
                return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    //Available only for API level 11+
    public static Uri getPhotoThumbnailUri(Context context, String lookupKey){
        Cursor cursor = context.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey),
                new String[] {ContactsContract.Contacts.PHOTO_THUMBNAIL_URI},
                null,
                null,
                null
        );
        if (cursor == null){
            return null;
        }
        try{
            if (cursor.moveToFirst()){
                return Uri.parse(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
