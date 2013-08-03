package me.oguzb.hnreader.reader;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ShareActionProvider;

import com.oguzdev.hnclient.Urls;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.utils.Utils;

public class ReaderWebView extends Activity
{
	private Context context;
	private ActionBar actionBar;
	private WebView browser;
	private ShareActionProvider mShareActionProvider;
	private BrowserControls browserControls;
	
	private SharedPreferences prefs;
	
	private String articleLink;
	private String finalUrl;
	private String articleTitle;
	private String articleId;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
        setContentView(R.layout.reader_web_layout);
        
        prefs = getSharedPreferences(getString(R.string.sharedprefs_main), Context.MODE_PRIVATE);

		setProgressBarVisibility(true);
        setProgress(1500);
		
		context = this;
		
		browserControls = new BrowserControls();
		
		actionBar = getActionBar();
		
		browser = (WebView) findViewById(R.id.reader_webview);
		
		Intent intent = getIntent();
		Bundle extras;
		try
		{
			if(intent == null)
				throw new Exception("intent is null");
				
			extras = intent.getExtras();
			if(extras == null)
				throw new Exception("extras are null");
				
			articleLink = extras.getString(
				getString(R.string.BUNDLE_OPEN_ARTICLE));
			if(articleLink == null)
				throw new Exception("article link is null");
				
			articleTitle = extras.getString(
				getString(R.string.BUNDLE_ARTICLE_TITLE));
			if(articleTitle == null)
				throw new Exception("article title is null");
			
			articleId = extras.getString(
				getString(R.string.BUNDLE_ARTICLE_ID));
			if(articleId == null)
				throw new Exception("article id is null");
				
			finalUrl = getFinalUrl(articleLink);	
		}
		catch(Exception e)
		{
			Utils.Toast(context,e.getMessage());
			Utils.log.w("[WEBVIEW] "+e.getMessage()+" // Exiting Activity");
			finish();
		}
		
		// Display title & url in the ActionBar
		actionBar.setTitle(articleTitle);
		actionBar.setSubtitle(articleLink);
		// Do not show icon/logo
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE|ActionBar.DISPLAY_HOME_AS_UP|ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.reader_web_action_view);
		
		// Adjust some webview settings
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setSupportZoom(true);
		browser.getSettings().setDisplayZoomControls(false);
		browser.getSettings().setBuiltInZoomControls(true);
		browser.getSettings().setLightTouchEnabled(false);
		browser.getSettings().setUseWideViewPort(true);
		browser.getSettings().setLoadWithOverviewMode(true);
		browser.getSettings().setDefaultZoom(ZoomDensity.FAR);
		
		browser.setWebViewClient(new BrowserClient());
		browser.setWebChromeClient(new ChromeClient());
		browser.setOnTouchListener(new BrowserTouchListener());
		
		// finally, tell the webview to load our article url
		browser.loadUrl(finalUrl);
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
	public void onDestroy()
	{
		browser.stopLoading();
		super.onDestroy();
	}
	 
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		 super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
	    super.onRestoreInstanceState(savedInstanceState);
	 }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.webview_menu, menu);
        
        
        // Set up browser controls
        browserControls.backItem = menu.findItem(R.id.browser_back);
        browserControls.forwardItem= menu.findItem(R.id.browser_forward);
        browserControls.reloadItem = menu.findItem(R.id.browser_reload);
        browserControls.mobilizerItem = menu.findItem(R.id.browser_reader_toggle);       
        // Set up ShareActionProvider
        browserControls.shareItem = menu.findItem(R.id.browser_share);
        mShareActionProvider = (ShareActionProvider) browserControls.shareItem.getActionProvider();
        // Show the last app used:
        //mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
        // Set a "send action" intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, articleLink);
        mShareActionProvider.setShareIntent(browserControls.createShareIntent());
        browserControls.update();
        
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{ 
			case android.R.id.home:
				finish();
			break;
			
			case R.id.browser_back:
				browserControls.back();
			break;
			
			case R.id.browser_forward:
				browserControls.forward();
			break;
			
			case R.id.browser_reload:
				browserControls.changeLoadingStatus();
			break;
			
			case R.id.browser_reader_toggle:
				browserControls.toggleMobilizer();
			break;
			
			case R.id.browser_open_in_browser:
				browserControls.openInBrowser();
			break;
			
			case R.id.browser_view_comments:
				openComments(articleId);
			break;
		
		}
		return true;
	}
	
	private class BrowserClient extends WebViewClient
	{
		@Override
		public void onReceivedError(WebView view, int errorCode, 
		String description, String failingUrl)
		{
			Utils.log.e("[WEBVIEW] error received: ["+errorCode+"]"+
				" "+description+" ("+failingUrl+")");
			Utils.Toast(context, "error received: "+description);
		}
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			super.onPageStarted(view, url, favicon);
			browserControls.loading = true;
			browserControls.update();
		}
		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			browserControls.loading = false;
			browserControls.update();
		}
	}
	
	private class ChromeClient extends WebChromeClient
	{
		@Override
		public void onProgressChanged(WebView view, int progress)
		{
			setProgress(progress*100);
			// If the download is complete, hide the action bar
			if(progress==100)
			{
				// We don't need this anymore
				//hideActionBar();
			}
		}
	}
	private class BrowserTouchListener implements View.OnTouchListener
	{
		private float downX, downY, upX, upY;
		public static final int CLICK_MAX_TIME = 100;
		public static final double MAX_DISTANCE = 7.0;
		@Override
		public boolean onTouch(View v, MotionEvent event) 
		{
			switch (event.getAction()) 
			{
				case MotionEvent.ACTION_DOWN: 
		            downX = event.getX();
		            downY = event.getY();
		        break;
		    	case MotionEvent.ACTION_UP:
		    		upX = event.getX();
		            upY = event.getY();

		            float deltaX = downX - upX;
		            float deltaY = downY - upY;
		            
		    	// Measure the touch duration
		    	// It will toggle the action bar only when the eventDuration is lower than CLICK_MAX_TIME
		    	// This way, swipes won't be count
		    	long eventDuration = event.getEventTime() - event.getDownTime();
		    	if(eventDuration < CLICK_MAX_TIME
		    			&& Math.abs(deltaX) < MAX_DISTANCE
		    			&& Math.abs(deltaY) < MAX_DISTANCE)
		    	{
		    		toggleActionBar();
		    		/*if(fullscreenAllowed)
		    			toggleFullscreen(!actionBar.isShowing());*/
		    		return false;
		    	}
		    }
			return false;
		}
	}
	
	/**
	 * This class puts Browser UI-related methods and variables together
	 */
	private class BrowserControls
	{
		public boolean loading;
		// Shortcut boolean to mobilizer
		// This setting will be stored in a database or keystore later
		public boolean mobilizerEnabled;
		
		public MenuItem backItem;
		public MenuItem forwardItem;
		public MenuItem reloadItem;
		public MenuItem mobilizerItem;
		public MenuItem shareItem;
		
		public BrowserControls()
		{
			loading = false;
			mobilizerEnabled = getMobilizerEnabled();
		}
		/**
		 * Updates the status of ActionBar elements
		 */
		public void update()
		{
			if(backItem == null
			   || reloadItem == null
			   || forwardItem == null
			   || mobilizerItem == null
			   || shareItem == null)
			return;
			if(loading)
			{
				showActionBar();
				// put "cancel" icon in ActionBar
				reloadItem.setIcon(R.drawable.ic_navigation_cancel);
				reloadItem.setTitle(R.string.browser_cancel);
			}
			else
			{
				hideActionBar();
				// put "reload" icon in ActionBar
				reloadItem.setIcon(R.drawable.ic_navigation_refresh);
				reloadItem.setTitle(R.string.browser_reload);
			}
			
			if(backAvailable())
			{
				backItem.setEnabled(true);
				backItem.setIcon(R.drawable.ic_navigation_back);
			}
			else
			{
				backItem.setEnabled(false);
				backItem.setIcon(R.drawable.ic_navigation_back_alt);
			}
			
			if(forwardAvailable())
			{
				forwardItem.setEnabled(true);
				forwardItem.setIcon(R.drawable.ic_navigation_forward);
			}
			else
			{
				forwardItem.setEnabled(false);
				forwardItem.setIcon(R.drawable.ic_navigation_forward_alt);
			}
			
			if(mobilizerEnabled)
			{
				mobilizerItem.setIcon(R.drawable.ic_threelines_alt);
				mobilizerItem.setTitle(R.string.browser_webmode);
			}
			else
			{
				mobilizerItem.setIcon(R.drawable.ic_threelines);
				mobilizerItem.setTitle(R.string.browser_readermode);
			}
		}
		
		/**
		 *  This method creates a special share Intent
		 *  for ShareActionProvider
		 */
		public Intent createShareIntent()
		{
			Intent intent = new Intent(Intent.ACTION_SEND);
	        intent.setType("text/plain");
	        intent.putExtra(Intent.EXTRA_TEXT, articleLink);
	        intent.putExtra(Intent.EXTRA_SUBJECT, articleTitle);
	        intent.putExtra(Intent.EXTRA_TITLE, articleTitle);
	        return intent;
		}
		
		public void share() 
	    {
	        startActivity(createShareIntent());
	    }
		
		/**
		 * Simply go back in local browser
		 * if backAvailable()
		 */
		public void back()
		{
			if(backAvailable())
				browser.goBack();
		}
		
		/**
		 * Simply go forward in local browser
		 * if forwardAvailable()
		 */
		public void forward()
		{
			if(forwardAvailable())
				browser.goForward();
		}
		
		/**
		 * Simply reload in local browser
		 */
		public void reload()
		{
			browser.reload();
		}
		
		/**
		 * Simply cancel in local browser
		 */
		public void cancel()
		{
			browser.stopLoading();
		}
		
		/**
		 * Change loading status of browser
		 * If already loading, cancel it
		 * If not loading, reload it
		 */
		public void changeLoadingStatus()
		{
			if(loading)
				cancel();
			else
				reload();
		}
		
		/**
		 * Change mobilizer status
		 * If mobilizer is already enabled, disable it
		 * If mobilizer is disabled, enabled it
		 */
		public void toggleMobilizer()
		{
			changeMobilizer(!mobilizerEnabled);
		}
		
		/**
		 * Get the new finalUrl and reload the page with the new finalUrl
		 * according to the status of enabled
		 * @param enabled : The status of mobilizer. Enabled or disabled
		 */
		public void changeMobilizer(boolean enabled)
		{
			setMobilizerEnabled(enabled);
			mobilizerEnabled = enabled;
			finalUrl = getFinalUrl(articleLink);			
			browser.loadUrl(finalUrl);
			browser.clearHistory();
		}
		
		public void openInBrowser()
		{
			Uri uri = Uri.parse(finalUrl);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
		
		/**
		 * @return if the browser can go back
		 */
		public boolean backAvailable()
		{
			return browser.canGoBack();
		}
		/**
		 * @return if the browser can go forward
		 */
		public boolean forwardAvailable()
		{
			return browser.canGoForward();
		}
	}

    private void openComments(String articleId)
    {
        try
        {
            Intent articleIntent = new Intent(context, me.oguzb.hnreader.comments.CommentsActivity.class);
            Bundle intentBundle = new Bundle();
            intentBundle.putString(context.getString(R.string.BUNDLE_ARTICLE_ID), articleId);
            intentBundle.putString(context.getString(R.string.BUNDLE_OPEN_COMMENTS), Urls.commentPage(articleId));
            articleIntent.putExtras(intentBundle);
            context.startActivity(articleIntent);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
	
	/**
	 * Creates a final url by using getFinalUrl(url) method if mobilizerEnabled == true
	 * @param url : The default url
	 * @return mobilized url or default url, according to the variable "mobilizerEnabled"
	 */
	public String getFinalUrl(String url)
	{
		if(browserControls.mobilizerEnabled)
			return getMobilizedUrl(articleLink);
		return articleLink;	
	}

	/**
	 * Creates a mobilized url according to the preferred url 
	 * @param url default url
	 * @return mobilized url with preferred provider
	 */
	public String getMobilizedUrl(String url)
	{
		// TODO: Multiple mobilizers to pick from
		// retrieve the current mobilizer url from a database or keystore
		String preferredMobilizerUrl = getString(R.string.instapaper_mobilizer_url);		
		return String.format(preferredMobilizerUrl,new String[]{url});
	}
	
	public void setMobilizerEnabled(Boolean state)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(getString(R.string.mobilizer_enabled_key), state);
		editor.commit();
	}
	
	public boolean getMobilizerEnabled()
	{
		return prefs.getBoolean(getString(R.string.mobilizer_enabled_key), false);		
	}
	
	/**
	 * Calls hideActionBar() if action bar is showing,
	 * Calls showActionBar() if not.
	 */
	public void toggleActionBar()
	{
		if(actionBar.isShowing())
			hideActionBar();
		else
			showActionBar();
	}
	/**
	 * Short hand to actionBar.show()
	 */
	public void showActionBar()
	{
		actionBar.show();
	}
	/**
	 * Short hand to actionBar.hide()
	 */
	public void hideActionBar()
	{
		actionBar.hide();
	}
	
	private void toggleFullscreen(boolean fullscreen)
	{
	    WindowManager.LayoutParams attrs = getWindow().getAttributes();
	    if (fullscreen)
	        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
	    else
	        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
	    getWindow().setAttributes(attrs);
	}
}