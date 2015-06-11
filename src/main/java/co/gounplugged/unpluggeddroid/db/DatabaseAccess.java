package co.gounplugged.unpluggeddroid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import co.gounplugged.unpluggeddroid.models.Conversation;
import co.gounplugged.unpluggeddroid.models.Mask;
import co.gounplugged.unpluggeddroid.models.Message;
import co.gounplugged.unpluggeddroid.models.Contact;

public class DatabaseAccess<T> {

    private static final String DATABASE_NAME = "unplugged.db";
    private static final int DATABASE_VERSION = 1;

    private Dao<T, Long> mDao;
    private DatabaseHelper mHelper;

    public DatabaseAccess(Context context, Class<T> type) {
        try {
            DatabaseHelper helper = getDatabaseHelperInstance(context);
            mDao = helper.getDao(type);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private DatabaseHelper getDatabaseHelperInstance(Context context) {
        if (mHelper == null) {
            mHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return mHelper;
    }

    public int create(T model) {
        try {
            return mDao.create(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int refresh(T model) {
        try {
            return mDao.refresh(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int update(T model) {
        try {
            return mDao.update(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int delete(T model) {
        try {
            return mDao.delete(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public T getById(long id) {
        try {
            QueryBuilder<T, Long> qb = mDao.queryBuilder();
            qb.where().eq("id", id);
            PreparedQuery<T> pq = qb.prepare();
            return mDao.queryForFirst(pq);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public T getFirstString(String columnName, String value) {
        try {
            QueryBuilder<T, Long> qb = mDao.queryBuilder();
            qb.where().eq(columnName, value);
            PreparedQuery<T> pq = qb.prepare();
            return mDao.queryForFirst(pq);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<T> getAll() {
        try {
            return mDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<T> getAllBut(T but) {
        List<T> all = getAll();
        if(but == null) return all;
        List<T> allBut = new ArrayList<T>();
        for(T t : all) {
            if(!t.equals(but)) allBut.add(t);
        }
        return allBut;
    }

    public List<T> getAllByColumnValue(String columnName, String value) {
        try {
            QueryBuilder<T, Long> qb = mDao.queryBuilder();
            qb.where().eq(columnName, value);
            PreparedQuery<T> pq = qb.prepare();
            return mDao.queryForEq(columnName, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int deleteAll() {
        try {
            DeleteBuilder<T, Long> db = mDao.deleteBuilder();
            return mDao.delete(db.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
