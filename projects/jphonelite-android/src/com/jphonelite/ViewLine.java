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

public class ViewLine extends Activity {
  private TextView name, user, auth, pass, host;
  private int line, same;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    line = ViewSettings.line;
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

  public int getR() {
    switch (line) {
      case 1: return R.array.line_2;
      case 2: return R.array.line_3;
      case 3: return R.array.line_4;
      case 4: return R.array.line_5;
      case 5: return R.array.line_6;
    }
    return -1;
  }

  public void showView() {
    setContentView(line == 0 ? R.layout.line1 : R.layout.linex);

    same = Settings.current.lines[line].same;

    name = (TextView) findViewById(R.id.settings_name);
    user = (TextView) findViewById(R.id.settings_user);
    auth = (TextView) findViewById(R.id.settings_auth);
    pass = (TextView) findViewById(R.id.settings_pass);
    host = (TextView) findViewById(R.id.settings_host);

    setText(name, Settings.current.lines[line].name);
    setText(user, Settings.current.lines[line].user);
    setText(auth, Settings.current.lines[line].auth);
    setText(pass, Settings.getPassword(Settings.current.lines[line].pass));
    setText(host, Settings.current.lines[line].host);

    if (line != 0) {
      if (same != -1) {
        setEditable(false);
      }
      Spinner spinner = (Spinner) findViewById(R.id.settings_line_x);
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, getR(), android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setAdapter(adapter);
      int id = same + 1;
      if (id > line) id--;
      spinner.setSelection(id);
      spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
          same = ((int)id) - 1;
          if (same >= line) same++;  //don't duplicate self
          setEditable(same == -1);
        }
        public void onNothingSelected(AdapterView parent) {}
      });
      setText((TextView)findViewById(R.id.settings_line_x_label), "Line " + (line+1));
    }
  }

  public void setEditable(boolean state) {
    //android is missing EditText.setEditable() functionality...
  }

  public void saveSettings() {
    Settings.current.lines[line].name = name.getText().toString();
    Settings.current.lines[line].user = user.getText().toString();
    Settings.current.lines[line].auth = auth.getText().toString();
    Settings.current.lines[line].pass = "crypto(1," + Settings.encodePassword(pass.getText().toString()) + ")";
    Settings.current.lines[line].host = host.getText().toString();
    Settings.current.lines[line].same = same;
    Settings.saveSettings();
  }
}
