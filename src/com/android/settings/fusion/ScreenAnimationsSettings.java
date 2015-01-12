/*
 * Copyright (C) 2014 The Fusion Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fusion;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.SwitchPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ScreenAnimationsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "ScreenAnimationsSettings";

    private static final String PROP_DISPLAY_DENSITY = "persist.sf.lcd_density";
    private static final String PROP_DISPLAY_DENSITY_MAX = "ro.sf.lcd_density.max";
    private static final String PROP_DISPLAY_DENSITY_MIN = "ro.sf.lcd_density.min";
    private static final String PROP_DISPLAY_DENSITY_OVERRIDE = "persist.sf.lcd_density.override";

    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String DISABLE_IMMERSIVE_MESSAGE = "disable_immersive_message";
    private static final String KEY_DISPLAY_DENSITY = "display_density";
    private static final String KEY_DISPLAY_DENSITY_OVERRIDE = "display_density_override";

    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private ListPreference mToastAnimation;
    private EditTextPreference mDisplayDensity;
    private EditTextPreference mDisplayDensityOverride;
    private SwitchPreference mDisableIM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.fusion_screen_animations_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        // ListView Animations
        mListViewAnimation = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        mToastAnimation = (ListPreference) prefSet.findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(
                getContentResolver(),Settings.System.TOAST_ANIMATION, 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation);
        mToastAnimation.setOnPreferenceChangeListener(this);

        mDisableIM = (SwitchPreference) findPreference(DISABLE_IMMERSIVE_MESSAGE);
        mDisableIM.setChecked((Settings.System.getInt(resolver,
                Settings.System.DISABLE_IMMERSIVE_MESSAGE, 0) == 1));

        mDisplayDensity = (EditTextPreference) findPreference(KEY_DISPLAY_DENSITY);
        mDisplayDensity.setText(SystemProperties.get(PROP_DISPLAY_DENSITY, "0"));
        mDisplayDensity.setOnPreferenceChangeListener(this);

        mDisplayDensityOverride = (EditTextPreference) findPreference(KEY_DISPLAY_DENSITY_OVERRIDE);
        mDisplayDensityOverride.setText(SystemProperties.get(PROP_DISPLAY_DENSITY_OVERRIDE, "0"));
        mDisplayDensityOverride.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if  (preference == mDisableIM) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DISABLE_IMMERSIVE_MESSAGE, checked ? 1:0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(value > 0);
        }
        if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
        }
        if (KEY_TOAST_ANIMATION.equals(key)) {
            int index = mToastAnimation.findIndexOfValue((String) objValue);
            Settings.System.putString(getContentResolver(),
                    Settings.System.TOAST_ANIMATION, (String) objValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(getActivity(), "Toast animation test!!!",
                    Toast.LENGTH_SHORT).show();
        }
        if (KEY_DISPLAY_DENSITY.equals(key)) {
            final int max = SystemProperties.getInt(PROP_DISPLAY_DENSITY_MAX, 480);
            final int min = SystemProperties.getInt(PROP_DISPLAY_DENSITY_MIN, 240);

            int value = SystemProperties.getInt(PROP_DISPLAY_DENSITY, 0);
            try {
                value = Integer.parseInt(String.valueOf(objValue));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input", e);
            }

            // 0 disables the custom density, so do not check for the value, else…
            if (value != 0) {
                // …cap the value
                if (value < min) {
                    value = min;
                } else if (value > max) {
                    value = max;
                }
            }

            SystemProperties.set(PROP_DISPLAY_DENSITY, String.valueOf(value));
            mDisplayDensity.setText(String.valueOf(value));

            // we handle it, return false
            return false;
        }
        if (KEY_DISPLAY_DENSITY_OVERRIDE.equals(key)) {
            int value = SystemProperties.getInt(PROP_DISPLAY_DENSITY_OVERRIDE, 0);
            try {
                value = Integer.parseInt(String.valueOf(objValue));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input", e);
            }

            SystemProperties.set(PROP_DISPLAY_DENSITY_OVERRIDE, String.valueOf(value));
            mDisplayDensityOverride.setText(String.valueOf(value));

            // we handle it, return false
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
