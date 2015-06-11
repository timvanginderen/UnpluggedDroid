package co.gounplugged.unpluggeddroid.db;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Conversation;
import co.gounplugged.unpluggeddroid.models.Mask;
import co.gounplugged.unpluggeddroid.models.Message;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "unplugged.db";
    private static final int DATABASE_VERSION = 1;

    Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, "password");
        mContext = context;
    }

    @Override
    public void onCreate(net.sqlcipher.database.SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        createDatabases(sqLiteDatabase, connectionSource, mContext);
    }

    private void createDatabases(net.sqlcipher.database.SQLiteDatabase db, ConnectionSource connectionSource, Context context) {
        Class<?>[] columns = {Conversation.class, Message.class, Mask.class, Contact.class};
        try {
            for (Class<?> c : columns) {
                TableUtils.createTable(connectionSource, c);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), " - Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(net.sqlcipher.database.SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {
//            if (oldVersion < 2) {
//                db.execSQL("ALTER TABLE \'blabla\' ADD COLUMN \'blabla\' BIGINT DEFAULT 0");
//            }
    }
}