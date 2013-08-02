package com.oguzdev.hnclient;

import java.io.IOException;
import java.util.ArrayList;

public class HNClient 
{
	private String nextLink;
    private NewsItem commentOriginalPost;
    private boolean isExpired = false;

	public HNClient()
	{
		
	}
	
	// Homepage Stuff
	public ArrayList<NewsItem> getNewsIndex() throws IOException
	{
		// Parse homepage
		News news = new News();
		news.parse();
		nextLink = news.getNext();
		return news.getNewsList();
	}
	public ArrayList<NewsItem> getNewsPage(String fnLink) throws IOException
	{
		// Parse some news page
		News news = new News(fnLink);
		news.parse();
        // The fnLink might be expired. Check it.
        if(news.isExpired())
        {
            isExpired = true;
            return null;
        }
		nextLink = news.getNext();
		return news.getNewsList();
	}
	public ArrayList<NewsItem> getNewest() throws IOException
	{
		// Parse newest
		News news = new News(Urls.newPage);
		news.parse();
		nextLink = news.getNext();
		return news.getNewsList();
	}
	public ArrayList<NewsItem> getAsk() throws IOException
	{
		// Parse ask hn
		News news = new News(Urls.askPage);
		news.parse();
		nextLink = news.getNext();
		return news.getNewsList();
	}
	
	public String getNextLink()
	{
		return nextLink;
	}
	
	// Comments Stuff
	public ArrayList<CommentItem> getComments(String itemLink) throws IOException
	{
		Comments comments = new Comments(itemLink);
		comments.parse();
        commentOriginalPost = comments.getOriginalPost();
		return comments.getComments();
	}
    public NewsItem getOriginalPostForComment()
    {
        return commentOriginalPost;
    }

    public boolean isExpired()
    {
        return isExpired;
    }

}
