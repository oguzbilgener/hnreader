package me.oguzb.hnreader.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import me.oguzb.hnreader.R;

public class AboutDialog extends DialogFragment implements View.OnClickListener
{
	public static final String TAG = "AboutDialog";
	private TextView brief, rateText, emailText, githubText;
	private String rateData, emailData, githubData;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Create an alert dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		builder.setTitle(getActivity().getString(R.string.about_title));

		// Prepare the about brief text
		String versionName = getString(R.string.VERSION_NAME);
		String aboutText = getString(R.string.about_brief);
		String versionFloat = "";
		try {
			versionFloat = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch(Exception e) {}
		aboutText = String.format(aboutText, new String[]{versionFloat, versionName});

		rateData = getString(R.string.playstore_prefix)+getActivity().getPackageName();
		emailData = getString(R.string.developer_email);
		githubData = getString(R.string.github_project_url);


		// Inflate the container
		View container = inflater.inflate(R.layout.about_dialog, null);

		// Find TextViews
		brief = (TextView) container.findViewById(R.id.about_brief);
		rateText = (TextView) container.findViewById(R.id.about_link_rate);
		emailText = (TextView) container.findViewById(R.id.about_link_email);
		githubText = (TextView) container.findViewById(R.id.about_link_github);

		brief.setText(aboutText);

		// Set some OnClickListeners
		rateText.setOnClickListener(this);
		emailText.setOnClickListener(this);
		githubText.setOnClickListener(this);

		// Set the container as the parent View
		builder.setView(container);
		return builder.create();
	}

	@Override
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case R.id.about_link_rate:
				openRate();
			break;

			case R.id.about_link_email:
				openEmail();
			break;

			case R.id.about_link_github:
				openGithub();
			break;
		}
	}

	public void openRate()
	{
		Uri uri = Uri.parse(rateData);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	public void openEmail()
	{
		String emailBody = "";
		try {
			String appVersion = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
			int appVersionCode = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
			emailBody += "App Version: "+appVersion+" ("+appVersionCode+")\n";
		} catch(Exception e) {}
		try {
			emailBody += "Device: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"\n";
		} catch(Exception e) {}
		try {
			emailBody += "Android "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\n";
		} catch(Exception e) {}
		emailBody += "\n";

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailData});
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_email_title));
		intent.putExtra(Intent.EXTRA_TEXT, emailBody);

		startActivity(Intent.createChooser(intent, getString(R.string.contact_email_dialog_title)));
	}

	public void openGithub()
	{
		Uri uri = Uri.parse(githubData);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
}
