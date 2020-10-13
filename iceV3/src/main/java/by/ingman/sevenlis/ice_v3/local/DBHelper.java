package by.ingman.sevenlis.ice_v3.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "iceDBv3";
    private static final int DATABASE_VERSION = 8;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock rLock = rwLock.readLock();
    private final Lock wLock = rwLock.writeLock();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static SQLiteDatabase getDatabaseReadable(Context context) {
        SQLiteDatabase db = null;
        DBHelper dbHelper = new DBHelper(context);
        try {
            db = dbHelper.getReadableDatabase();
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }
        return db;
    }

    public static SQLiteDatabase getDatabaseWritable(Context context) {
        SQLiteDatabase db = null;
        DBHelper dbHelper = new DBHelper(context);
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
        }
        return db;
    }

    public static void closeDatabase(SQLiteDatabase db) {
        if (db.inTransaction())
            db.endTransaction();
        if (db.isOpen())
            db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DBLocal.TABLE_ORDERS + " ("
                + "_id integer primary key autoincrement,"
                + "order_id text not null,"
                + "name_m text not null,"
                + "order_date integer not null,"
                + "is_advertising integer not null,"
                + "adv_type integer not null,"
                + "code_k text not null,"
                + "name_k text not null,"
                + "code_r text not null,"
                + "name_r text not null,"
                + "code_s text not null,"
                + "name_s text not null,"
                + "code_p text not null,"
                + "name_p text not null,"
                + "agreementId text,"
                + "order_type integer,"
                + "weight_p real not null,"
                + "price_p real not null,"
                + "num_in_pack_p real not null,"
                + "amount real not null,"
                + "amt_packs real not null,"
                + "weight real not null,"
                + "price real not null,"
                + "summa real not null,"
                + "comments text,"
                + "status integer not null,"
                + "processed integer not null,"
                + "sent integer not null,"
                + "date_unload integer not null" + ");");
        
        db.execSQL("create table " + DBLocal.TABLE_ANSWERS + " ("
                + "_id integer primary key autoincrement,"
                + "order_id text not null,"
                + "description text not null,"
                + "result integer,"
                + "date_unload integer not null" + ");");
        
        db.execSQL("create table " + DBLocal.TABLE_RESTS + " ("
                + "_id integer primary key autoincrement,"
                + "code_s text not null,"
                + "name_s text not null,"
                + "code_p text not null,"
                + "name_p text not null,"
                + "packs real not null,"
                + "amount real not null,"
                + "price real not null,"
                + "gross_weight real not null,"
                + "amt_in_pack integer not null,"
                + "search_uppercase text not null,"
                + "date_unload integer not null" + ");");
        
        db.execSQL("create table " + DBLocal.TABLE_DEBTS + " ("
                + "_id integer primary key autoincrement,"
                + "code_k text not null,"
                + "name_k text not null,"
                + "code_mk text not null,"
                + "rating text not null,"
                + "debt real not null,"
                + "overdue real not null,"
                + "search_uppercase text not null,"
                + "date_unload integer not null" + ");");
        
        db.execSQL("create table " + DBLocal.TABLE_CONTRAGENTS + " ("
                + "_id integer primary key autoincrement,"
                + "code_k text not null,"
                + "name_k text not null,"
                + "code_mk text not null,"
                + "name_mk text not null,"
                + "code_r text not null,"
                + "name_r text not null,"
                + "code_mr text not null,"
                + "name_mr text not null,"
                + "client_uppercase text,"
                + "point_uppercase text,"
                + "date_unload integer not null" + ");");
    
        db.execSQL("create table " + DBLocal.TABLE_LOCATION + " ("
                + "_id integer primary key autoincrement,"
                + "latitude real not null,"
                + "longitude real not null,"
                + "time integer not null,"
                + "time_update integer not null"
                + ");");

        db.execSQL("create table " + DBLocal.TABLE_AGREEMENTS + " ("
                + "_id integer primary key autoincrement,"
                + "code_k text not null,"
                + "name_k text not null,"
                + "id text not null,"
                + "name text not null"
                + ");");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor cursor;
        
        List<ContentValues> ordersDataArray = new ArrayList<>();
        cursor = db.query(DBLocal.TABLE_ORDERS, null, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            ContentValues orderData = new ContentValues();
            
            orderData.put("order_id", cursor.getString(cursor.getColumnIndex("order_id")));
            orderData.put("name_m", cursor.getString(cursor.getColumnIndex("name_m")));
            orderData.put("order_date", cursor.getLong(cursor.getColumnIndex("order_date")));
            orderData.put("is_advertising", cursor.getInt(cursor.getColumnIndex("is_advertising")));
            orderData.put("adv_type", cursor.getInt(cursor.getColumnIndex("adv_type")));
            orderData.put("code_k", cursor.getString(cursor.getColumnIndex("code_k")));
            orderData.put("name_k", cursor.getString(cursor.getColumnIndex("name_k")));
            orderData.put("code_r", cursor.getString(cursor.getColumnIndex("code_r")));
            orderData.put("name_r", cursor.getString(cursor.getColumnIndex("name_r")));
            orderData.put("code_s", cursor.getString(cursor.getColumnIndex("code_s")));
            orderData.put("name_s", cursor.getString(cursor.getColumnIndex("name_s")));
            orderData.put("code_p", cursor.getString(cursor.getColumnIndex("code_p")));
            orderData.put("name_p", cursor.getString(cursor.getColumnIndex("name_p")));
            orderData.put("weight_p", cursor.getDouble(cursor.getColumnIndex("weight_p")));
            orderData.put("price_p", cursor.getDouble(cursor.getColumnIndex("price_p")));
            orderData.put("num_in_pack_p", cursor.getDouble(cursor.getColumnIndex("num_in_pack_p")));
            orderData.put("amount", cursor.getDouble(cursor.getColumnIndex("amount")));
            orderData.put("amt_packs", cursor.getDouble(cursor.getColumnIndex("amt_packs")));
            orderData.put("weight", cursor.getDouble(cursor.getColumnIndex("weight")));
            orderData.put("price", cursor.getDouble(cursor.getColumnIndex("price")));
            orderData.put("summa", cursor.getDouble(cursor.getColumnIndex("summa")));
            orderData.put("comments", cursor.getString(cursor.getColumnIndex("comments")));
            orderData.put("status", cursor.getInt(cursor.getColumnIndex("status")));
            orderData.put("processed", cursor.getInt(cursor.getColumnIndex("processed")));
            orderData.put("sent", cursor.getInt(cursor.getColumnIndex("sent")));
            orderData.put("date_unload", cursor.getLong(cursor.getColumnIndex("date_unload")));
            if (oldVersion > 5)
                orderData.put("agreementId", cursor.getString(cursor.getColumnIndex("agreementId")));
            if (oldVersion > 7) {
                orderData.put("order_type", cursor.getInt(cursor.getColumnIndex("order_type")));
            } else {
                orderData.put("order_type", 1);
            }

            ordersDataArray.add(orderData);
        }
        cursor.close();
        
        List<ContentValues> answersDataArray = new ArrayList<>();
        cursor = db.query(DBLocal.TABLE_ANSWERS, null, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            ContentValues answerData = new ContentValues();
            answerData.put("order_id", cursor.getString(cursor.getColumnIndex("order_id")));
            answerData.put("description", cursor.getString(cursor.getColumnIndex("description")));
            answerData.put("result", cursor.getInt(cursor.getColumnIndex("result")));
            answerData.put("date_unload", cursor.getLong(cursor.getColumnIndex("date_unload")));
            
            answersDataArray.add(answerData);
        }
        cursor.close();
        
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_ANSWERS);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_RESTS);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_DEBTS);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_CONTRAGENTS);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_LOCATION);
        db.execSQL("DROP TABLE IF EXISTS " + DBLocal.TABLE_AGREEMENTS);
        onCreate(db);
        
        for (ContentValues cv : ordersDataArray) {
            db.insert(DBLocal.TABLE_ORDERS, null, cv);
        }
        
        for (ContentValues cv : answersDataArray) {
            db.insert(DBLocal.TABLE_ANSWERS, null, cv);
        }
    }
    
}
