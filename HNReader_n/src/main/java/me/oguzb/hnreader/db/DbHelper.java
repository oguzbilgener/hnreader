package me.oguzb.hnreader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper 
{
	// Database name
	public static final String DBNAME = "hnreader_db";
	// Database table names
	public static final String T_SETTINGS = "settings";
	public static final String T_NEWS = "news";
	
	// Database column names
		// for Settings table
	public static final String C_SETTINGS_ID = "_id";
	public static final String C_SETTINGS_KEY = "key";
	public static final String C_SETTINGS_VAL = "val";
		// for news table
	public static final String C_NEWS_ROW_ID = "_id";
	public static final String C_NEWS_ID = "nid";
	public static final String C_NEWS_TYPE = "news_type";
	public static final String C_NEWS_URL = "news_url";
	public static final String C_NEWS_OBJECT = "news_object";
	
	// Values for news types
	public static final int V_NEWS_HOMEPAGE = 0;
	public static final int V_NEWS_NEWEST = 1;
	public static final int V_NEWS_ASK = 2;
	
	public final static int DB_VER = 1;
	
	public DbHelper(Context context) 
	{
		super(context, DBNAME, null, DB_VER);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		// Create settings table
		db.execSQL("CREATE TABLE "+T_SETTINGS+"("+
				   C_SETTINGS_ID+" INTEGER PRIMARY KEY,"+
				   C_SETTINGS_KEY+" TEXT UNIQUE,"+
				   C_SETTINGS_VAL+" INTEGER "+
				   ")");
		
		// Create news table
		db.execSQL("CREATE TABLE "+T_NEWS+" ("+
					C_NEWS_ROW_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
					C_NEWS_TYPE+" INTEGER, "+
					C_NEWS_ID+" TEXT, "+
					C_NEWS_URL+" TEXT, "+
					C_NEWS_OBJECT+" BLOB"+
					")");

        /*
         * WARNING about settings table:
         * The first three items (0,1,2) belong to the next urls of each NewsFragment.
         * 4th, 5th and 6th items (3,4,5) belong to the last updated timestamps of each NewsFragment.
         */
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) 
	{
		
	}

}
