package com.jphonelite;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import android.net.*;
import android.database.*;
import android.provider.*;
import android.provider.Contacts.*;
import android.content.*;

import java.util.*;

import javaforce.*;
import javaforce.voip.*;

public class ViewGlobal extends Activity {
  private CheckBox cb_g729a;
  private TextView dndon, dndoff, speaker_threshold, speaker_delay;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showView();
  }

  @Override
  public void onPause() {
    super.onPause();
    saveSettings();
  }

  public void setText(TextView tv, String str) {
    tv.setText(str.toCharArray(),0,str.length());
  }

  public void showView() {
    setContentView(R.layout.global);

    dndon = (TextView) findViewById(R.id.settings_dndon);
    dndoff = (TextView) findViewById(R.id.settings_dndoff);
    speaker_threshold = (TextView) findViewById(R.id.settings_speaker_threshold);
    speaker_delay = (TextView) findViewById(R.id.settings_speaker_delay);
    cb_g729a = (CheckBox) findViewById(R.id.settings_g729a);

    setText(dndon, Settings.current.dndCodeOn);
    setText(dndoff, Settings.current.dndCodeOff);
    setText(speaker_threshold, "" + Settings.current.speakerThreshold);
    setText(speaker_delay, "" + Settings.current.speakerDelay);
    cb_g729a.setChecked(Settings.current.use_g729a);
  }

  public void saveSettings() {
    Settings.current.dndCodeOn = dndon.getText().toString();
    Settings.current.dndCodeOff = dndoff.getText().toString();
    Settings.current.use_g729a = cb_g729a.isChecked();
    try {
      Settings.current.speakerThreshold = Integer.valueOf(speaker_threshold.getText().toString());
    } catch (Exception e1) {
      Settings.current.speakerThreshold = 1000;
    }
    try {
      Settings.current.speakerDelay = Integer.valueOf(speaker_delay.getText().toString());
    } catch (Exception e2) {
      Settings.current.speakerDelay = 1250;
    }
    Settings.saveSettings();
  }
}
