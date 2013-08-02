package me.oguzb.hnreader.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.oguzdev.hnclient.NewsItem;
import com.oguzdev.hnclient.Urls;

import java.util.ArrayList;

public class Utils 
{
	public static final String tag = "hnreader";
	public static final int API_LEVEL = android.os.Build.VERSION.SDK_INT;
	
	public static class log
	{
		public static void w(String message)
		{
			Log.w(tag,message);
		}
		public static void d(String message)
		{
			Log.d(tag,message);
		}
		public static void e(String message)
		{
			Log.e(tag,message);
		}
		public static void v(String message)
		{
			Log.v(tag,message);
		}
		public static void i(String message)
		{
			Log.i(tag,message);
		}
	}
	public static void Toast(Context context,String text)
	{
		new QuickToast(context,text,false);
	}
    public static void Toast(Context context, String text, Boolean isLong)
    {
        new QuickToast(context,text,isLong);
    }
	public static class QuickToast 
	{
		Toast quick;
		int duration;
		public QuickToast(Context context,CharSequence message, Boolean isLong)
		{
			if(isLong)
				duration = Toast.LENGTH_LONG;
			else 
				duration = Toast.LENGTH_SHORT;

			quick = Toast.makeText(context, message, duration);
			quick.show();
		}
	}
	
	public static class Connection
	{
		public static boolean isSyncAvailable(Context context)
		{
			// soon
			return false;
		}
		public static boolean isNetworkAvailable(Context context)
		{
			try 
			{
				ConnectivityManager con=(ConnectivityManager)context.getSystemService(Activity.CONNECTIVITY_SERVICE);
				try 
				{
					return con.getActiveNetworkInfo().isConnectedOrConnecting();
				}
				catch(Exception e) {
					boolean wifi=con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
					boolean mobile=con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
					if(wifi || mobile) {
						return true;	        
					}
					return false;
				}
			}
			catch(Exception e) 
			{
				return false;
			}
		}
	}
	
	public static float getPixelsByDp(Context context,float dp)
	{
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

    public static void printList(ArrayList<NewsItem> list, String def)
    {
        log.i("------ "+def+" -----");
        for(int i=0;i<list.size();i++)
        {
            log.i(i+" "+list.get(i).getTitle()+" "+list.get(i).getExternalUrl());
        }
        log.i("--------------------");
    }

    public static boolean isExternalUrl(String url)
    {
        if(url.startsWith(Urls.homePageNoSlash))
            return false;
        return true;
    }

    public static int getUnixTimestamp()
    {
        return (int)(System.currentTimeMillis()/1000L);
    }

	/**
	 * A simple method to slice an ArrayList<NewsItem>
	 * @param firstList the list to slice
	 * @param start the index for the place to start slicing
	 * @param count the item count for the new list
	 * @return a new ArrayList<NewsItem>
	 */
	public static ArrayList<NewsItem> sliceNewsList(ArrayList<NewsItem> firstList, int start, int count)
	{
		if(start >= firstList.size() || start+count > firstList.size())
			return firstList;
		ArrayList<NewsItem> newList = new ArrayList<NewsItem>();
		for(int i=start; i<start+count; i++)
		{
			newList.add(firstList.get(i));
		}
		return newList;
	}
}
