package com.example.smartlock;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;


public class DatabaseUtil {
    public static void SetLastUser(SQLiteDatabase db, String email)
    {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS CONFIG(EMAIL VARCHAR);");
            Cursor c = db.rawQuery("SELECT * FROM CONFIG;", null);
            if (c.moveToFirst()) {
                db.execSQL("UPDATE CONFIG SET EMAIL='" + email +"';");
            } else {
                db.execSQL("INSERT INTO CONFIG VALUES('" + email+ "');");
            }
            c.close();
        }
        catch(SQLiteException exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
        catch(Exception exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
    }
    public static String GetLastUser(SQLiteDatabase db)
    {
        try{
            db.execSQL("CREATE TABLE IF NOT EXISTS CONFIG(EMAIL VARCHAR);");
            Cursor c = db.rawQuery("SELECT * FROM CONFIG;", null);
            if (c.moveToFirst()) {
                return c.getString(0);
            }
        }
        catch(SQLiteException exp)
        {
            Log.d("SmartLockService", exp.getMessage());
            return "";
        }
        catch(Exception exp)
        {
            Log.d("SmartLockService", exp.getMessage());
            return "";
        }
        return "";
    }

    public static void ClearTables(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IoTDataTable;");
        db.execSQL("CREATE TABLE IF NOT EXISTS IoTDataTable(temp REAL, tlimithigh REAL, tlimitlow REAL, heater INTEGER,cooler INTEGER, dalarm INTEGER, dbreach INTEGER);");

    }

    public static void UpdateToken(SQLiteDatabase db, String user, String token)
    {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS GToken(user VARCHAR, token VARCHAR,newtoken INT);");
            Cursor c = db.rawQuery("SELECT * FROM GToken where user ='" + user + "';", null);
            if (c.moveToFirst()) {
                db.execSQL("UPDATE GToken SET token='" + token + "', newtoken=1 WHERE user = '" + user + "';");
            } else {
                db.execSQL("INSERT INTO GToken VALUES('" + user + "','" + token + "',1);");
            }
            c.close();
        }
        catch(SQLiteException exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
        catch(Exception exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
    }

    public static String GetTokenFromUser(SQLiteDatabase db, String user)
    {
        String token = "";
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS GToken(user VARCHAR, token VARCHAR,newtoken INT);");

            Cursor c = db.rawQuery("SELECT * FROM GToken WHERE user='" + user + "';", null);
            if (c.moveToFirst()) {
                token = c.getString(1);
            }
        }
        catch(SQLiteException exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
        catch(Exception exp)
        {
            Log.d("SmartLockService", exp.getMessage());
        }
        return token;
    }

    public static String GetUserFromToken(SQLiteDatabase db,String token)
    {
        try {
            String user = "";
            Cursor c = db.rawQuery("SELECT * FROM GToken WHERE token='" + token + "';", null);
            if (c.moveToFirst()) {
                user = c.getString(0);
            }
            return user;
        }
        catch(SQLiteException exp)
        {
            Log.w("IoTDevice",exp.getMessage());
        }
        catch(Exception exp)
        {
            Log.w("IoTDevice",exp.getMessage());
        }
        return "";
    }

}
