package me.oguzb.hnreader.news;

import com.oguzdev.hnclient.*;

public class AskFragment extends NewsFragment
{
	public static int fragmentIndex = 3;
	public AskFragment()
	{
		setMainUrl(Urls.askPage);
		setFragType(NewsFragment.FRAG_TYPE_ASK);
	}
}
