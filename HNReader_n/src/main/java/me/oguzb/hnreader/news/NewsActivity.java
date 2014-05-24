package me.oguzb.hnreader.news;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.Window;
import android.widget.ArrayAdapter;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.utils.AboutDialog;
import me.oguzb.hnreader.utils.ActivityCommunicator;
import me.oguzb.hnreader.utils.FragmentCommunicator;
import me.oguzb.hnreader.utils.Utils;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;


public class NewsActivity extends FragmentActivity 
	implements ActionBar.OnNavigationListener, ActivityCommunicator
{
    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private PullToRefreshAttacher mPullToRefreshAttacher;

    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    
    public FragmentCommunicator fragmentCommunicator;


    
    public static final int MSG_FRAGMENT_READY = 7900001;
    public static final int MSG_REFRESH_STARTED = 78223323;
    public static final int MSG_REFRESH_FINISHED = 78023323;
    public static final int MSG_LOADMORE_STARTED = 78223324;   
    public static final int MSG_LOADMORE_FINISHED = 78023324;

    private Bundle dataToSave;
    private Bundle dataToSend;

    public static final int NEWS_LIST_STATE = R.string.NEWS_LIST_STATE;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);	
        // request progress bar on action bar
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.newsview_layout);
        // set progress bar as indeterminate
        setProgressBarIndeterminate(true);
        setProgressBarVisibility(false);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		actionBar.setIcon(getResources().getDrawable(R.drawable.feed_icon));

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        getResources().getStringArray(R.array.title_sections)),
                this);
        // Create Pull to Refresh Attacher with some custom options
        // Later the Fragments will need it.
        MyHeaderTransformer myTransformer = new MyHeaderTransformer();
        PullToRefreshAttacher.Options refreshOpts = new PullToRefreshAttacher.Options();
        refreshOpts.headerTransformer = myTransformer;
        //refreshOpts.headerInAnimation = R.anim.enter_top;
        mPullToRefreshAttacher = new PullToRefreshAttacher(this, refreshOpts);

        // Initialize bundles
        dataToSave = new Bundle();
        dataToSend = new Bundle();
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
            // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM))
        {
            Integer selectedNavItem = savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM);
            if(selectedNavItem != null)
                getActionBar().setSelectedNavigationItem(selectedNavItem);
        }
        dataToSend = extractBundleFromSavedState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) 
    {
        outState = includeBundleFromFragment(outState);
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getActionBar().getSelectedNavigationIndex());
        // THIS IS EVIL:
        // super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.news_view_menu, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{ 
            case R.id.action_settings:
                openSettings();
            break;

			case R.id.action_about:
				showAboutDialog();
			break;
		}
		return true;
	}
    
    @Override
    public boolean onNavigationItemSelected(int position, long id) 
    {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        Fragment fragment;
        switch(position)
        {
            case 0:
                fragment = new TrendingFragment();
            break;
            case 1:
            	fragment = new NewestFragment();
            break;
            case 2:
            	fragment = new AskFragment();
            break;
            default:
            	fragment = new NewsFragment();
        }
        // Set our Fragment Communicator every time the Spinner is changed
        // This way, we make sure our messages goes to the right Fragment
        this.fragmentCommunicator = (FragmentCommunicator) fragment;

		try
		{
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
        
        return true;
    }
    
    // Run 
    public void initFragment()
    {
        // refresh the news feed on startup
        refreshNews(true);
    }
    
    public void refreshNews(boolean auto)
    {
        try
        {
            if(auto)
                fragmentCommunicator.passDataToFragment(NewsFragment.CMD_REFRESH_IF_EMPTY);
            else
                fragmentCommunicator.passDataToFragment(NewsFragment.CMD_REFRESH);
        }
        catch(Exception e)
        {
            Utils.log.e("[ACT] Exception:  "+e.toString());
            e.printStackTrace();
        }
    }
    
    @Override
    public void sendMessage(int message)
    {
    	switch(message)
    	{
    		case MSG_FRAGMENT_READY:
    			// Active fragment sends us a message when it's ready to start running tasks
    			initFragment();
    		break;
    		case MSG_REFRESH_STARTED:
    			// Notify PullToRefreshAttacher that the refresh has started
    			showProgressBar(true);
                lockScreenOrientation();
    		break;
    		
    		case MSG_REFRESH_FINISHED:
    			// Notify PullToRefreshAttacher that the refresh has finished
    			showProgressBar(false);
                // Send newsListView's saved state to Fragment
                sendBundleToFragment(dataToSend);
                releaseScreenOrientation();
    		break;
    		
    		case MSG_LOADMORE_STARTED:
    		break;
    		
    		case MSG_LOADMORE_FINISHED:
    		break;
    		default:
    			Utils.log.w("[ACT] unknown message: "+message);
    	}
   }

    /**
     * When a child Fragment sends a bundle to this NewsActivity, this method is called
     * @param bundle the Bundle to be sent
     */
    @Override
    public void sendBundle(Bundle bundle)
    {
        try
        {
            // was this bundle the news list state?
            if(bundle.containsKey(getString(NEWS_LIST_STATE)))
            {
                dataToSave.putParcelable(getString(NEWS_LIST_STATE),
                        bundle.getParcelable(getString(NEWS_LIST_STATE)));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Bundle includeBundleFromFragment(Bundle outState)
    {
        if(dataToSave.containsKey(getString(NEWS_LIST_STATE)))
        {
            outState.putParcelable(getString(NEWS_LIST_STATE),dataToSave.getParcelable(getString(NEWS_LIST_STATE)));
        }

        return outState;
    }

    public Bundle extractBundleFromSavedState(Bundle savedInstanceState)
    {
        Bundle b = new Bundle();
        if(savedInstanceState.containsKey(getString(NEWS_LIST_STATE)))
            b.putParcelable(getString(NEWS_LIST_STATE), savedInstanceState.getParcelable(getString(NEWS_LIST_STATE)));
        return b;
    }

    public void sendBundleToFragment(Bundle dataToSend)
    {
        fragmentCommunicator.sendBundle(dataToSend);
    }

	/**
	 *	Instances of NewsFragment use this method to get a reference of PullToRefreshAttacher
	 */
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

	protected void openSettings()
    {
        Intent settingsIntent = new Intent(this, me.oguzb.hnreader.settings.SettingsActivity.class);
        startActivity(settingsIntent);
    }

	/**
	 * This method tries to temporarily block the user from changing the orientation
	 * while loading something.
	 */
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

    public void releaseScreenOrientation()
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

	protected void showAboutDialog()
	{
		new AboutDialog().show(getSupportFragmentManager(), AboutDialog.TAG);
	}

}
