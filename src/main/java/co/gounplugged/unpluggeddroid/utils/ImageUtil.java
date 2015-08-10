package co.gounplugged.unpluggeddroid.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

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


    public static Drawable getDrawableFromUri(Context context, Uri uri) {
        Drawable drawable;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            drawable = Drawable.createFromStream(inputStream, uri.toString() );
        } catch (FileNotFoundException e) {
            drawable = context.getResources().getDrawable(R.drawable.avatar);
        }
        return drawable;
    }

    public static RoundedBitmapDrawable getCircularDrawable(Context context, Bitmap bitmap) {
        RoundedBitmapDrawable circularBitmapDrawable = //
                RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);

        circularBitmapDrawable.setCornerRadius(bitmap.getWidth());

        return circularBitmapDrawable;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

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

    public static Bitmap getRoundedContactBitmap(Context context, Contact contact) {
        Drawable drawable = getDrawableFromUri(context, contact.getImageUri());
        Bitmap bitmap = drawableToBitmap(drawable);
        RoundedBitmapDrawable roundedBitmapDrawable = getCircularDrawable(context, bitmap);
        return drawableToBitmap(roundedBitmapDrawable);

    }

}
