package me.oguzb.hnreader.comments;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.ShareActionProvider;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.news.MyHeaderTransformer;
import me.oguzb.hnreader.utils.ActivityCommunicator;
import me.oguzb.hnreader.utils.FragmentCommunicator;
import me.oguzb.hnreader.utils.Utils;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class CommentsActivity extends FragmentActivity implements ActivityCommunicator
{
    private ActionBar actionBar;
    private PullToRefreshAttacher mPullToRefreshAttacher;

	private String articleTitle;
    private String commentsUrl;

    public FragmentCommunicator fragmentCommunicator;
    private UrlSender urlSender;

    public static final int MSG_FRAGMENT_READY = 3900001;
    public static final int MSG_REFRESH_STARTED = 38223323;
    public static final int MSG_REFRESH_FINISHED = 38023323;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setContentView(R.layout.comments_activity_layout);

        // Set up the ActionBar with some options
        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Create Pull to Refresh Attacher with some custom options
        // Later the Fragments will need it.
        MyHeaderTransformer myTransformer = new MyHeaderTransformer();
        PullToRefreshAttacher.Options refreshOpts = new PullToRefreshAttacher.Options();
        refreshOpts.headerTransformer = myTransformer;
        mPullToRefreshAttacher = new PullToRefreshAttacher(this, refreshOpts);

        // Get the intent bundle, get the comments URL
        Intent incomingIntent = getIntent();
        Bundle extras = incomingIntent.getExtras();
        try
        {
            if(extras == null)
                throw new Exception("extras is null");

            commentsUrl = extras.getString(
                    getString(R.string.BUNDLE_OPEN_COMMENTS));
			articleTitle = extras.getString(
					getString(R.string.BUNDLE_ARTICLE_TITLE));

            if(commentsUrl == null)
                throw new Exception("no comment url specified");
        }
        catch(Exception e)
        {
            Utils.log.w("[CMT] "+e.getMessage());
            finish();
        }

        // Create a new CommentsFragment
        CommentsFragment commentsFragment = new CommentsFragment();
        // Set our FragmentCommunicator as the commentsFragment
        fragmentCommunicator = commentsFragment;
        urlSender =  commentsFragment;
        // Display the fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.comments_container, commentsFragment).commit();
    }

    @Override
    public void onStart()
    {
        super.onStart();
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
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.comments_menu, menu);

		MenuItem shareItem = menu.findItem(R.id.comments_share);
		ShareActionProvider mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
		// Set a "send action" intent
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, commentsUrl);
		mShareActionProvider.setShareIntent(createShareIntent());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
                // Home button
				//NavUtils.navigateUpFromSameTask(this);
				finish();
			return true;
				//finish();
        }
        return false;
    }

    public void initFragment()
    {
        Utils.log.d("[CMT] initFragment()");
        // first set the comments url, then refresh the comments list on startup
        urlSender.setCommentsUrl(commentsUrl);
        refreshComments(true);
    }

    public void refreshComments(boolean auto)
    {
        if(auto)
            fragmentCommunicator.passDataToFragment(CommentsFragment.CMD_REFRESH_IF_EMPTY);
        else
            fragmentCommunicator.passDataToFragment(CommentsFragment.CMD_REFRESH);
    }

    @Override
    public void sendMessage(int message)
    {
        switch(message)
        {
            case MSG_FRAGMENT_READY:
                Utils.log.d("[CMT] comments fragment is ready!");
                // Active fragment sends us a message when it's ready to start running tasks
                initFragment();
                break;
            case MSG_REFRESH_STARTED:
                // Notify PullToRefreshAttacher that the refresh has started
                showProgressBar(true);
                lockScreenOrientation();
                break;
            case MSG_REFRESH_FINISHED:
                Utils.log.d("[CMT] comments frag finished!");
                // Notify PullToRefreshAttacher that the refresh has finished
                showProgressBar(false);
                releaseScreenOrientation();
                break;
            default:
                Utils.log.w("unknown message to activity: "+message);
        }
    }

    @Override
    public void sendBundle(Bundle bundle)
    {

    }

    // Instances of CommentsFragment use this method to get a reference of PullToRefreshAttacher
    public PullToRefreshAttacher getPullToRefreshAttacher()
    {
        return mPullToRefreshAttacher;
    }

    // Manually update the ProgressBar status
    public void showProgressBar(boolean state)
    {
        mPullToRefreshAttacher.setEnabled(true);
        mPullToRefreshAttacher.setRefreshing(state);
    }

    public void lockScreenOrientation()
    {
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        display.getSize(size);

        int lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we
            // have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }
        setRequestedOrientation(lock);
    }

	/**
	 *  This method creates a special share Intent
	 *  for ShareActionProvider
	 */
	public Intent createShareIntent()
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, commentsUrl);
		intent.putExtra(Intent.EXTRA_SUBJECT, articleTitle);
		intent.putExtra(Intent.EXTRA_TITLE, articleTitle);
		return intent;
	}

    public void releaseScreenOrientation()
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static interface UrlSender
    {
        public void setCommentsUrl(String url);
    }
}
