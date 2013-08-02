package com.oguzdev.hnclient;

public class Urls
{
	// Hardcoded URLs of news.ycombinator.com
	public static String homePage = "https://news.ycombinator.com/";
	public static String homePageNoSlash = "https://news.ycombinator.com";
	public static String trendingPage = "https://news.ycombinator.com/news";
	public static String morePage = "https://news.ycombinator.com/x?fnid=";
	public static String newPage = "https://news.ycombinator.com/newest";
	public static String askPage = "https://news.ycombinator.com/ask";
	public static String jobsPage = "https://news.ycombinator.com/jobs";
	public static String submitPage = "https://news.ycombinator.com/submit";
	public static String votePage = "https://news.ycombinator.com/vote?for=";
    public static String itemPage = "https://news.ycombinator.com/item?id=";
    public static String news2Page = "https://news.ycombinator.com/news2";
	
	public static String fnlinkPage(String fnlink)
	{
		return homePage + fnlink;
	}
    public static String commentPage(String id)
    {
        return itemPage + id;
    }
	public static String voteFor(String id)
	{
		return votePage + id;
	}

}
