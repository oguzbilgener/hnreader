package com.oguzdev.hnclient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class Comments 
{
	public static final int MAXIMUM_DEEPNESS = 50;
	
	private String pageUrl;
	private ArrayList<CommentItem> commentsList;
    private NewsItem originalPost;
	
	public Comments(String url)
	{
		pageUrl = url;
	}
	
	/*
	 * Try to iterate in YCombinator's horrible HTML table structure and parse it.
	 */
	public void parse() throws IOException
	{
		commentsList = new ArrayList<CommentItem>();
		Document doc = Jsoup.connect(pageUrl).get();

		try
		{
            // TODO: update here and make sure it is really working
			// Find our comments container
			Element container = doc.getElementsByTag("table").first().
									getElementsByTag("tr").get(3).
									getElementsByTag("td").first();
			// In this container, every <table> is a comment,
			// except the first one.
			
			Elements tables = container.getElementsByTag("table");

            // Get the original post and put it in a NewsItem
            try
            {
                originalPost = new NewsItem();
                Element oTable = tables.first();
                Elements rows = oTable.getElementsByTag("tr");

                if(rows.get(0).getElementsByTag("a").size()<1)
                    // not a true list item.
                    throw new Exception();

                // find table cells that we need
				// Sometimes there is an extra empty "td"
				Element td1, td2 = null;
				if(rows.get(0).getElementsByTag("td").get(0).html().equals(""))
				{
					td1 = rows.get(0).getElementsByTag("td").get(1);
					if(rows.get(0).getElementsByTag("td").size()>2)
						td2 = rows.get(0).getElementsByTag("td").get(2);
				}
				else
				{
					td1 = rows.get(0).getElementsByTag("td").get(0);
					td2 = rows.get(0).getElementsByTag("td").get(1);
				}
				Element td4 = rows.get(1).getElementsByTag("td").get(1);

                Element aVote = null;
				if(td2 != null)
				{
					// find the link of the item
					Element aLink = td2.getElementsByTag("a").first();
					// set the title and the url of the item
					originalPost.setTitle(aLink.text());
					originalPost.setExternalUrl(aLink.attr("href"));
				}

                 // if there is no empty <td>, get the vote link
                 aVote = td1.getElementsByTag("a").first();
                 // now set the item id
                 if(aVote != null && aVote.id().length()>3)
                 	originalPost.setItemId(aVote.id().substring(3));

				if(td2 != null)
				{
					// get the domain name
					Elements comheads = td2.getElementsByClass("comhead");
					Element comhead = null;
					// if exists, of course
					if(comheads.size()>0)
					{
						comhead = comheads.first();
						originalPost.setDomain(comhead.text().replaceAll("\\s","").replaceAll("[\\(\\)]",""));
					}
				}

                // Now get the points of the item
                Elements spans = td4.getElementsByTag("span");
                if(spans.size()>0)
                {
                    // if there is a span element in the second row, it is probably the points count
                    Element spanP = spans.first();
                    // check it anyways and set it
                    if(spanP.id().startsWith("score"))
                        originalPost.setPoints(spanP.text());
                }

                // Now get the owner and comments count of the item
                // Look for the a elements
                Elements aSecondRow = td4.getElementsByTag("a");
                // There should be at least 2 a elements.
                if(aSecondRow.size()>=2)
                {
                    Element aUser = aSecondRow.get(0);
                    Element aComments = aSecondRow.get(1);

                    originalPost.setUsername(aUser.text());
                    originalPost.setComments(aComments.text());
                }

				// Now remove all child elements from 'td4' then get the time
					for(Element child : td4.children())
						child.remove();
					originalPost.setTime(td4.text());

				// If there are 6 table rows, then there is a post body
				if(rows.size()==6)
				{
					// Get the post body
					Element td6 = rows.get(3).getElementsByTag("td").get(1);
					originalPost.setBodyText(td6.html());
				}
				// fix for  a weird post type:
				if(td2 == null)
				{
					originalPost.setTitle(rows.get(0).getElementsByTag("td").get(1).text());
					originalPost.setBodyText(rows.get(3).getElementsByTag("td").get(1).html());
				}
            }
            catch(Exception e)
            {
                // Cannot parse the original post
				e.printStackTrace();
                originalPost = null;
            }

			
			// Start from the second table, because the first one is the original post
			for(int e=1; e<tables.size(); e++)
			{
				// Another try/catch here, 
				// so that if there is an exception in one comment, we can move on 
				try
				{
					CommentItem comment = new CommentItem();
					
					Element ctable = tables.get(e);
					// The depth of the comment is the width of an invisible image
					// this sucks.
					comment.setDepth(Integer.parseInt(ctable.getElementsByTag("img").first().attr("width"))/40);
					
					Element chead = ctable.getElementsByClass("comhead").first();
					Elements aheads = chead.getElementsByTag("a");
					
					// check if there is a username
					if(aheads==null || aheads.size()==0)
						continue;
					
					// Set username from one of a elements
					comment.setUsername(aheads.get(0).text());
					
					// Set username from the other a element
					comment.setCommentLink(aheads.get(1).attr("href"));
					
					// Now remove a elements from chead then get the time
					for(Element child : chead.children())
						child.remove();
					
					// Set the comment time
					comment.setCommentTime(chead.text());
					
					// Find comment body from a span with class 'comment'
					Element bodyTextContainer = ctable.getElementsByClass("comment").first();
					// First, find clean up the "reply" link
					for(Element aInBody : bodyTextContainer.getElementsByTag("a"))
					{
						if(aInBody.text().equals("reply"))
						{
							// Save the reply link for this comment
							if(aInBody.attr("href") != null)
								comment.setReplyLink(aInBody.attr("href").toString());
							// Remove this "a" element
							aInBody.remove();
							break;
						}
					}
					comment.setCommentText(bodyTextContainer.html());
					
					// Finally, add our comment to the single depth comments list
					commentsList.add(comment);
				}
				catch(Exception ec)
				{
					System.out.println("e "+e);
					ec.printStackTrace();
					continue;
				}
			}
		}
		catch(Exception e)
		{
			// Parse error. the page is not exactly like we expected.
			e.printStackTrace();
			return;
		}
	}
	
	public ArrayList<CommentItem> getComments()
	{
		return commentsList;
	}

    public NewsItem getOriginalPost()
    {
        return originalPost;
    }

}
