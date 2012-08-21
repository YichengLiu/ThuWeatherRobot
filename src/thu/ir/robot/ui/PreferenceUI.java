package thu.ir.robot.ui;

import thu.ir.robot.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferenceUI extends PreferenceActivity  implements OnSharedPreferenceChangeListener {

    private EditTextPreference mServerAddressPreference;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mServerAddressPreference = (EditTextPreference)getPreferenceScreen().findPreference(getString(R.string.server_editor));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup the initial values
        mServerAddressPreference.setSummary("当前地址 ： " + prefs.getString(getString(R.string.server_editor), getString(R.string.default_server_editor))); 

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(getString(R.string.server_editor))) {
            mServerAddressPreference.setSummary("当前地址 ： " + prefs.getString(getString(R.string.server_editor), getString(R.string.default_server_editor)));
        }
    }
}
