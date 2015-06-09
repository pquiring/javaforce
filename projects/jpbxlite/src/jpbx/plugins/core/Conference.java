package jpbx.plugins.core;

import java.util.*;

import jpbx.core.*;

/* Conference - see IVR.java for logic code (conferences are special IVRs) */

public class Conference {
  public static enum State {
    CONF_WAIT,  //waiting for admin (aka chair person)
    CONF_TALK,  //in session
    CONF_DROP   //admin has left, drop all regular users
  }
  public static final int bufs = 5;
  public static class Member {
    public boolean dropped, admin;
    public short buf[][] = new short[bufs][160];
    public int idx;  //points to head buffer in 3 buffers (0-2) (last buffer is always volatile)
    public int idxs[];  // = new int[memberList.size()];
    public CallDetailsPBX cd;
    public Object lock = new Object();
    public Vector<Member> memberList;
  }

  public static HashMap<String, Vector<Member>> list = new HashMap<String, Vector<Member>>();
  public static Object lock = new Object();

  //the actual Conference object contains no data and is never created
}
