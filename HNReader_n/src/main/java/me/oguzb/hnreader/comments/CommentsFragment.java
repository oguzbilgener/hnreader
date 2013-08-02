package me.oguzb.hnreader.comments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.oguzdev.hnclient.CommentItem;
import com.oguzdev.hnclient.HNClient;
import com.oguzdev.hnclient.NewsItem;

import org.jsoup.HttpStatusException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.news.NewsFragment;
import me.oguzb.hnreader.utils.ActivityCommunicator;
import me.oguzb.hnreader.utils.FragmentCommunicator;
import me.oguzb.hnreader.utils.Utils;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class CommentsFragment extends Fragment
        implements PullToRefreshAttacher.OnRefreshListener, FragmentCommunicator,
        CommentsActivity.UrlSender, AdapterView.OnItemClickListener
{
    protected Context context;
    protected ListView commentsListView;
    protected View emptyView;

    protected String commentsUrl;

    protected PullToRefreshAttacher mPullToRefreshAttacher;
    protected ActivityCommunicator activityCommunicator;

    protected ArrayList<CommentItem> commentsList;
    protected CommentsListAdapter commentsAdapter;
	protected NewsItem originalPost;

    protected RefreshComments refreshTask;
    protected Boolean refreshing;

    protected HNClient hn;

    public static final int CMD_REFRESH = 38123323;
    public static final int CMD_REFRESH_IF_EMPTY = 38123325;
    public static final int CMD_LOADMORE = 38123324;

    // Constructor is always neccesary
    public CommentsFragment()
    {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Utils.log.d("[FRAG] onCreate()");

        refreshing = false;
        commentsList = new ArrayList<CommentItem>();
        commentsAdapter = new CommentsListAdapter(getActivity(), commentsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Utils.log.d("[FRAG] onCreateView()");
        View rootView = inflater.inflate(R.layout.commentsfrag_layout, container, false);

        // Create our main ListView
        commentsListView = (ListView) rootView.findViewById(R.id.comments_list);

        // Configurate the empty view
        emptyView = rootView.findViewById(R.id.comments_empty);
        emptyView.setVisibility(View.GONE);

        // Set our Adapter to the ListView
        commentsListView.setAdapter(commentsAdapter);
		// Set an OnItemClickListener to catch the item clicks
		commentsListView.setOnItemClickListener(this);

        // Get the PulltoRefreshAtacher from Parent Activity
        mPullToRefreshAttacher = getParent().getPullToRefreshAttacher();

        // Attach Pull to Refresh to the ListView
        mPullToRefreshAttacher.setRefreshableView(commentsListView, this);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        Utils.log.d("[FRAG] onAttach()");
        context = getActivity();
        // Get an ActivityCommunicator that points our parent Activity
        activityCommunicator =(ActivityCommunicator)context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Utils.log.d("[FRAG] comments  onActivityCreated()");

        sendReady();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
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

    /**
     * Tell parent Activity that we're ready to go!
     */
    public void sendReady()
    {
        activityCommunicator.sendMessage(CommentsActivity.MSG_FRAGMENT_READY);
    }

    // Allow parent Activity to set commentsUrl from the incoming Intent
    @Override
    public void setCommentsUrl(String url)
    {
        commentsUrl = url;
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
                if(commentsList == null || commentsList.isEmpty())
                { Utils.log.d("[FRAG] CMD_REFRESH_IF_EMPTY received"); refresh(true); }
                break;
        }
    }

    // When user swipes down to refresh, this method is called
    @Override
    public void onRefreshStarted(View view)
    {
        Utils.log.d("[FRAG] comments onRefreshStarted()");
        refresh(false);
    }

    /**
     * Starts a new RefreshComments task, if there isn't already one running.
     */
    private void refresh(boolean auto)
    {
        if(refreshing!=null && refreshing)
            return;
        Utils.log.d("[FRAG] comments refresh()");

        // the task might not be ready to start again
        if(refreshTask!=null && !refreshTask.isCancelled())
            refreshTask.cancel(true);

        refreshTask = new RefreshComments();
        refreshTask.execute();
    }

    /**
     * This method is used to cancel RefreshComments Task prematurely
     */
    private void cancelRefresh()
    {
        if(refreshTask!=null)
            refreshTask.cancel(true);

        // Notify the parent activity that the refresh has finished
        activityCommunicator.sendMessage(CommentsActivity.MSG_REFRESH_FINISHED);
    }

	private class RefreshComments extends AsyncTask<Void, Void, Integer>
    {
        private static final int SUCCESS = 1,
                UNKNOWN_ERROR = 2, NO_CONNECTION = 3, CANCELLED = 4,
				OP_NULL = 5;

        @Override
        protected void onPreExecute()
        {
            // Notify the parent activity that the refresh has started
            activityCommunicator.sendMessage(CommentsActivity.MSG_REFRESH_STARTED);
            Utils.log.d("[CFRAG] starting refresh (RefreshComments)");
            refreshing = true;
        }
        @Override
        protected Integer doInBackground(Void... params)
        {
            Utils.log.d("[CFRAG] Now loading: "+commentsUrl);
            try {
                if(!Utils.Connection.isNetworkAvailable(getActivity()))
                    // No Internet? return with error
                    return NO_CONNECTION;

                if(hn == null)
                    hn = new HNClient();
                ArrayList<CommentItem> downloaded = hn.getComments(commentsUrl);
                if(downloaded != null)
                {
                    if(this.isCancelled())
                        return CANCELLED;
                    commentsList.clear();
					// Get the originalPost and send it to the List
					originalPost = hn.getOriginalPostForComment();

					if(originalPost == null)
						return OP_NULL;

					CommentItem firstItem = new CommentItem();
					firstItem.setOpObject(originalPost);
					commentsList.add(firstItem);

                    commentsList.addAll((Collection<CommentItem>) downloaded);



                    return SUCCESS;
                }
                else
                {
                    Utils.log.w("downloaded comments list is null");
                    return UNKNOWN_ERROR;
                }

            }
            catch(HttpStatusException he)
            {
                // TODO: show a nice error
                Utils.log.e("[CFRAG] Request to "+he.getUrl()+" returned with HTTP "+he.getStatusCode());
                return UNKNOWN_ERROR;
            }
            catch(UnknownHostException uh)
            {
                Utils.log.e("[CFRAG]Could not resolve host of "+commentsUrl+". Device is probably not connected to the Internet");
                return NO_CONNECTION;
            }
            catch (Exception e) {
                Utils.log.w("[CFRAG] Exception in refresh doInBackground: "+e.toString());
                e.printStackTrace();
                // Error? Notify the parent activity that the refresh has finished anyways
                return UNKNOWN_ERROR;
            }
        }

        @Override
        protected void onCancelled()
        {
            refreshing = false;
			if(getParent()!=null)
				getParent().releaseScreenOrientation();
        }

        @Override
        protected void onPostExecute(Integer result)
        {
            refreshing = false;
            Utils.log.d("[CFRAG] finishing refresh (RefreshComments) ["+result+"]");
            switch(result)
            {
                case SUCCESS:
                    // Update the Adapter
                    commentsAdapter.notifyDataSetChanged();
                    break;
                case CANCELLED:
                    Utils.log.d("[CFRAG] RefreshComments task ended prematurely");
                    break;
                case NO_CONNECTION:
                    Utils.Toast(getActivity(), getString(R.string.commentslist_error_no_internet));
                    break;
                case UNKNOWN_ERROR:
				default:
                    Utils.Toast(getActivity(), getString(R.string.commentslist_error_unknown));
                    break;
            }
            // Notify the parent activity that the refresh has finished
            activityCommunicator.sendMessage(CommentsActivity.MSG_REFRESH_FINISHED);

            // Show the empty View if commentsList is still empty
            if(commentsList.isEmpty())
                emptyView.setVisibility(View.VISIBLE);
            else
                emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void sendBundle(Bundle bundle)
    {

    }

    /**
     * @return getActivity() casted to CommentsActivity
     */
    protected CommentsActivity getParent()
    {
        return (CommentsActivity) getActivity();
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(position == 0)
		{
			if(originalPost.hasAnExternalUrl())
			{
				NewsFragment.openArticle(getActivity(), originalPost.getExternalUrl(),
						originalPost.getTitle(), originalPost.getItemId());
			}
		}
	}

}
