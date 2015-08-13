package co.gounplugged.unpluggeddroid.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.models.Contact;

public class ImageUtil {

    // When using network requests we will probably have to use the no-fade option
    // https://github.com/hdodenhof/CircleImageView
    public static void loadContactImage(Context context, Contact contact, ImageView imageView) {
        Picasso.with(context).load(contact.getImageUri()).placeholder(R.drawable.avatar).into(imageView);
    }



    public static RoundedBitmapDrawable getCircularDrawable(Context context, Bitmap bitmap) {
        RoundedBitmapDrawable circularBitmapDrawable =
                RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);

        circularBitmapDrawable.setCornerRadius(bitmap.getWidth());

        return circularBitmapDrawable;
    }

    public static Drawable bitmapToDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getCircularBitmap(Context context, Bitmap bitmap) {
        return drawableToBitmap(getCircularDrawable(context, bitmap));
    }



    public static Bitmap getContactAvatar(Context context, String lookupKey){
        Bitmap bitmap = getBitmap(context, lookupKey);
        if (bitmap == null)
            return null;
        return getCircularBitmap(context, bitmap);
    }




    private static Bitmap getBitmap(Context context, String lookupKey){
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

    private static Uri getDataUri(Context context, String lookupKey){
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
    private static Uri getPhotoThumbnailUri(Context context, String lookupKey){
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
                if (cursor.getString(0) != null)
                    return Uri.parse(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

}
