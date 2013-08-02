package me.oguzb.hnreader.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.oguzdev.hnclient.NewsItem;

import java.util.List;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.utils.Utils;

public class NewsListAdapter extends ArrayAdapter<NewsItem> implements OnClickListener
{
	private final Context context;
	private final List<NewsItem> list;
	private LayoutInflater inflater;
	
	public NewsListAdapter(Context con, List<NewsItem> list) 
	{
		super(con, R.layout.news_item, list);
		this.context = con;
		this.list = list;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setNotifyOnChange(false);
	}
	
	public static class ViewHolder 
	{
		protected TextView titleText;
		protected TextView username;
		protected TextView domainText;
		protected TextView pointsText;
		protected TextView commentsText;
		
		protected LinearLayout linksRow;
		protected LinearLayout articleLink;
		protected LinearLayout commentsLink;
		
		protected long itemId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = null;
		
		int layoutId = R.layout.news_item;
		
		if(convertView == null)
		{
			view = inflater.inflate(layoutId, null);
			final ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.titleText = (TextView) view.findViewById(R.id.titleText);
			// username is not displayed at the moment
			viewHolder.username = new TextView(parent.getContext());
			viewHolder.domainText = (TextView) view.findViewById(R.id.domainText);
			viewHolder.pointsText = (TextView) view.findViewById(R.id.pointsText);
			viewHolder.commentsText = (TextView) view.findViewById(R.id.commentsText);
			
			viewHolder.linksRow = (LinearLayout) view.findViewById(R.id.linksRow);
			viewHolder.articleLink = (LinearLayout) view.findViewById(R.id.articleLink);
			viewHolder.commentsLink = (LinearLayout) view.findViewById(R.id.commentsLink);
			
			view.setTag(R.id.newsitem_object,viewHolder);
		}
		else
		{
			view = convertView;
		}
		final ViewHolder holder = (ViewHolder) view.getTag(R.id.newsitem_object);
		
		// Get our list item in this specific position
		NewsItem item = list.get(position);
		
		// Set tags to parent view and some other views
		// so that we can handle events like onclick easily
		view.setTag(R.id.newsitem_id, item.getItemId());
		view.setTag(R.id.newsitem_index, position);
		holder.commentsLink.setTag(R.id.newsitem_id, item.getItemId());
		holder.articleLink.setTag(R.id.newsitem_id, item.getItemId());
		holder.articleLink.setTag(R.id.newsitem_article_link, item.getExternalUrl());
		holder.articleLink.setTag(R.id.newsitem_article_title, item.getTitle());
		
		// Set values for list item's views
		holder.titleText.setText(item.getTitle());
		holder.username.setText(item.getUsername());
		holder.domainText.setText(item.getDomain());
		holder.pointsText.setText(item.getPoints());
		holder.commentsText.setText(item.getComments());
		
		try // Just in case
		{
			// Fix the height of the button holder
			RelativeLayout.LayoutParams lr = (RelativeLayout.LayoutParams) holder.linksRow.getLayoutParams();
			lr.height = (int) Utils.getPixelsByDp(getContext(),(float) context.getResources().getInteger(R.dimen.feed_item_max_height));
			holder.linksRow.setLayoutParams(lr);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		// Set onclick listeners
		holder.articleLink.setOnClickListener(this);
		holder.commentsLink.setOnClickListener(this);
		
		return view;
	}
	
	
	@Override
	public void onClick(View v) 
	{
		switch(v.getId())
		{
			case R.id.articleLink:
                // if the item has no external url, open the comments for it
                String link = getArticleLinkByView(v);
                if(Utils.isExternalUrl(link))
				    NewsFragment.openArticle(context, link, getArticleTitleByView(v), getItemIdByView(v));
                else
                    NewsFragment.openComments(context, getItemIdByView(v), getArticleTitleByView(v));
			break;
			case R.id.commentsLink:
                NewsFragment.openComments(context, getItemIdByView(v), getArticleTitleByView(v));
			break;
		}	
	}
	
	private static String getArticleTitleByView(View v)
	{
		try
		{
			return (String)(v.getTag(R.id.newsitem_article_title));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	private static String getArticleLinkByView(View v)
	{
		try
		{
			return (String)(v.getTag(R.id.newsitem_article_link));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	private static String getItemIdByView(View v)
	{
		try
		{
			return (String)(v.getTag(R.id.newsitem_id));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
