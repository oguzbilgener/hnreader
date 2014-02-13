package me.oguzb.hnreader.news;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.oguzb.hnreader.R;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.HeaderTransformer;

/**
 * Custom HeaderTransformer for ActionBarPullToRefresh library
 * 
 */
public class MyHeaderTransformer extends HeaderTransformer 
{
    private TextView mHeaderTextView;
    private ProgressBar mHeaderProgressBar;

    private CharSequence mPullRefreshLabel, mRefreshingLabel;

    private final Interpolator mInterpolator = new AccelerateInterpolator();

    @SuppressWarnings("deprecation")
	@Override
    public void onViewCreated(Activity activity, View headerView) 
    {
        // Get ProgressBar and TextView. Also set initial text on TextView
        mHeaderProgressBar = (ProgressBar) headerView.findViewById(R.id.ptr_progress);
        mHeaderTextView = (TextView) headerView.findViewById(R.id.ptr_text);

        // Labels to display
        mPullRefreshLabel = activity.getString(R.string.pull_to_refresh_pull_label);
        mRefreshingLabel = activity.getString(R.string.pull_to_refresh_refreshing_label);

        View contentView = headerView.findViewById(R.id.ptr_content);
        if (contentView != null) {
            contentView.getLayoutParams().height = getActionBarSize(activity);
            contentView.requestLayout();
        }

        Drawable abBg = getActionBarBackground(activity);
        if (abBg != null) {
            // If we do not have a opaque background we just display a solid solid behind it
            if (abBg.getOpacity() != PixelFormat.OPAQUE) {
                View view = headerView.findViewById(R.id.ptr_text_opaque_bg);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
            mHeaderTextView.setBackgroundDrawable(abBg);
            
            // Set some color to the header text view
            mHeaderTextView.setTextColor(activity.getResources().getColor(R.color.swipe_refresh_text));
        }

        // Call onReset to make sure that the View is consistent
        onReset();
    }

    @Override
    public void onReset() {
        // Reset Progress Bar
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.GONE);
            mHeaderProgressBar.setProgress(0);
            mHeaderProgressBar.setIndeterminate(false);
        }

        // Reset Text View
        if (mHeaderTextView != null) {
            mHeaderTextView.setVisibility(View.VISIBLE);
            mHeaderTextView.setText(mPullRefreshLabel);
            mHeaderTextView.clearAnimation();
        }
    }

    @Override
    public void onPulled(float percentagePulled) 
    {
    	//Utils.log.v("[TRANS] onPulled "+percentagePulled);
    	 // Custom enter from top animation
    	if(percentagePulled == 0.0)
    	{
	        Animation enterAnim = AnimationUtils.loadAnimation(mHeaderTextView.getContext(), R.anim.refresh_enter_top);
	        enterAnim.setInterpolator((new AccelerateDecelerateInterpolator()));
	        enterAnim.setFillAfter(true);
	        mHeaderTextView.startAnimation(enterAnim);
    	}
        
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            final float progress = mInterpolator.getInterpolation(percentagePulled);
            mHeaderProgressBar.setProgress(Math.round(mHeaderProgressBar.getMax() * progress));
        }
    }

    @Override
    public void onRefreshStarted() 
    {
    	// When refresh started, we don't need the "Loading" sign anymore. Just hide it.
        if (mHeaderTextView != null) {
        	mHeaderTextView.clearAnimation();
            mHeaderTextView.setText(mRefreshingLabel);
        	mHeaderTextView.setVisibility(View.GONE);
        }
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            mHeaderProgressBar.setIndeterminate(true);
        }
    }

    /**
     * Set Text to show to prompt the user is pull (or keep pulling).
     * @param pullText - Text to display.
     */
    public void setPullText(CharSequence pullText) {
        mPullRefreshLabel = pullText;
        if (mHeaderTextView != null) {
            mHeaderTextView.setText(mPullRefreshLabel);
        }
    }

    /**
     * Set Text to show to tell the user that a refresh is currently in progress.
     * @param refreshingText - Text to display.
     */
    public void setRefreshingText(CharSequence refreshingText) {
        mRefreshingLabel = refreshingText;
    }

    protected Drawable getActionBarBackground(Context context) {
        int[] android_styleable_ActionBar = { android.R.attr.background };

        // Need to get resource id of style pointed to from actionBarStyle
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.actionBarStyle, outValue, true);
        // Now get action bar style values...
        TypedArray abStyle = context.getTheme().obtainStyledAttributes(outValue.resourceId,
                android_styleable_ActionBar);
        try {
            // background is the first attr in the array above so it's index is 0.
            return abStyle.getDrawable(0);
        } finally {
            abStyle.recycle();
        }
    }

    protected int getActionBarSize(Context context) {
        int[] attrs = { android.R.attr.actionBarSize };
        TypedArray values = context.getTheme().obtainStyledAttributes(attrs);
        try {
            return values.getDimensionPixelSize(0, 0);
        } finally {
            values.recycle();
        }
    }
}
