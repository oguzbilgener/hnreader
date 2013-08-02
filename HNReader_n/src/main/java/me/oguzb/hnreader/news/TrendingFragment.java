package me.oguzb.hnreader.news;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.oguzdev.hnclient.*;

/**
 * TrendingFragment is just a NewsFragment with different main url.
 * Since NewsFragment does all the hard work, we don't have much to do here
 * It's enough if we just set the main url and fragment type.
 */
public class TrendingFragment extends NewsFragment
{
	public static int fragmentIndex = 0; 
	public TrendingFragment()
	{
		setMainUrl(Urls.homePage);
		setFragType(NewsFragment.FRAG_TYPE_HOME);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
	{
		View v = super.onCreateView(inflater, container, savedInstanceState);
		return v;
	}
}
