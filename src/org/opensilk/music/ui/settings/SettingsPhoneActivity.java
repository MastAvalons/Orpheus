package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsPhoneActivity extends Activity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper themeHelper = ThemeHelper.getInstance(this);
        setTheme(themeHelper.getTheme());
        setContentView(R.layout.settings_fragment_activity);

        //Load the main fragment
        getFragmentManager().beginTransaction()
            .replace(R.id.settings_content, new SettingsMainFragment())
            .commit();

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
