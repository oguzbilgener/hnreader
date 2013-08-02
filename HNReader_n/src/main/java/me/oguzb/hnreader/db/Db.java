package me.oguzb.hnreader.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.oguzdev.hnclient.NewsItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import me.oguzb.hnreader.utils.Utils;

public class Db 
{
	private SQLiteDatabase db;
	private DbHelper dbh;
	
	// Copy news table names for ease of use
	public final static int NEWS_HOMEPAGE = DbHelper.V_NEWS_HOMEPAGE;
	public final static int NEWS_NEWEST = DbHelper.V_NEWS_NEWEST;
	public final static int NEWS_ASK = DbHelper.V_NEWS_ASK;
	
	public final static String SETTINGS_NEXT_PREFIX = "next_link_";
    public final static String SETTINGS_LASTUPTD_PREFIX = "last_updated_";
	
	public Db(Context context) throws SQLException
	{
		dbh = new DbHelper(context);
		db = dbh.getWritableDatabase();
	}
	
	public void open()
	{
		db = dbh.getWritableDatabase();
	}
	
	public void close()
	{
		try	{
			dbh.close();
		}catch(Exception e)	{
			e.printStackTrace();
			Utils.log.w("cannot close db "+e.toString());
		}
	}
	
	public boolean isOpen()
	{
		return db.isOpen();
	}
	
	public byte[] serializeObject(Object obj)
	{	
		try 
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			out = new ObjectOutputStream(bos);   
			out.writeObject(obj);
			out.close();
			bos.close();
			return bos.toByteArray();
		}
		catch(Exception e)
		{
			Utils.log.w(e.toString());
			e.printStackTrace();
			return null;
		}
	}
	
	public Object deSerializeObject(byte[] blob)
	{
		try
		{
			ByteArrayInputStream bis = new ByteArrayInputStream(blob);
			ObjectInput in = null;
			in = new ObjectInputStream(bis);
			bis.close();
			in.close();
		  	return in.readObject(); 
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public void insertNewsItem(NewsItem item, int type)
	{
		byte[] serializedItem = serializeObject(item);
		if(serializedItem == null)
		{
			Utils.log.w("could not serialize NewsItem");
			return;
		}
		String statement = "INSERT INTO "+DbHelper.T_NEWS+" ("+
				DbHelper.C_NEWS_TYPE+", "+ // 1
				DbHelper.C_NEWS_ID+", "+ // 2
				DbHelper.C_NEWS_URL+", "+ // 3
				DbHelper.C_NEWS_OBJECT+" "+ // 4
				") VALUES (?,?,?,?)";
		SQLiteStatement ss = db.compileStatement(statement);
		ss.bindLong(1, type);
		ss.bindString(2, item.getItemId());
		ss.bindString(3, item.getExternalUrl());
		ss.bindBlob(4, serializedItem);
		ss.execute();
		ss.close();			
	}
	
	public void clearNews(int type)
	{
		try
		{
			db.execSQL("DELETE FROM "+DbHelper.T_NEWS+" WHERE "+DbHelper.C_NEWS_TYPE+"="+type);
			Utils.log.d("[DB] Successfully cleared items with type: "+type);
		}
		catch(Exception e)
		{
			Utils.log.w("Could not clear items: "+e.toString());
			e.printStackTrace();
		}
	}
	
	public void insertNewsList(ArrayList<NewsItem> list, int type)
	{
		for(int i=0;i<list.size(); i++)
		{
			insertNewsItem(list.get(i), type);
		}
	}
	
	public NewsItem getNewsItemById(String id)
	{
		String columnList[] = new String[] {
			DbHelper.C_NEWS_ROW_ID, // 0
			DbHelper.C_NEWS_OBJECT // 1	
		};
		try
		{
			Cursor get = db.query(DbHelper.T_NEWS, columnList, DbHelper.C_NEWS_ID+" = ?", new String[]{id}, null, null, null,null);
			if(!get.moveToFirst())
				return null;
			
			Object dObject = deSerializeObject(get.getBlob(1));
			get.close();
			if(dObject!=null)
				return (NewsItem) dObject;
			else
				return null;
		}
		catch(Exception e)
		{
			Utils.log.w("[DB] Exception in getNewsItemById(String): "+e.toString());
			e.printStackTrace();
			return null;
		}
	}
	
	public ArrayList<NewsItem> getNewsList(Integer type)
	{
		ArrayList<NewsItem> list = new ArrayList<NewsItem>();
		String columnList[] = new String[] {
			DbHelper.C_NEWS_ROW_ID, // 0
			DbHelper.C_NEWS_OBJECT // 1	
		};
		try
		{
			Cursor get = db.query(DbHelper.T_NEWS, columnList, DbHelper.C_NEWS_TYPE+" = ?", new String[]{type.toString()}, null, null, null, null);
			if(!get.moveToFirst())
				return list;
			while(!get.isAfterLast())
			{
				Object dObject = deSerializeObject(get.getBlob(1));
				if(dObject!=null)
					list.add((NewsItem)dObject);
				get.moveToNext();
			}
			get.close();
		}
		catch(Exception e)
		{
			Utils.log.w("[DB] Exception in getNewsList(int): "+e.toString());
			e.printStackTrace();
		}
		return list;
	}
	
	public String getNextLink(int type)
	{
		String dataKey = SETTINGS_NEXT_PREFIX+type;
		Cursor get = db.query(DbHelper.T_SETTINGS, new String[]{ DbHelper.C_SETTINGS_VAL}, DbHelper.C_SETTINGS_KEY+" = ?", new String[]{dataKey}, null, null, null, null);
		if(get.moveToFirst())
            return get.getString(0);
		else
			return null;
	}
	
	public void setNextLink(int type, String nextLink)
	{
		String statement = "REPLACE INTO "+DbHelper.T_SETTINGS+" ("+
                DbHelper.C_SETTINGS_ID+", "+ // 1
				DbHelper.C_SETTINGS_KEY+", "+ // 2
				DbHelper.C_SETTINGS_VAL+" "+ // 3
				") VALUES (?,?,?)";
		SQLiteStatement ss = db.compileStatement(statement);
        ss.bindLong(1, type);
		ss.bindString(2, SETTINGS_NEXT_PREFIX+type);
		ss.bindString(3, nextLink);
		ss.execute();
		ss.close();	
	}

    public Integer getLastUpdated(int type)
    {
        String dataKey = SETTINGS_LASTUPTD_PREFIX+type;
        Cursor get = db.query(DbHelper.T_SETTINGS, new String[]{ DbHelper.C_SETTINGS_VAL}, DbHelper.C_SETTINGS_KEY+" = ?", new String[]{dataKey}, null, null, null, null);
        if(get.moveToFirst())
            return Integer.parseInt(get.getString(0));
        else
            return null;
    }

    public void setLastUpdated(int type, int lastUpdated)
    {
        String statement = "REPLACE INTO "+DbHelper.T_SETTINGS+" ("+
                DbHelper.C_SETTINGS_ID+", "+ // 1
                DbHelper.C_SETTINGS_KEY+", "+ // 2
                DbHelper.C_SETTINGS_VAL+" "+ // 3
                ") VALUES (?,?,?)";
        SQLiteStatement ss = db.compileStatement(statement);
        ss.bindLong(1, type+3);
        ss.bindString(2, SETTINGS_LASTUPTD_PREFIX+type);
        ss.bindLong(3, lastUpdated);
        ss.execute();
        ss.close();
    }
}
