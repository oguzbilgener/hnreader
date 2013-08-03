package com.oguzdev.hnclient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;


public class News 
{
	private String newsUrl;
	private ArrayList<NewsItem> newsList;
	
	public String fnLink = "";

    public static int TIMEOUT_SECONDS = 30;

    private boolean isExpired = false;
	
	public News()
	{
		newsUrl = Urls.trendingPage;
	}
	public News(String url)
	{
		newsUrl = url;
	}
	
	/*
	 * Try to iterate in horrible YCombinator HTML table structure and parse it.
	 */
	public void parse() throws IOException
	{
		long firsttime = System.currentTimeMillis();
		newsList = new ArrayList<NewsItem>();
		Document doc = Jsoup.connect(newsUrl).timeout(30*1000).get();
        // Check if the link is expired
        if(doc.html().equals("Unknown or expired link."))
        {
            // if true, stop parsing.
            isExpired = true;
            return;
        }
		long secondtime = System.currentTimeMillis();

		Elements tables = doc.getElementsByTag("table");
		// Our table is the third table in the document. if it does not exist, there's nothing to parse. exit.
		if(tables.size()<3)
			return;
		Element feedTable = tables.get(2);
		Elements tableRows = feedTable.getElementsByTag("tr");
		
		for(int r=0; r<tableRows.size(); r+=3)
		{
			// Page structure might be different than we expected.
			try {
				NewsItem item = new NewsItem();
				
				// Get first two rows in every three-row block
				Element row1 = tableRows.get(r);
				Element row2 = tableRows.get(r+1);
				
				if(row1.getElementsByTag("a").size()<1)
					// not a true list item.
					continue;
				
				// find table cells that we need
                Elements tds1 = row1.getElementsByTag("td");
                Elements tds2 = row2.getElementsByTag("td");
				Element td1 = tds1.get(1);
				Element td2 = tds1.get(2);
				Element td4 = tds2.get(1);
				
				Element aVote = null;
				// find the link of the item
				Element aLink = td2.getElementsByTag("a").first();
				// set the title and the url of the item
				item.setTitle(aLink.text());
				item.setExternalUrl(aLink.attr("href"));
				// sometimes there is an extra empty <td>.
				if(td1.html().equals(""))
				{
					// and this kind of items do not include vote link
					td1 = tds1.get(2);
					td2 = tds2.get(1);

                    // Get the itemId from the a element
                    item.setItemId(aLink.attr("href").substring(8));
				}
				else
				{
					// If there is no empty <td>, get the vote link
					aVote = td1.getElementsByTag("a").first();
					// Get the itemId from vote link this time
					if(aVote != null)
						item.setItemId(aVote.id().substring(3));
				}
				
				// get the domain name
				Elements comheads = td2.getElementsByClass("comhead");

                // TODO: test this:
                if(comheads.size()==0)
                    // Not found? try another one. Hacker news can be tricky.
                    comheads = tds1.get(2).getElementsByClass("comhead");

				// If exists, get the domain
				if(comheads.size()>0)
				{
					Element comhead = comheads.first();
					item.setDomain(comhead.text().replaceAll("\\s","").replaceAll("[\\(\\)]",""));
				}
				
				// Now get the points of the item
				Elements spans = td4.getElementsByTag("span");
				if(spans.size()>0)
				{
					// if there is a span element in the second row, it is probably the points count
					Element spanP = spans.first();
					// check it anyways and set it
					if(spanP.id().startsWith("score"))
						item.setPoints(spanP.text());
				}
				
				// Now get the owner of the item
				// Look for the a elements
				Elements aSecondRow = td4.getElementsByTag("a");
				// There should be at least 2 a elements.
				if(aSecondRow.size()>=2)
				{
					Element aUser = aSecondRow.get(0);
					Element aComments = aSecondRow.get(1);
					
					item.setUsername(aUser.text());
					item.setComments(aComments.text());
				}
				// Finally, add our item to the ArrayList
				newsList.add(item);
			}
			catch(Exception e)
			{
				//System.out.println("exception in parser loop");
				e.printStackTrace();
				continue;
			}
		}
		// Items are complete. Now look for an fnid to help with the next page
		Elements allAs = doc.getElementsByTag("a");
		for(Element aDoc : allAs)
		{
			if(aDoc.text().equals("More"))
			{
					fnLink = aDoc.attr("href");
					break;
			}
			
		}
		if(fnLink !=null && !fnLink.equals("") && !fnLink.startsWith("http"))
			if(fnLink.startsWith("/"))
				fnLink = Urls.homePage + fnLink.substring(1);
			else
				fnLink = Urls.homePage + fnLink;
		
		long thirdtime = System.currentTimeMillis();

		return;
	}
	
	public ArrayList<NewsItem> getNewsList()
	{
		return newsList;
	}
	
	public String getNext()
	{
		return fnLink;
	}

    public boolean isExpired()
    {
        return isExpired;
    }

}
