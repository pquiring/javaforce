package javaforce.voip;

/**
 * Keeps track of Call Details based on the 'callid' field of SIP messages.
 */
public class CallDetails {

  /**
   * Every call has 2 sides, orginator and terminator.
   */
  public static class SideDetails {

    public int cseq = 0;
    public int expires = 0;
    public String to[], from[];  //0=name < 1=# 2=host/port ... > ':' ...
    public String uri;
    public String contact;
    public String vialist[];
    public String branch;
    public String extra;   //extra headers
    public long o1, o2;
    public Codec codecs[];
    public String host;
    public int port;
    public String rtp_host;
    public int rtp_port_audio, rtp_port_video;
  };
  /**
   * Keeps track of details from source side (call originator)
   */
  public SideDetails src = new SideDetails();
  /**
   * Keeps track of details from destination side (call terminator)
   */
  public SideDetails dst = new SideDetails();
  public String callid;  //unique id for this call leg (not caller ID)
  public String sdp;
  public String authstr;
  public boolean authsent;  //was auth (401/407) tried
  public boolean terminated;
  public boolean holding;  //hold started this side (sendonly)
  public boolean onhold;  //hold started other side (recvonly)
}
