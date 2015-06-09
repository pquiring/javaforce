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

/**
 * jPhoneLite for Android : Main Activity
 *
 * author : Peter Quiring (pquiring at gmail dot com)
 *
 * website : jphonelite.sourceforge.net
 */

public class Main extends Activity {

  private String ICICLE_KEY = "jpl-view";
  private TextView dial, status;
  private Button l1, l2, l3, l4, l5, l6;
  private Button xfr, hld, aa, ac, dnd, cnf;
  private Button n0, n1, n2, n3, n4, n5, n6, n7, n8, n9;
  private Button star, pound;
  private Button contacts, callButton, del;
  protected static Button spk;
  private Button lineButtons[];  //make it easy to access them
  private java.util.Timer buttonTimer;
  private final int PICK_CONTACT = 0;
  private final int VIEW_SETTINGS = 1;
  private final int DIALOG_CONTACTS_ID = 0;
  private final int DIALOG_ABOUT_ID = 1;
  private ArrayList<CharSequence> contactList;
  private String ns;  //notification service
  private NotificationManager mNotificationManager;
  private final int NOTIFY_ID = 1;
  private boolean pickingContact = false;

  public final String TAG = "JPLMAIN";
  public static Handler handler = null;
  public boolean exiting = false;
  public Engine engine;
  public static boolean active = false;

  /**
   * Called when Activity is first created. Turns off the title bar, sets up
   * the content views, and fires up the View.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
Log.i(TAG, "onCreate()"+this);
    super.onCreate(savedInstanceState);

    if (handler == null) handler = new Handler();

    requestWindowFeature(Window.FEATURE_NO_TITLE);  // No Title bar

    ns = Context.NOTIFICATION_SERVICE;
    mNotificationManager = (NotificationManager) getSystemService(ns);

    engine = Engine.getInstance(this, this);

    showView();

    spk.setBackgroundResource(Settings.current.speakerMode ? R.drawable.spk_green : R.drawable.spk_grey);
  }

  @Override
  protected void onStart() {
Log.i(TAG, "onStart()"+this);
    super.onStart();
    // The activity is about to become visible.
  }

  @Override
  protected void onResume() {
Log.i(TAG, "onResume()"+this);
    super.onResume();
    // The activity has become visible (it is now "resumed").
    active = true;
    notify(null, false);
    startButtonTimer();
    updateScreen();
  }

  @Override
  protected void onPause() {
Log.i(TAG, "onPause()"+this);
    super.onPause();
    // Pause the app along with the activity
    active = false;
    stopButtonTimer();
    if ((engine.active) && (!pickingContact)) {
      if (!exiting) {
        if (engine.line == -1) notify("Idle", false); else notify(engine.lines[engine.line].incall ? "Incall" : "Idle", false);
      }
      if (!isFinishing()) finish();  //BUG : notify() intent still sometimes creates a second instance of this class (so confusing)
    }
  }

  @Override
  protected void onStop() {
Log.i(TAG, "onStop()"+this);
    super.onStop();
    // The activity is no longer visible (it is now "stopped")
  }

  @Override
  protected void onDestroy() {
Log.i(TAG, "onDestroy()"+this);
    super.onDestroy();
    // The activity is about to be destroyed.
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    //Store the game state
    outState.putBundle(ICICLE_KEY, saveState());
  }

  public void notify(String msg, boolean sound) {
    if (msg == null) {
      mNotificationManager.cancel(NOTIFY_ID);
      return;
    }

    int icon = R.drawable.ic_main;
    CharSequence tickerText = msg;
    long when = System.currentTimeMillis();
    Notification notification = new Notification(icon, tickerText, when);

    Context context = getApplicationContext();
    CharSequence contentTitle = "VoIP Phone Status";
    CharSequence contentText = msg;
    Intent notificationIntent = new Intent(this, Main.class);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    notification.setLatestEventInfo(context, contentTitle, contentText, pendingIntent);
    if (sound) {
      notification.defaults |= Notification.DEFAULT_SOUND;
      notification.defaults |= Notification.DEFAULT_VIBRATE;
    }
    mNotificationManager.notify(NOTIFY_ID, notification);
  }

  /**
   * Restore game state if our process is being relaunched
   *
   * @param icicle a Bundle containing the app state
   */
  public void restoreState(Bundle icicle) {
    // member = icicle.getX("name");
  }

  /**
   * Saves state if our process is being relaunched
   *
   * @return a Bundle containing the app state
   */
  public Bundle saveState() {
    Bundle map = new Bundle();
    // map.putX("name", value);
    return map;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.about:
        about();
        return true;
      case R.id.settings:
        showSettings();
        return true;
      case R.id.exit:
        exiting = true;
        if (engine.active) {
          engine.uninit();
          engine.release();
        }
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  protected Dialog onCreateDialog(int id) {
    Dialog dialog;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    switch(id) {
      case DIALOG_CONTACTS_ID:
        builder.setTitle("Pick a number");
        builder.setItems((CharSequence[])contactList.toArray(new CharSequence[] {}), new DialogInterface.OnClickListener() {
          private ArrayList<CharSequence> contactList;
          public void onClick(DialogInterface dialog, int item) {
            String res[] = contactList.get(item).toString().split(":");
            engine.lines[engine.line].dial = res[0].replaceAll("-", "");
            updateScreen();
          }
          public DialogInterface.OnClickListener init(ArrayList<CharSequence> contactList) {
            this.contactList = contactList;
            return this;
          }
        }.init(contactList));
        dialog = builder.create();
        break;
      case DIALOG_ABOUT_ID:
        builder.setTitle("About")
          .setMessage(R.string.main_about_text)
          .setCancelable(false)
          .setPositiveButton("Ok", null);
        dialog = builder.create();
        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  protected void onPrepareDialog (int id, Dialog dialog) {
    switch (id) {
      case DIALOG_CONTACTS_ID:
    }
  }

  private String getType(String type) {
    if (type.equals("1")) return "HOME";
    if (type.equals("2")) return "MOBILE";
    if (type.equals("3")) return "WORK";
    return "OTHER";
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
//Log.i(TAG, "onActivityResult(" + reqCode + "," + resultCode + "," + data);
    switch (reqCode) {
      case PICK_CONTACT:
        pickingContact = false;
//        if (resultCode == Activity.RESULT_OK) {
          if (data == null) break;
          Uri contactData = data.getData();
          if (contactData == null) break;
//Log.i(TAG, "Uri=" + contactData);
          ContentResolver cr = getContentResolver();
          Cursor c = managedQuery(contactData, null, null, null, null);
//Log.i(TAG, "Cursor=" + c);
          if (c.moveToFirst()) {
//Log.i(TAG, "moveToFirst = true");
            if (Integer.parseInt(c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
              String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
              Cursor pCur = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id},
                null);
              contactList = new ArrayList<CharSequence>();
              while (pCur.moveToNext()) {
                String num = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String type = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (num != null) {
//Log.i(TAG, "num=" + num + ",type=" + type);
                  String ent = num.replaceAll("-", "") + ":" + getType(type);
                  boolean dup = false;
                  for(int a=0;a<contactList.size();a++) {
                    if (contactList.get(a).toString().split(":")[0].equals(ent.split(":")[0])) {dup = true; break;}
                  }
                  if (!dup) contactList.add((CharSequence)ent);
                }
              }
              if (contactList.size() > 1) {
//Log.i(TAG, "contactList.size=" + contactList.size());
                removeDialog(DIALOG_CONTACTS_ID);  //make sure it's deleted
                showDialog(DIALOG_CONTACTS_ID);
              } else if (contactList.size() == 1) {
                String res[] = contactList.get(0).toString().split(":");
                engine.lines[engine.line].dial = res[0].replaceAll("-", "");
                updateScreen();
              }
            }
          }
//        }
        break;
      case VIEW_SETTINGS:
        //return from ViewSettings
        engine.reinit();
        updateScreen();
        break;
    }
  }

  public void showView() {
//    Display display = getWindowManager().getDefaultDisplay();
//    Log.i(TAG, "ScreenSize=" + display.getWidth() + "x" + display.getHeight());
    setContentView(R.layout.main);

    dial = (TextView) findViewById(R.id.main_text1);
    status = (TextView) findViewById(R.id.main_text2);

    spk = (Button) findViewById(R.id.main_spk);
    spk.setBackgroundResource(Settings.current.speakerMode ? R.drawable.spk_green : R.drawable.spk_grey);

    l1 = (Button) findViewById(R.id.main_l1);
    l2 = (Button) findViewById(R.id.main_l2);
    l3 = (Button) findViewById(R.id.main_l3);
    l4 = (Button) findViewById(R.id.main_l4);
    l5 = (Button) findViewById(R.id.main_l5);
    l6 = (Button) findViewById(R.id.main_l6);
    lineButtons = new Button[6];
    lineButtons[0] = l1;
    lineButtons[1] = l2;
    lineButtons[2] = l3;
    lineButtons[3] = l4;
    lineButtons[4] = l5;
    lineButtons[5] = l6;

    xfr = (Button) findViewById(R.id.main_xfr);
    hld = (Button) findViewById(R.id.main_hld);
    aa = (Button) findViewById(R.id.main_aa);
    ac = (Button) findViewById(R.id.main_ac);
    dnd = (Button) findViewById(R.id.main_dnd);
    cnf = (Button) findViewById(R.id.main_cnf);

    n0 = (Button) findViewById(R.id.main_0);
    n0.setBackgroundResource( R.drawable.phone_0 );
    n1 = (Button) findViewById(R.id.main_1);
    n1.setBackgroundResource( R.drawable.phone_1 );
    n2 = (Button) findViewById(R.id.main_2);
    n2.setBackgroundResource( R.drawable.phone_2 );
    n3 = (Button) findViewById(R.id.main_3);
    n3.setBackgroundResource( R.drawable.phone_3 );
    n4 = (Button) findViewById(R.id.main_4);
    n4.setBackgroundResource( R.drawable.phone_4 );
    n5 = (Button) findViewById(R.id.main_5);
    n5.setBackgroundResource( R.drawable.phone_5 );
    n6 = (Button) findViewById(R.id.main_6);
    n6.setBackgroundResource( R.drawable.phone_6 );
    n7 = (Button) findViewById(R.id.main_7);
    n7.setBackgroundResource( R.drawable.phone_7 );
    n8 = (Button) findViewById(R.id.main_8);
    n8.setBackgroundResource( R.drawable.phone_8 );
    n9 = (Button) findViewById(R.id.main_9);
    n9.setBackgroundResource( R.drawable.phone_9 );

    star = (Button) findViewById(R.id.main_star);
    star.setBackgroundResource( R.drawable.phone_star );
    pound = (Button) findViewById(R.id.main_pound);
    pound.setBackgroundResource( R.drawable.phone_pound );
    contacts = (Button) findViewById(R.id.main_contacts);
    contacts.setBackgroundResource( R.drawable.phone_contacts );
    callButton = (Button) findViewById(R.id.main_call);
    callButton.setBackgroundResource( R.drawable.phone_green );
    del = (Button) findViewById(R.id.main_del);
    del.setBackgroundResource( R.drawable.phone_del );

    spk.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Settings.current.speakerMode = !Settings.current.speakerMode;
        spk.setBackgroundResource(Settings.current.speakerMode ? R.drawable.spk_green : R.drawable.spk_grey);
        engine.sound.restartSound();
      }
    });
    l1.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(0);
      }
    });
    l2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(1);
      }
    });
    l3.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(2);
      }
    });
    l4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(3);
      }
    });
    l5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(4);
      }
    });
    l6.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        engine.selectLine(5);
      }
    });

    xfr.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_xfr();
      }
    });
    hld.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_hld();
      }
    });
    aa.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_aa();
      }
    });
    ac.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_ac();
      }
    });
    dnd.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_dnd();
      }
    });
    cnf.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_cnf();
      }
    });

    n0.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('0');
      }
    });
    n1.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('1');
      }
    });
    n2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('2');
      }
    });
    n3.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('3');
      }
    });
    n4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('4');
      }
    });
    n5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('5');
      }
    });
    n6.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('6');
      }
    });
    n7.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('7');
      }
    });
    n8.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('8');
      }
    });
    n9.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('9');
      }
    });

    star.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('*');
      }
    });
    pound.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        addDigit('#');
      }
    });
    contacts.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_contacts();
      }
    });
    del.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_del();
      }
    });
    del.setOnLongClickListener(new View.OnLongClickListener() {
      public boolean onLongClick(View v) {
        engine.clearDial();
        return true;
      }
    });
    callButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        do_call();
      }
    });
    callButton.setOnLongClickListener(new View.OnLongClickListener() {
      public boolean onLongClick(View v) {
        redial();
        return true;
      }
    });

    aa.setBackgroundResource( Settings.current.aa ? R.drawable.sel_green : R.drawable.sel_grey );
    ac.setBackgroundResource( Settings.current.ac ? R.drawable.sel_green : R.drawable.sel_grey );
  }

  public void setText(TextView tv, String str) {
    tv.setText(str.toCharArray(),0,str.length());
  }

  public void startButtonTimer() {
    if (buttonTimer == null) {
      buttonTimer = new Timer();
      buttonTimer.schedule(new ButtonColorUpdater(), 250, 250);
    }
  }

  public void stopButtonTimer() {
    if (buttonTimer != null) {
      buttonTimer.cancel();
      buttonTimer = null;
    }
  }

  public void do_xfr() {
    engine.do_xfr();
    updateScreen();
  }

  public void do_hld() {
    engine.do_hld();
    updateScreen();
  }

  public void do_aa() {
    Settings.current.aa = !Settings.current.aa;
    aa.setBackgroundResource( Settings.current.aa ? R.drawable.sel_green : R.drawable.sel_grey );
  }

  public void do_ac() {
    Settings.current.ac = !Settings.current.ac;
    ac.setBackgroundResource( Settings.current.ac ? R.drawable.sel_green : R.drawable.sel_grey );
  }

  public void do_dnd() {
    engine.do_dnd();
    updateScreen();
    call();
  }

  public void do_cnf() {
    engine.do_cnf();
    updateScreen();
  }

  public void do_call() {
    engine.do_call();
  }

  public void do_contacts() {
    if (engine.line == -1) return;
    if (engine.lines[engine.line].incall) return;
    pickingContact = true;
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
    startActivityForResult(intent, PICK_CONTACT);
  }

  public void do_del() {
    addDigit('x');
  }

  public void addDigit(char digit) {
    engine.addDigit(digit);
    updateScreen();
  }

  /** Update the number to be dialed, status and buttons for current selected line. */

  public void updateScreen() {
//Log.i(TAG, "updateScreen():line=" + engine.line);
    if (engine.line == -1) {
      setText(dial, "");
      setText(status, "");
      xfr.setBackgroundResource( R.drawable.sel_grey );
      hld.setBackgroundResource( R.drawable.sel_grey );
      dnd.setBackgroundResource( R.drawable.sel_grey );
      cnf.setBackgroundResource( R.drawable.sel_grey );
      callButton.setBackgroundResource( R.drawable.phone_red );
      return;
    }
//Log.i(TAG, "updateScreen():status=" + engine.lines[engine.line].status);
    setText(dial, engine.lines[engine.line].dial);
    setText(status, engine.lines[engine.line].status);
    xfr.setBackgroundResource( engine.lines[engine.line].xfr ? R.drawable.sel_green : R.drawable.sel_grey );
    hld.setBackgroundResource( engine.lines[engine.line].hld ? R.drawable.sel_green : R.drawable.sel_grey );
    dnd.setBackgroundResource( engine.lines[engine.line].dnd ? R.drawable.sel_green : R.drawable.sel_grey );
    cnf.setBackgroundResource( engine.lines[engine.line].cnf ? R.drawable.sel_green : R.drawable.sel_grey );
    callButton.setBackgroundResource( engine.lines[engine.line].incall ? R.drawable.phone_red : R.drawable.phone_green );
  }

  /** TimerTask executed every 250ms to change the line lights(icons). */

  private class ButtonColorUpdater extends TimerTask {
    private boolean odd = false;
    public void run() {
//Log.i(TAG, "ButtonColorUpdater:line=" + engine.line);
      int clr = R.drawable.black;
      for(int a=0;a<6;a++) {
        if ((engine.lines[a].sip != null) && (engine.lines[a].sip.isRegistered())) {
          clr = (engine.line == a ? R.drawable.sel_grey : R.drawable.grey);
          if (engine.lines[a].msgwaiting) {
            if (odd) clr = (engine.line == a ? R.drawable.sel_orange : R.drawable.orange);
          }
          if (engine.lines[a].incoming) {
            if (odd) clr = (engine.line == a ? R.drawable.sel_green : R.drawable.green);
              else clr = (engine.line == a ? R.drawable.sel_grey : R.drawable.grey);
          } else if (engine.lines[a].incall) clr = (engine.line == a ? R.drawable.sel_green : R.drawable.green);
        } else {
          if (engine.lines[a].unauth)
            clr = (engine.line == a ? R.drawable.sel_red : R.drawable.red);
          else
            clr = (engine.line == a ? R.drawable.sel_black : R.drawable.black);
        }
        if (engine.lines[a].clr != clr) {
//Log.i(TAG, "ButtonColorUpdater:line=" + (a+1) + "=" + clr);
          _setBackgroundResource(lineButtons[a], clr);
          engine.lines[a].clr = clr;
        }
      }
      odd = !odd;
    }
  }

  /** Starts a call or accepts an inbound call. (may!UI thread) */

  public void call() {
    engine.call();
    _updateScreen();
  }

  public void redial() {
    engine.redial();
    updateScreen();
  }

  public void about() {
    showDialog(DIALOG_ABOUT_ID);
  }

  public void showSettings() {
    for(int a=0;a<6;a++) {
      if (engine.lines[a].incall) return;
    }
    engine.uninit();
    Intent intent = new Intent(this, ViewSettings.class);
    startActivityForResult(intent, VIEW_SETTINGS);
  }

  //these members call the same member without the underscore in the UI thread

  public void _updateScreen() {
    handler.post(new Runnable() {public void run() { updateScreen(); }});
  }

  public static void _setBackgroundResource(Button b, int clr) {
    handler.post(new Runnable() {
      private Button b;
      private int clr;
      public void run() { b.setBackgroundResource(clr); }
      public Runnable init(Button b, int clr) { this.b = b; this.clr = clr; return this;}
    }.init(b, clr));
  }
}
