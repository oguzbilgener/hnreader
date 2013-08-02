package me.oguzb.hnreader.news;


import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.utils.Utils;

public class ListWarningController
{
	private Context context;
	private View container;
	private View button;
	private TextView textView;
	private ViewPropertyAnimator animator;

	public static final int RESOURCE_ID = R.layout.news_list_expired_warning;
	public static final int CLICKABLE_ID = R.id.news_list_expired_clickable;
	public static final int SHOW_DEFAULT_DELAY = 1000;
	public static final int HIDE_DEFAULT_DELAY = 1000;

	public ListWarningController(LayoutInflater inflater, ViewGroup rootView, Activity c, View.OnClickListener listener)
	{
		context = c;
		// Inflate the View
		container = inflater.inflate(RESOURCE_ID, null, false);
		rootView.addView(container);
		// Find the Button
		button = container.findViewById(CLICKABLE_ID);
		// Parent Activity is often the listener
		button.setOnClickListener(listener);
		// Find the TextView
		textView = (TextView) container.findViewById(R.id.news_list_expired_message_text);
		try
		{
			// Put the container into the bottom of the screen
			int height = (int) Utils.getPixelsByDp(context, context.getResources().getDimension(R.dimen.f_warning_height));
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) container.getLayoutParams();
			params.height = height;
			params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			container.setLayoutParams(params);
		}
		catch(Exception e)
		{
			Utils.log.w("[WC] exception while trying to set layout params");
			e.printStackTrace();
		}

		animator = container.animate();

		hideWarning(0);
	}

	public void showWarning(int delay)
	{
		container.setAlpha(0);
		container.setVisibility(View.VISIBLE);
		if(delay == 0)
		{
			// show the warning container immediately
			container.setAlpha(1);
		}
		else
		{
			// Start the animator
			animator.cancel();
			animator.alpha(1).setDuration(delay).setListener(null).start();
		}
	}

	public void hideWarning(int delay)
	{
		if(delay == 0)
		{
			// Hide the warning container immediately
			container.setAlpha(0);
			container.setVisibility(View.GONE);
		}
		else
		{
			animator.cancel();
			animator.alpha(0).setDuration(delay).setListener(new Animator.AnimatorListener()
			{

				@Override
				public void onAnimationEnd(Animator animation) {
					container.setVisibility(View.GONE);
				}

				@Override
				public void onAnimationStart(Animator animation) {}
				@Override
				public void onAnimationCancel(Animator animation) {}
				@Override
				public void onAnimationRepeat(Animator animation) {}
			}).start();
		}
	}

	public void showWarning()
	{
		showWarning(SHOW_DEFAULT_DELAY);
	}

	public void hideWarning()
	{
		hideWarning(HIDE_DEFAULT_DELAY);
	}

	public void setText(CharSequence text)
	{
		textView.setText(text);
	}

	public void setTextColor(int color)
	{
		textView.setTextColor(color);
	}
}
