package me.oguzb.hnreader.news;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.oguzdev.hnclient.HNClient;
import com.oguzdev.hnclient.NewsItem;
import com.oguzdev.hnclient.Urls;

import org.jsoup.HttpStatusException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.db.Db;
import me.oguzb.hnreader.db.DbHelper;
import me.oguzb.hnreader.utils.ActivityCommunicator;
import me.oguzb.hnreader.utils.FragmentCommunicator;
import me.oguzb.hnreader.utils.Utils;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class NewsFragment extends Fragment 
		implements  PullToRefreshAttacher.OnRefreshListener, FragmentCommunicator,
		 View.OnClickListener, OnItemClickListener, OnItemLongClickListener, OnScrollListener
{
    private int frag_type;
	public Context context;

    protected String mainUrl;
    protected String nextUrl;

    protected Db db;
  
    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected ActivityCommunicator activityCommunicator;

	private ListWarningController warningController;

    protected RefreshNews refreshTask;
    protected Boolean refreshing;

    protected LoadMore loadMore;
    protected Boolean loadingMore;

    protected HNClient hn;

    protected ListView newsListView;
    protected ArrayList<NewsItem> newsList;
    protected NewsListAdapter newsAdapter;
    protected View emptyView;

    protected View loadingView;

    protected int currentFirstVisibleItem;
    protected int currentVisibleItemCount;

    protected boolean canLoadMore;
    protected Timer enableLoadMoreTimer;
    protected EnableTimerTask enableLoadMoreTask;
    protected static EnableHandler loadMoreEnabler;
    public static final int LOADMORE_TIMEOUT = 5000;
	public static final int LOADMORE_TIMEOUT_MAX = 99999999;
	public static final int LOADMORE_TIMEOUT_MIN = 1000;

    private boolean lastStateSet;
    
    public static final int CMD_REFRESH = 78123323;
    public static final int CMD_REFRESH_IF_EMPTY = 78123325;
	public static final int CMD_SCROLLTOP = 789123323;
    
    public static final int FRAG_TYPE_HOME = DbHelper.V_NEWS_HOMEPAGE;
    public static final int FRAG_TYPE_NEWEST = DbHelper.V_NEWS_NEWEST;
    public static final int FRAG_TYPE_ASK = DbHelper.V_NEWS_ASK;

    public static final int NEWS_LIST_STATE = R.string.NEWS_LIST_STATE;
    public static final int NEWS_PAGE_ITEMCOUNT = 30;
    public static final int NEWS_FNID_TIMEOUT = 650;

    // Constructor is always neccesary
    public NewsFragment()
    {	
    	mainUrl = Urls.homePage;
    	frag_type = FRAG_TYPE_HOME;
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	Utils.log.d("[FRAG] onCreate()");
    	
    	refreshing = false;
    	loadingMore = false;
        lastStateSet = false;

    	newsList = new ArrayList<NewsItem>();
    	newsAdapter = new NewsListAdapter(getActivity(), newsList);
        setRetainInstance(false);
        // Initialize EnableLoadMore task
        initEnableLoadMoreTask();
    	
    	db = new Db(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) 
    {
    	Utils.log.d("[FRAG] onCreateView()");
        View rootView = inflater.inflate(R.layout.newsfrag_layout, container, false);
		LayoutInflater activityInflater = ((LayoutInflater) getParent().getSystemService(Context.LAYOUT_INFLATER_SERVICE));

        // Create our main ListView
        newsListView = (ListView) rootView.findViewById(R.id.feed_list);
        
        // Configure the empty view
        emptyView = rootView.findViewById(R.id.newslist_empty_view);
        emptyView.setVisibility(View.GONE);
        
        // Set the footer load more indicator view
        loadingView = activityInflater.inflate(R.layout.news_list_loadmore, null, false);
        newsListView.addFooterView(loadingView, null, false);
        showLoadingView(false);
        
        // Set our Adapter to our ListView
        newsListView.setAdapter(newsAdapter);
        newsListView.setOnItemClickListener(this);
        newsListView.setOnScrollListener(this);
		//newsListView.setOverScrollMode(ListView.OVER_SCROLL_ALWAYS);

        // Get the PulltoRefreshAtacher from Parent Activity
        mPullToRefreshAttacher = getParent().getPullToRefreshAttacher();

        // Attach Pull to Refresh to the ListView
        mPullToRefreshAttacher.setRefreshableView(newsListView, this);

		// Initialize the warningController
		warningController = new ListWarningController(
				activityInflater,
				(ViewGroup)rootView.findViewById(R.id.feed_root),
				getParent(), this);
		// Hide the warningController View
		warningController.hideWarning(0);
        
        // Update newsList with previously stored items in the database
        new GetStoredNewsList().execute();
        
        return rootView;
    }

	@Override
	public void onAttach(Activity activity) 
	{
	   super.onAttach(activity);
	   Utils.log.d("[FRAG] onAttach()");
	   context = getActivity();
	   // Get an ActivityCommunicator that points our parent Activity
	   activityCommunicator =(ActivityCommunicator) getActivity();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		Utils.log.d("[FRAG] onActivityCreated()");
		
		sendReady();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		Utils.log.d("[FRAG] onResume()");
        // Do not let a LoadMore block to stay
        canLoadMore = true;
		db.open();
	}
	
	@Override
	public void onPause()
	{
		Utils.log.d("[FRAG] onPause()");
		//db.close();
        // Send newsListView's state to the parent Activity
        sendBundleToActivity(createListStateBundle(getListState()));
		super.onPause();
	}
	
	@Override
	public void onDestroy()
	{
		Utils.log.d("[FRAG] onDestroy()");
		refreshing = false;
		if(refreshTask!=null)
			refreshTask.cancel(true);
		super.onDestroy();
	}

    // Allow Activity to pass us it's PullToRefreshAttacher
    public void setPullToRefreshAttacher(PullToRefreshAttacher attacher) 
    {
        mPullToRefreshAttacher = attacher;
    }
    
    public String getMainUrl()
    {
    	return mainUrl;
    }
    protected void setMainUrl(String url)
    {
    	mainUrl = url;
    }
    public int getFragtype()
    {
    	return frag_type;
    }
    protected void setFragType(int type)
    {
    	frag_type = type;
    }  
    
    /** 
     * Tell parent Activity that we're ready to go!
     */
    public void sendReady()
    {
 		activityCommunicator.sendMessage(NewsActivity.MSG_FRAGMENT_READY);
    }

    // Allow parent Activity to send us commands via FragmentCommunicator interface
	 @Override
	 public void passDataToFragment(int command)
	 {
		 switch(command)
		 {
		 	case CMD_REFRESH:
		 		Utils.log.d("[FRAG] CMD_REFRESH received");
		 		refresh(false);
		 	break;
		 	
		 	case CMD_REFRESH_IF_EMPTY:
		 		if(newsList == null || newsList.isEmpty())
		 		{ Utils.log.d("[FRAG] CMD_REFRESH_IF_EMPTY received"); refresh(true); }
		 	break;
		 	
		 	case CMD_SCROLLTOP:
		 		Utils.log.d("[FRAG] CMD_SCROLLTOP received");
				// Scroll to top in the newsListView
				Utils.Toast(getActivity(), "scroll to top");
				newsListView.smoothScrollToPosition(0);
		 	break;
		 }
	 }

	// When user swipes down to refresh, this method is called
    @Override
    public void onRefreshStarted(View view) 
    {
    	Utils.log.d("[FRAG] onRefreshStarted()");
    	refresh(false);      
    }
    
    /**
     * Starts a new RefreshNews task, if there isn't already one.
     */
    private void refresh(boolean auto)
    {
		// Hide the refresh warning
		warningController.hideWarning();

    	if(refreshing==true)
    		return;
    	Utils.log.d("[FRAG] refresh()");

    	// the task might not be ready to start again
    	if(refreshTask!=null && !refreshTask.isCancelled())
			refreshTask.cancel(true);
    	
    	refreshTask = new RefreshNews();
    	refreshTask.execute();
    }
    
    /**
     * This method is used to cancel RefreshNews Task prematurely
     */
    private void cancelRefresh()
    {
    	if(refreshTask!=null)
			refreshTask.cancel(true);
    	
    	// Notify the parent activity that the refresh has finished
        // activityCommunicator.sendMessage(NewsActivity.MSG_REFRESH_FINISHED);
    }

    /**
     * Communicates with HNClient asynchronously to reload the list. 
     * Only one instance of this class should be used.
     */
    private class RefreshNews extends AsyncTask<Void, Void, Integer>
    {
    	private static final int SUCCESS = 1,
    			UNKNOWN_ERROR = 2, NO_CONNECTION = 3, CANCELLED = 4;
    	
    	@Override
    	protected void onPreExecute()
    	{
            // Notify the parent activity that the refresh has started
    		activityCommunicator.sendMessage(NewsActivity.MSG_REFRESH_STARTED);
    		Utils.log.d("[FRAG] starting refresh (RefreshNews)");
    		refreshing = true;
    	}
        @Override
        protected Integer doInBackground(Void... params) 
        {
            try {
            	if(!Utils.Connection.isNetworkAvailable(getActivity()))
            		// No Internet? return with error
            		return NO_CONNECTION;
            	
            	if(hn == null)
            		hn = new HNClient();
            	ArrayList<NewsItem> downloaded = hn.getNewsPage(mainUrl);
				Utils.log.i("[FRAG] refresh downloaded size: "+downloaded.size());
            	if(downloaded != null)
            	{
            		if(this.isCancelled())
            			return CANCELLED;
            		newsList.clear();
                    // Add newly downloaded items to the newsList
            		newsList.addAll(downloaded);
                    //Utils.printList(newsList, "newsList after refresh()");
                    nextUrl = hn.getNextLink();
            		Utils.log.d("[FRAG] RefreshNews next url: "+nextUrl);
            		return SUCCESS;
            	}
            	else
            	{
            		Utils.log.w("[FRAG] downloaded news list is null");
            		return UNKNOWN_ERROR;
            	}          		
            	
            } 
            catch(HttpStatusException he)
            {
            	// TODO: show a nice error
            	Utils.log.e("Request to "+he.getUrl()+" returned with HTTP "+he.getStatusCode());
            	return UNKNOWN_ERROR;
            }
            catch(UnknownHostException uh)
            {
                Utils.log.e("Could not resolve host of "+mainUrl+". Device is probably not connected to the Internet");
                return NO_CONNECTION;
            }
            catch (Exception e) {
            	Utils.log.w("Exception in refresh doInBackground: "+e.toString());
                e.printStackTrace();
                // Error? Notify the parent activity that the refresh has finished anyways
                return UNKNOWN_ERROR;
            }     
        }
        
        @Override
        protected void onCancelled()
        {
        	refreshing = false;
        	Utils.log.d("[FRAG] onCancelled()");
            // Notify the parent activity that the refresh has finished anyways
            activityCommunicator.sendMessage(NewsActivity.MSG_REFRESH_FINISHED);
        }

        @Override
        protected void onPostExecute(Integer result) 
        {            
            refreshing = false;
            Utils.log.d("[FRAG] finishing refresh (RefreshNews) ["+result+"]");
            switch(result)
            {
            	case SUCCESS:
            		// Update the Adapter
            		newsAdapter.notifyDataSetChanged();
            		// Update the Database with the just-downloaded NewsItems.
            		// Here, "true" means it will delete the old ones first.
            		new StoreNewsList(newsList, db, nextUrl, frag_type, true).execute();
					// Enable the LoadMore Task
					initEnableLoadMoreTask();
            	break;
            	case CANCELLED:
            		Utils.log.d("[FRAG] RefreshNews task ended prematurely");
            	break;
            	case NO_CONNECTION:
            		Utils.Toast(getActivity(), getString(R.string.newslist_error_no_internet));
            	break;
            	case UNKNOWN_ERROR:
            		Utils.Toast(getActivity(), getString(R.string.newslist_error_unknown));
            	break;
            } 
            // Notify the parent activity that the refresh has finished
            activityCommunicator.sendMessage(NewsActivity.MSG_REFRESH_FINISHED);

             // Show the empty View if the newsList is still empty
            showEmptyViewIfNeeded();
        }
    }
    
    /**
     * Starts a new RefreshNews task, if there isn't already one.
     */
    private void loadMore()
    {    	
    	if(loadingMore==true || !canLoadMore)
    		return;

    	Utils.log.d("[FRAG] loadMore()");

    	// the task might not be ready to start again
    	if(loadMore!=null && !loadMore.isCancelled())
			loadMore.cancel(true);
    	
    	loadMore = new LoadMore();
    	loadMore.execute();
    }
    
    /**
     * Communicates with HNClient asynchronously to load more items to the bottom of the list. 
     * Only one instance of this class should be used.
     */
    private class LoadMore extends AsyncTask<Void, Void, Integer>
    {
    	private static final int SUCCESS = 1,
    			UNKNOWN_ERROR = 2, NO_CONNECTION = 3, CANCELLED = 4,
    			NEXT_URL_NULL = 5, NEXT_URL_EXPIRED = 6, DOWNLOADED_LIST_EMPTY = 7;

        private ArrayList<NewsItem> downloaded;
    	
    	@Override
    	protected void onPreExecute()
    	{
            // Notify the parent activity that the refresh has started
    		activityCommunicator.sendMessage(NewsActivity.MSG_LOADMORE_STARTED);
    		Utils.log.v("[FRAG] starting LoadMore");
    		loadingMore = true;
    		showLoadingView(true);
    	}
        @Override
        protected Integer doInBackground(Void... params) 
        {
            try
			{
            	if(!Utils.Connection.isNetworkAvailable(getActivity()))
            		// No Internet? return with error
            		return NO_CONNECTION;
            	
            	if(hn == null)
            		hn = new HNClient();

				// the current nextUrl might be null or empty. In this case, we can't operate.
            	if(nextUrl == null || nextUrl.equals(""))
            		return NEXT_URL_NULL;
            	
            	downloaded = hn.getNewsPage(nextUrl);

                // The nextUrl might be expired. Check it.
                if(hn.isExpired())
                    return NEXT_URL_EXPIRED;

            	if(downloaded != null)
            	{
            		if(this.isCancelled())
            			return CANCELLED;

					// Is the downloaded list empty?
                    if(downloaded.isEmpty())
                        return DOWNLOADED_LIST_EMPTY;

            		newsList.addAll(downloaded);
            		nextUrl = hn.getNextLink();

            		Utils.log.d("[FRAG] ("+downloaded.size()+") next url: "+nextUrl);
            		return SUCCESS;
            	}
            	else
            	{
            		Utils.log.w("[FRAG] downloaded news list is null");
            		return UNKNOWN_ERROR;
            	}          		
            	
            } 
            catch(HttpStatusException he)
            {
            	// TODO: show a nicer error
            	Utils.log.e("Request to "+he.getUrl()+" returned with HTTP "+he.getStatusCode());
            	return UNKNOWN_ERROR;
            }
            catch (Exception e) {
            	Utils.log.w("Exception in refresh doInBackground: "+e.toString());
                e.printStackTrace();
                // Error? Notify the parent activity that the refresh has finished anyways
                return UNKNOWN_ERROR;
            }
        }

        @Override
        protected void onCancelled()
        {
        	loadingMore = false;
        	// Hide "loading" view
        	showLoadingView(false);
            getParent().releaseScreenOrientation();
        }

        @Override
        protected void onPostExecute(Integer result) 
        {            
        	loadingMore = false;
        	// Hide "loading" view
        	showLoadingView(false);
            Utils.log.d("[FRAG] finishing LoadMore ["+result+"]");
            switch(result)
            {
            	case SUCCESS:
            		// Update the Adapter
            		newsAdapter.notifyDataSetChanged();
            		// Update the Database with the just-downloaded NewsItems.
                    new StoreNewsList(downloaded, db, nextUrl, frag_type, false).execute();
            	break;
            	case CANCELLED:
            		Utils.log.d("[FRAG] LoadMore task ended prematurely");
            	break;
            	case NO_CONNECTION:
                    // No connection? Start a new Timeout to wait for the next try
                    initEnableLoadMoreTask();
                    startLoadMoreEnabledTimeout();
                    // Show a toast to user
            		Utils.Toast(getActivity(), getString(R.string.newslist_error_no_internet));
            	break;
                case DOWNLOADED_LIST_EMPTY:
                    Utils.log.w("[FRAG] LoadMore: downloaded list is empty");
					// Practically disable LoadMore
                    initEnableLoadMoreTask();
                    startLoadMoreEnabledTimeout(LOADMORE_TIMEOUT_MAX);
					// Show a custom Warning View
					warningController.showWarning();
                break;
            	case NEXT_URL_NULL:
            		Utils.log.w("[FRAG] LoadMore: next url is null");
					// Practically disable LoadMore
					initEnableLoadMoreTask();
					startLoadMoreEnabledTimeout(LOADMORE_TIMEOUT_MAX);
					// Show a custom Warning View
					warningController.showWarning();
            	break;
                case NEXT_URL_EXPIRED:
                    Utils.log.w("[FRAG] LoadMore: next url is expired");
					// Practically disable LoadMore
					initEnableLoadMoreTask();
					startLoadMoreEnabledTimeout(LOADMORE_TIMEOUT_MAX);
					// Show a custom Warning View
					warningController.showWarning();
                break;
            	case UNKNOWN_ERROR:
            		Utils.Toast(getActivity(), getString(R.string.newslist_error_unknown));
                    // Start a new Timeout to wait for the next try anyways
                    initEnableLoadMoreTask();
                    startLoadMoreEnabledTimeout();
            	break;
            } 
            // Notify the parent activity that the refresh has finished
            activityCommunicator.sendMessage(NewsActivity.MSG_LOADMORE_FINISHED);

            // Show the empty View if the newsList is still empty
            showEmptyViewIfNeeded();
        }
    }

    /**
     * Starts an AsyncTask to get the stored news items from the SQLite Database, 
     * then adds those items into the main newsList
     */
    private class GetStoredNewsList extends AsyncTask<Void, Void, Integer>
    {
    	private static final int SUCCESS = 1, UNKNOWN_ERROR = 2,
    			MAIN_LIST_NOT_EMPTY = 3, STORED_LIST_EMPTY = 4;
    	
        @Override
        protected Integer doInBackground(Void... params) 
        {
            if(!db.isOpen())
                db.open();
        	long ftime = System.currentTimeMillis();
            ArrayList<NewsItem> stored = db.getNewsList(frag_type);
			Utils.log.i("[FRAG] stored size: "+stored.size());
            Utils.log.d("[FRAG] ("+frag_type+") "+(System.currentTimeMillis()-ftime)+" millis to get from db");
            if(stored != null)
            {
            	// Retrieve the next url for this fragment, from database
                if(!db.isOpen())
                    db.open();
            	String nextUrlFromDb = db.getNextLink(frag_type);
            	if(nextUrlFromDb != null)
            		nextUrl = nextUrlFromDb;
            	else
            		Utils.log.d("[FRAG] ("+frag_type+") LoadMore: next url from db is null");
                Utils.log.d("[FRAG] next url from db: "+nextUrl);

                // Get the "last updated" value from database
                Integer lastUpdated_fromDb = db.getLastUpdated(frag_type);
                // Has the timeout been reached?
                if(lastUpdated_fromDb != null)
                {
                    if(timeoutReached(lastUpdated_fromDb))
                    {
                        Utils.log.v("[FRAG] GetStored: there are more than 30 items from db");
                        if(frag_type == FRAG_TYPE_HOME)
                        {
                            if(stored.size() >= NEWS_PAGE_ITEMCOUNT)
                            {
                                // We can only restore the news from homepage
                                // Because the nextUrl for the first page is always "/next2"
                                Utils.log.v("[FRAG] GetStored: timeout reached. Delete old ones.");
                                // If timeout has been reached, the nextUrl might be already expired.
                                // Trim the list, only display the first page:
								stored = Utils.sliceNewsList(stored, 0, NEWS_PAGE_ITEMCOUNT);
                                // Set the next url to "/news2" page
                                nextUrl = Urls.news2Page;
                            }
                        }
                        else
                        {
                       		Utils.log.v("[FRAG] GetStored: other type of list");
							// If timeout has been reached, we cannot restore from other news pages.
                        	// Because the nextUrl for the first page is an expired "fnid"
                        	// Clear the stored list:
                        	stored = new ArrayList<NewsItem>();
                        }

                    }
                }
                else
                {
                    Utils.log.i("last upd from db is null");
                }
            	
            	Utils.log.d("[FRAG] stored size: "+stored.size());
            	if(stored.size() == 0)
            	{
            		Utils.log.d("[FRAG] GetStoredNewsList: stored list is empty.");
            		return STORED_LIST_EMPTY;
            	}
            	if(newsList.size() != 0)
            	{
            		Utils.log.d("[FRAG] GetStoredNewsList: There are already items in the list. It's too late.");
            		return MAIN_LIST_NOT_EMPTY;
            	}
                //Utils.printList(stored, "stored list from db");
            	newsList.addAll((Collection<NewsItem>) stored);
            	return SUCCESS;
            }
            else
            {
            	Utils.log.w("[FRAG] ("+frag_type+") stored news list returns null from db");
            	return UNKNOWN_ERROR;
            }          		   
        }
        
        @Override
        protected void onPostExecute(Integer result) 
        {
        	Utils.log.d("GetStoredNewsList result: "+result);
            switch(result)
            {
            	case SUCCESS:
            		// cancel refreshing the list
            		cancelRefresh();
            		// Update the Adapter
            		newsAdapter.notifyDataSetChanged();
            	break;
                // In all other cases, the main RefreshNews task won't be cancelled.
            	case UNKNOWN_ERROR:
            		Utils.Toast(getActivity(), getString(R.string.newslist_error_unknown));
            	break;
            }
        }
    }
    
    /**
     * Starts an AsyncTask to store the newsList in the SQLite Database, 
     */
    private static class StoreNewsList extends AsyncTask<Void, Void, Integer>
    {
    	private static final int SUCCESS = 1, UNKNOWN_ERROR = 2,
    			LIST_EMPTY = 3;

        private ArrayList<NewsItem> list;
    	private boolean replace;
        private Db db;
        private String nextUrl;
        private int frag_type;

        /**
         * Create a new StoreNewsList AsyncTask
         * @param list the list to store
         * @param db the db reference
         * @param nextUrl the next url to store
         * @param frag_type frag type
         * @param replace whether to clear the old items or not
         */
    	public StoreNewsList(ArrayList<NewsItem> list, Db db, String nextUrl, int frag_type, boolean replace)
    	{
    		this.replace = replace;
            this.list = list;
            this.db = db;
            this.nextUrl = nextUrl;
            this.frag_type = frag_type;
    	}
    	
        @Override
        protected Integer doInBackground(Void... params) 
        {
        	if(list.size()==0)
        		return LIST_EMPTY;
        	try
        	{
        		if(!db.isOpen())
        			db.open();

                // If this is a replace request, delete the old items first
        		if(replace)
                    db.clearNews(frag_type);

                // Then insert the newsList
                db.insertNewsList(list, frag_type);

                // Also update lastUpdated value
                db.setLastUpdated(frag_type, Utils.getUnixTimestamp());
        		
        		// Finally, update the next url in the db
        		db.setNextLink(frag_type, nextUrl);
                Utils.log.d("[FRAG] next url to be stored: "+nextUrl);
        		
        		return SUCCESS;
        	}
        	catch(Exception e)
        	{
        		Utils.log.w("[FRAG] could not store newsList: "+e.toString());
        		e.printStackTrace();
        		return UNKNOWN_ERROR;
        	}
        }
    }

    // Parent Activity uses this method to send the Fragment a Bundle, usually a savedInstanceState
    @Override
    public void sendBundle(Bundle bundle)
    {
        try
        {
            Utils.log.d("[FRAG] sendBundle()");
            if(!lastStateSet && bundle.containsKey(getString(NEWS_LIST_STATE)))
            {
                lastStateSet = true;
                setListState(bundle.getParcelable(getString(NEWS_LIST_STATE)));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Sets the state for the newsListView
     * @param state the state to be set
     */
    public void setListState(Parcelable state)
    {
        if(state != null)
            newsListView.onRestoreInstanceState(state);
    }

    /**
     * Easy access for newsListView.onSaveInstanceState()
     * @return Parcelable data of newsListView's instance state
     */
    public Parcelable getListState()
    {
        return newsListView.onSaveInstanceState();
    }

    /**
     * Creates a new Bundle with state Parcelable objct
     * @param state the Parcelable to be put into the bundle
     * @return a bundle with state object
     */
    public Bundle createListStateBundle(Parcelable state)
    {
        Utils.log.d("[FRAG] createListState()");
        Bundle b = new Bundle();
        b.putParcelable(getString(NEWS_LIST_STATE), state);
        return b;
    }

    /**
     * Simply sends the specified bundle to the Parent Activity
     * @param bundle object to be sent
     */
    public void sendBundleToActivity(Bundle bundle)
    {
        Utils.log.d("[FRAG] sendBundleToActivity()");
        getParent().sendBundle(bundle);
    }



    /**
     * Displays the emptyView if newsList is empty, \n
     * Hides the emptyVİew is newsList is not empty.
     */
    public void showEmptyViewIfNeeded()
    {
        if(newsList.isEmpty())
            emptyView.setVisibility(View.VISIBLE);
        else
            emptyView.setVisibility(View.GONE);
    }

    /**
     *
     * @param show Whether the loading view should be displayed or not
     */
    public void showLoadingView(boolean show)
    {
    	if(loadingView == null)
    		return;
    	if(show)
    		loadingView.setVisibility(View.VISIBLE);
    	else
    		loadingView.setVisibility(View.GONE);
    }
    
    /**
     * @return getActivity() casted to NewsActivity
     */
    protected NewsActivity getParent() 
    {
    	return (NewsActivity) getActivity();
    }

    /**
     * Starts a new ReaderWebView Activity with the parameters specified.
     * @param context an Activity Context
     * @param articleLink article link
     * @param articleTitle article title
     * @param articleId article id
     */
    public static void openArticle(Context context, String articleLink, String articleTitle, String articleId)
    {
        try
        {
            Intent articleIntent = new Intent(context, me.oguzb.hnreader.reader.ReaderWebView.class);
            Bundle intentBundle = new Bundle();
            intentBundle.putString(context.getString(R.string.BUNDLE_OPEN_ARTICLE), articleLink);
            intentBundle.putString(context.getString(R.string.BUNDLE_ARTICLE_TITLE), articleTitle);
            intentBundle.putString(context.getString(R.string.BUNDLE_ARTICLE_ID), articleId);
            articleIntent.putExtras(intentBundle);
            context.startActivity(articleIntent);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Starts a new CommentsActivity with the parameters specified.
     * @param context an Activity Context
     * @param articleId article id
     */
    public static void openComments(Context context, String articleId, String articleTitle)
    {
        try
        {
            Intent articleIntent = new Intent(context, me.oguzb.hnreader.comments.CommentsActivity.class);
            Bundle intentBundle = new Bundle();
            intentBundle.putString(context.getString(R.string.BUNDLE_ARTICLE_ID), articleId);
            intentBundle.putString(context.getString(R.string.BUNDLE_OPEN_COMMENTS), Urls.commentPage(articleId));
			intentBundle.putString(context.getString(R.string.BUNDLE_ARTICLE_TITLE), articleTitle);
            articleIntent.putExtras(intentBundle);
            context.startActivity(articleIntent);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

	@Override
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case ListWarningController.CLICKABLE_ID:
				// Click on list warning refresh button!
				Utils.log.d("[ACT] onCLick: ListWarning");
				warningController.hideWarning();
				// Scroll to top of the list
				newsListView.smoothScrollToPosition(0);
				// Delay LoadMore a little, so that it won't work again while scrolling
				initEnableLoadMoreTask();
				startLoadMoreEnabledTimeout(LOADMORE_TIMEOUT_MIN);
				// And finally, refresh it
				refresh(false);
				break;
		}
	}
    
    @Override
	public boolean onItemLongClick(AdapterView<?> av, View v, int position,long id) 
    {

		return false;
	}


	@Override
	public void onItemClick(AdapterView<?> av, View v, int position,long id) 
	{
        String link = newsList.get(position).getExternalUrl();
        if(Utils.isExternalUrl(link))
            openArticle(context, newsList.get(position).getExternalUrl(), newsList.get(position).getTitle(), newsList.get(position).getItemId());
        else
            openComments(context, newsList.get(position).getItemId(), newsList.get(position).getTitle());
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) 
	{
		this.currentFirstVisibleItem = firstVisibleItem;
	    this.currentVisibleItemCount = visibleItemCount;
	}


	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		if(newsList.size()>0 && currentFirstVisibleItem >= newsList.size()-(currentVisibleItemCount+2))
	    {
	    	loadMore();
	    	Utils.log.d("YEP "+currentFirstVisibleItem+"|"+(newsList.size()-(currentVisibleItemCount+2))+"|"+newsList.size());
	    }
	    else
	    	Utils.log.d("NOPE "+currentFirstVisibleItem+"|"+(newsList.size()-(currentVisibleItemCount+2))+"|"+newsList.size());
		
	}

    /**
     * A protection to prevent trying to start LoadMore task consecutively, when there is no Internet connection. \n
     * When enableLoadMoreTimer finishes, loadMoreEnabler (Handler) is called.
     */
    public class EnableTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            loadMoreEnabler.sendEmptyMessage(0);
        }
    }

    /**
     * When EnableTimerTask finishes, EnablerHandler indicates that the user can load more again.
     */
    public class EnableHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            canLoadMore = true;
            Utils.log.d("[FRAG] Released LoadMore block");
        }
    }

    /**
     * Initializes the EnableLoadMore Task, Timer and Handler in order to make them ready to execute.
     */
    public void initEnableLoadMoreTask()
    {
        Utils.log.d("[FRAG] initEnableLoadMoreTask()");
        canLoadMore = true;
		if(enableLoadMoreTimer!=null)
			enableLoadMoreTimer.cancel();
		if(enableLoadMoreTask!=null)
			enableLoadMoreTask.cancel();
        enableLoadMoreTask = new EnableTimerTask();
        enableLoadMoreTimer = new Timer();
        loadMoreEnabler = new EnableHandler();
    }

    /**
     * Schedules a new Timer to execute a new EnableTimerTask
     */
	public void startLoadMoreEnabledTimeout(int timeout)
	{
		canLoadMore = false;
		Utils.log.d("[FRAG] Set up LoadMore block");
		enableLoadMoreTimer.schedule(enableLoadMoreTask,timeout);
	}
    public void startLoadMoreEnabledTimeout()
    {
        startLoadMoreEnabledTimeout(LOADMORE_TIMEOUT);
    }

	/**
	 * Timeout check method for Hacker News fnid value
	 * @param lastUpdated
	 * @return
	 */
    public static boolean timeoutReached(int lastUpdated)
    {
        if(Utils.getUnixTimestamp() >= lastUpdated + NEWS_FNID_TIMEOUT)
            return true;
        return false;
    }

}
