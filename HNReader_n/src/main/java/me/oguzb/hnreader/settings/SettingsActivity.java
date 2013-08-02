package me.oguzb.hnreader.settings;

import android.app.ActionBar;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import me.oguzb.hnreader.R;
import me.oguzb.hnreader.db.Db;
import me.oguzb.hnreader.utils.Utils;

public class SettingsActivity extends PreferenceActivity
    implements Preference.OnPreferenceClickListener
{
    private Context context;
    private ActionBar actionBar;
    private Db db;

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

        Preference deleteDbNews = findPreference(getString(R.string.s_key_delete_news));
        deleteDbNews.setOnPreferenceClickListener(this);
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
}
