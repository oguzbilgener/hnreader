package me.oguzb.hnreader.news;

import com.oguzdev.hnclient.*;

public class NewestFragment extends NewsFragment 
{
	public static int fragmentIndex = 1;
	public NewestFragment()
	{
		setMainUrl(Urls.newPage);
		setFragType(NewsFragment.FRAG_TYPE_NEWEST);
	}
}
