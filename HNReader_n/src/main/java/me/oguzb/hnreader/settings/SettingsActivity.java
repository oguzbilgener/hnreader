package me.oguzb.hnreader.settings;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.db.Db;
import me.oguzb.hnreader.utils.Utils;

public class SettingsActivity extends PreferenceActivity
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener
{
    private Context context;
    private ActionBar actionBar;
    private Db db;

	ListPreference userAgent;

	Integer uaVal;
	String[] uaList;

	private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_layout);
        context = this;

        // Set up the ActionBar with some options
        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Connect to database
        db = new Db(this);

		// Get SharedPrefs
		prefs = getSharedPreferences(getString(R.string.sharedprefs_main), Context.MODE_PRIVATE);

        Preference deleteDbNews = findPreference(getString(R.string.s_key_delete_news));
		userAgent = (ListPreference) findPreference(getString(R.string.s_key_user_agent));

        deleteDbNews.setOnPreferenceClickListener(this);
		userAgent.setOnPreferenceChangeListener(this);

		// User Agent settings is stored as an integer in the SharedPreferences key-value store
		uaVal = prefs.getInt(getString(R.string.ua_key), 0);
		// This is the list of the User Agent names (or device names)
		// that will be displayed to the user
		uaList = getResources().getStringArray(R.array.s_titles_ua);
		if(uaVal<0 || uaVal >= uaList.length)
			uaVal = 0;
		// Current user agent
		String uaDisplay = uaList[uaVal];

		userAgent.setDefaultValue(uaDisplay);
		userAgent.setValue(uaDisplay);
		userAgent.setSummary(uaDisplay);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        db.open();
    }

    @Override
    public void onPause()
    {
        db.close();
        super.onPause();
    }

    @Override
    public boolean onPreferenceClick(Preference pref)
    {
        switch(pref.getTitleRes())
        {
            case R.string.s_title_delete_news:
                deleteNews();
                return true;
			case R.string.s_title_user_agent:

			break;
            default:
                Utils.log.w("[SET] onPreferenceClick: Unknown preference element");
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
                finish();
            break;
        }
        return true;
    }

    private void deleteNews()
    {
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                // Clear all kinds of news
                db.clearNews(Db.NEWS_HOMEPAGE);
                db.clearNews(Db.NEWS_NEWEST);
                db.clearNews(Db.NEWS_ASK);
                return null;
            }
            @Override
            protected void onPostExecute(Void r)
            {
                Utils.Toast(context, getString(R.string.s_result_debug));
            }
        }.execute();
    }

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		switch(pref.getTitleRes())
		{
			case R.string.s_title_user_agent:
				// When the user changes the User Agent preference,
				// Find new value's integer equivalent and store it.
				Integer valueToStore = getIntValueOfUaDisplay(newValue.toString(), uaList);
				if(valueToStore == null)
					valueToStore = 0;

				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(getString(R.string.ua_key), valueToStore);
				editor.commit();

				userAgent.setSummary(newValue.toString());
			break;
		}
		return false;
	}

	public static Integer getIntValueOfUaDisplay(String display, String[] uaList)
	{
		if(display == null || uaList == null)
			return null;
		for(int i=0; i<uaList.length; i++)
		{
			if(uaList[i].equals(display))
				return i;
		}
		return null;
	}
}
