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

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.util.fusion.DeviceUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarSettings extends SettingsPreferenceFragment 
        implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";

    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";

    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String KEY_STATUS_BAR_TICKER = "status_bar_ticker_enabled";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

    private static final String PREF_BATT_BAR = "battery_bar_list";
    private static final String PREF_BATT_BAR_STYLE = "battery_bar_style";
    private static final String PREF_BATT_BAR_COLOR = "battery_bar_color";
    private static final String PREF_BATT_BAR_WIDTH = "battery_bar_thickness";
    private static final String PREF_BATT_ANIMATE = "battery_bar_animate";

    private static final String STATUS_BAR_BATTERY_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_BATTERY_STYLE_TEXT = "6";

    private PreferenceScreen mClockStyle;
    private SwitchPreference mTicker;
    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;
    private ListPreference mCircleAnimSpeed;
    private ListPreference mBatteryBar;
    private ListPreference mBatteryBarStyle;
    private ListPreference mBatteryBarThickness;
    private SwitchPreference mBatteryBarChargingAnimation;

    private ColorPickerPreference mBatteryBarColor;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.fusion_statusbar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return;
        }

        mStatusBarBatteryShowPercent = (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        int batteryShowPercent = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(batteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        int batteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mTicker = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_TICKER);
        final boolean tickerEnabled = systemUiResources.getBoolean(systemUiResources.getIdentifier(
                    "com.android.systemui:bool/enable_ticker", null, null));
        mTicker.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TICKER_ENABLED, tickerEnabled ? 1 : 0) == 1);
        mTicker.setOnPreferenceChangeListener(this);

        mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);

        mBatteryBar = (ListPreference) findPreference(PREF_BATT_BAR);
        mBatteryBar.setOnPreferenceChangeListener(this);
        mBatteryBar.setValue((Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR, 0)) + "");
        mBatteryBar.setSummary(mBatteryBar.getEntry());

        mBatteryBarStyle = (ListPreference) findPreference(PREF_BATT_BAR_STYLE);
        mBatteryBarStyle.setOnPreferenceChangeListener(this);
        mBatteryBarStyle.setValue((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_STYLE, 0)) + "");
        mBatteryBarStyle.setSummary(mBatteryBarStyle.getEntry());

        mBatteryBarColor = (ColorPickerPreference) findPreference(PREF_BATT_BAR_COLOR);
        mBatteryBarColor.setOnPreferenceChangeListener(this);
        int defaultColor = 0xffffffff;
        intColor = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_COLOR, defaultColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBatteryBarColor.setSummary(hexColor);

        mBatteryBarChargingAnimation = (SwitchPreference) findPreference(PREF_BATT_ANIMATE);
        mBatteryBarChargingAnimation.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, 0) == 1);

        mBatteryBarThickness = (ListPreference) findPreference(PREF_BATT_BAR_WIDTH);
        mBatteryBarThickness.setOnPreferenceChangeListener(this);
        mBatteryBarThickness.setValue((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, 1)) + "");
        mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntry());

        updateClockStyleDescription();
        updateBatteryBarOptions();
    }


    @Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
    }

    private void updateClockStyleDescription() {
        if (mClockStyle == null) {
            return;
        }
        if (Settings.System.getInt(getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
            mClockStyle.setSummary(getString(R.string.enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.disabled));
         }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_BATTERY_STYLE, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);

            enableStatusBarBatteryDependents((String) newValue);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            int batteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, batteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        } else if (preference == mTicker) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_TICKER_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mBatteryBarColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR_COLOR, intHex);
            return true;
        } else if (preference == mBatteryBar) {
            int val = Integer.valueOf((String) newValue);
            int index = mBatteryBar.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR, val);
            mBatteryBar.setSummary(mBatteryBar.getEntries()[index]);
            updateBatteryBarOptions();
            return true;
        } else if (preference == mBatteryBarStyle) {
            int val = Integer.valueOf((String) newValue);
            int index = mBatteryBarStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_STYLE, val);
            mBatteryBarStyle.setSummary(mBatteryBarStyle.getEntries()[index]);
            return true;
        } else if (preference == mBatteryBarThickness) {
            int val = Integer.valueOf((String) newValue);
            int index = mBatteryBarThickness.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, val);
            mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntries()[index]);
            return true;
        }
        return false;
    }

   public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mBatteryBarChargingAnimation) {
            value = mBatteryBarChargingAnimation.isChecked();
            Settings.System.putInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateBatteryBarOptions() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUSBAR_BATTERY_BAR, 0) == 0) {
            mBatteryBarStyle.setEnabled(false);
            mBatteryBarThickness.setEnabled(false);
            mBatteryBarChargingAnimation.setEnabled(false);
            mBatteryBarColor.setEnabled(false);
        } else {
            mBatteryBarStyle.setEnabled(true);
            mBatteryBarThickness.setEnabled(true);
            mBatteryBarChargingAnimation.setEnabled(true);
            mBatteryBarColor.setEnabled(true);
        }
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_BATTERY_STYLE_TEXT)
                || value.equals(STATUS_BAR_BATTERY_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }
}
