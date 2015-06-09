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

public class ViewSettings extends Activity {
  private CheckBox cb_g729a, cb_earpiece;
  private Button global, l1, l2, l3, l4, l5, l6;
  public static int line;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showView();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  public void setText(TextView tv, String str) {
    tv.setText(str.toCharArray(),0,str.length());
  }

  public void showView() {
    setContentView(R.layout.settings);

    global = (Button) findViewById(R.id.settings_global);
    l1 = (Button) findViewById(R.id.settings_l1);
    l2 = (Button) findViewById(R.id.settings_l2);
    l3 = (Button) findViewById(R.id.settings_l3);
    l4 = (Button) findViewById(R.id.settings_l4);
    l5 = (Button) findViewById(R.id.settings_l5);
    l6 = (Button) findViewById(R.id.settings_l6);

    global.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        startActivity(new Intent(ViewSettings.this, ViewGlobal.class));
      }
    });
    l1.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(0);
      }
    });
    l2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(1);
      }
    });
    l3.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(2);
      }
    });
    l4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(3);
      }
    });
    l5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(4);
      }
    });
    l6.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        selectLine(5);
      }
    });
  }

  public void selectLine(int newline) {
    line = newline;
    startActivity(new Intent(this, ViewLine.class));
  }
}
