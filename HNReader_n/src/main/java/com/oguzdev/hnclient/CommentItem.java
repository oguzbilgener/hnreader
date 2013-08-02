package com.oguzdev.hnclient;

public class CommentItem
{
	private int depth;
	
	private String username;
	private String commentText;
	private String commentTime;
	private String commentLink;
	private String replyLink;

	private boolean op;
	private NewsItem opObject;
	
	public CommentItem()
	{
		depth = 0;
		username = commentText = commentTime = commentLink = "";
		op = false;
	}
	
	public String toString()
	{
		return "["+depth+"] "+username+" ("+commentTime+")\n"+commentText+"\n"+commentLink+"\n";
	}
	
	public int depth()
	{
		return depth;
	}
	public void setDepth(int depth)
	{
		this.depth = depth;
	}
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public String getCommentText()
	{
		return commentText;
	}
	public String getReplyLink()
	{
		return replyLink;
	}
	public void setCommentText(String text) 
	{
		if(text.endsWith(" reply"))
			text = text.substring(0,text.length()-6);
		commentText = text;
	}
	public String getCommentTime()
	{
		return commentTime;
	}
	public void setCommentTime(String time)
	{
		if(time.endsWith(" |"))
			time = time.substring(0,time.length()-2);
		commentTime = time;
	}
	public String getCommentLink() {
		return commentLink;
	}
	public void setCommentLink(String link)
	{
		if(!link.startsWith("http"))
			if(link.startsWith("/"))
				link = Urls.homePageNoSlash + link;
			else
				link = Urls.homePage + link;
		commentLink = link;
	}
	public void setReplyLink(String link)
	{
		if(!link.startsWith("http"))
			if(link.startsWith("/"))
				link = Urls.homePageNoSlash + link;
			else
				link = Urls.homePage + link;
		replyLink = link;
	}
	public boolean isOp()
	{
		return op;
	}
	public void setIsOp(boolean newOp)
	{
		op = newOp;
	}

	public NewsItem getOpObject()
	{
		return opObject;
	}
	public void setOpObject(NewsItem newObject)
	{
		opObject = newObject;
	}

}
