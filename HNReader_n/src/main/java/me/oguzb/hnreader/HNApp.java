package me.oguzb.hnreader;

import java.lang.reflect.Field;

import me.oguzb.hnreader.utils.Utils;

import android.app.Application;
import android.view.ViewConfiguration;

public class HNApp extends Application 
{
	@Override
	public void onCreate()
	{
		// A little hack to force to show overflow menu
		// on devices with physical menu button
		try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception ex) {
	        Utils.log.d("cannot force overflow menu");
	    }
	}
}
