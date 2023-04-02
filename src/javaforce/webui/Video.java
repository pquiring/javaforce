package javaforce.webui;

/** Video
 *
 * @author pquiring
 */

public class Video extends Component {
  private String src;
  private boolean inited;
  private int state;

  public static final int STATE_UNINIT = 0;
  public static final int STATE_STOP = 1;
  public static final int STATE_PLAY = 2;
  public static final int STATE_PAUSE = 3;

  public Video() {
    addEvent("onplay", "media_onplay(this);");
    addEvent("onstop", "media_onstop(this);");
    addEvent("onpause", "media_onpause(this);");
    addEvent("onseeking", "console.log('Video.seeking:readyState=' + this.readyState);");
    addEvent("onstaled", "console.log('Video.staled:readyState=' + this.readyState);");
    addEvent("onsuspend", "console.log('Video.suspend:readyState=' + this.readyState);");
    addEvent("onwaiting", "console.log('Video.waiting:readyState=' + this.readyState);");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<video");
    sb.append(getAttrs());
    if (width != 0) {
      sb.append(" width='" + width + "'");
    }
    if (height != 0) {
      sb.append(" height='" + height + "'");
    }
    if (src != null) {
      sb.append(" src='" + src + "'");
    }
    sb.append(" controls autoplay></video>");
    return sb.toString();
  }
  public void setSource(String url) {
    src = url;
  }
  public void init(String codecs) {
    if (inited) return;
    sendEvent("media_init", new String[] {"codecs=" + codecs});
    inited = true;
  }
  public void play() {
    sendEvent("media_play", null);
    state = STATE_PLAY;
  }
  public void pause() {
    sendEvent("media_pause", null);
    state = STATE_PAUSE;
  }
  public void stop() {
    sendEvent("media_stop", null);
    state = STATE_STOP;
  }
  public void seek(double time) {
    sendEvent("media_seek", new String[] {"time=" + String.format("%.3f", time)});
  }
  public void dispatchEvent(String event, String args[]) {
    switch (event) {
      case "onplay": onplay(); break;
      case "onpause": onpause(); break;
      case "onstop": onstop(); break;
    }
    super.dispatchEvent(event, args);
  }
  private void onplay() {
    state = STATE_PLAY;
    onChanged(null);
  }
  private void onpause() {
    state = STATE_PAUSE;
    onChanged(null);
  }
  private void onstop() {
    state = STATE_STOP;
    onChanged(null);
  }
  public boolean isInited() {
    return inited;
  }
  public boolean isPlaying() {
    return state == STATE_PLAY;
  }
}
