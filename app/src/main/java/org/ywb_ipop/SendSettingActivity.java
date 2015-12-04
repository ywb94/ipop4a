package org.ywb_ipop;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import org.ywb_ipop.util.PreferenceConstants;

/**
 * Created by Administrator on 2015/4/25.
 */
public class SendSettingActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Send.Settings";
    private EditTextPreference send_period,send_height,send_fontsize,send_autosavedir;
    private ListPreference send_type,send_enter,send_loop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            addPreferencesFromResource(R.xml.send_prefs);
        } catch (ClassCastException e) {
            Log.e(TAG, "Shared preferences are corrupt! Resetting to default values.");
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            // Blow away all the preferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            PreferenceManager.setDefaultValues(this, R.xml.send_prefs, true);

            // Since they were able to get to the Settings activity, they already agreed to the EULA
            editor = preferences.edit();
            editor.putBoolean(PreferenceConstants.EULA, true);
            editor.commit();

            addPreferencesFromResource(R.xml.send_prefs);
        }
        send_period = (EditTextPreference) getPreferenceScreen().findPreference("send_period");
        send_height = (EditTextPreference) getPreferenceScreen().findPreference("send_height");
        send_fontsize = (EditTextPreference) getPreferenceScreen().findPreference("send_fontsize");
        send_autosavedir = (EditTextPreference) getPreferenceScreen().findPreference("send_autosavedir");

        send_type=(ListPreference) getPreferenceScreen().findPreference("send_type");
        send_enter=(ListPreference) getPreferenceScreen().findPreference("send_enter");
        send_loop=(ListPreference) getPreferenceScreen().findPreference("send_loop");
       /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();

        PreferenceManager.setDefaultValues(this, R.xml.send_prefs, true);
*/
    }

    private void updateSummaries() {

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // update values on changed preference
        //this.updateSummaries();
      /*  CharSequence value;
        Preference pref = this.findPreference(key);
        if(pref == null) return;
        if(pref instanceof CheckBoxPreference) return;
        value=((EditTextPreference)pref).getText();
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            int entryIndex = listPref.findIndexOfValue((String) value);
            if (entryIndex >= 0)
                value = listPref.getEntries()[entryIndex];
        }


        pref.setSummary(value);

    }*/
        if(key.equals("send_period"))
        {

               send_period.setSummary(send_period.getText());
        }
        else if(key.equals("send_height"))
        {
            send_height.setSummary(send_height.getText());
        }
        else if(key.equals("send_fontsize"))
        {
            send_fontsize.setSummary(send_fontsize.getText());
        }
        else if(key.equals("send_autosavedir"))
        {
            send_autosavedir.setSummary(send_autosavedir.getText());
        }
        else if(key.equals("send_type"))
        send_type.setSummary(send_type.getEntry());
        else if(key.equals("send_enter"))
        send_enter.setSummary(send_enter.getEntry());
        else if(key.equals("send_loop"))
            send_loop.setSummary(send_loop.getEntry());

    }

    @Override
    public void onResume() {
        super.onResume();
        try {

              send_period.setSummary(send_period.getText());
            if(send_autosavedir.getText().equals("")) {
                String temps=Environment.getExternalStorageDirectory().toString();
                send_autosavedir.setSummary(temps);
                send_autosavedir.setText(temps);
            }
            else
              send_autosavedir.setSummary(send_autosavedir.getText());
              send_height.setSummary(send_height.getText());
              send_fontsize.setSummary(send_fontsize.getText());

            send_type.setSummary(send_type.getEntry());
            send_enter.setSummary(send_enter.getEntry());
            send_loop.setSummary(send_loop.getEntry());


    }catch(Exception e){
            send_period.setSummary("1000");
            send_autosavedir.setSummary(Environment.getExternalStorageDirectory().toString());
            send_autosavedir.setText(Environment.getExternalStorageDirectory().toString());
            send_height.setSummary("300");
            send_fontsize.setSummary("25");

            send_type.setSummary(send_type.getEntry());
            send_enter.setSummary(send_enter.getEntry());
            send_loop.setSummary(send_loop.getEntry());
    }
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
