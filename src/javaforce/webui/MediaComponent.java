package javaforce.webui;

import javaforce.webui.event.*;

/** Media : Base class for Audio and Video elements.
 *
 * @author pquiring
 */

public class MediaComponent extends Container {
  private String src;
  private int state;

  private Button button_play;

  public static final int STATE_UNINIT = 0;
  public static final int STATE_STOP = 1;
  public static final int STATE_PLAY = 2;
  public static final int STATE_PAUSE = 3;
  public static final int STATE_CAPTURE = 4;

  public MediaComponent(String tag) {
    setTag(tag);
    addEvent("onplay", "media_onplay(this);");
    addEvent("onstop", "media_onstop(this);");
    addEvent("onpause", "media_onpause(this);");
    addEvent("onseeking", "console.log('Media.seeking:readyState=' + this.readyState);");
    addEvent("onstaled", "console.log('Media.staled:readyState=' + this.readyState);");
    addEvent("onsuspend", "console.log('Media.suspend:readyState=' + this.readyState);");
    addEvent("onwaiting", "console.log('Media.waiting:readyState=' + this.readyState);");
    setStyle("position", "absolute");
    setStyle("left", "0px");
    setStyle("top", "0px");
    button_play = new Button("Play");
    button_play.addClickListener(new Click() {
      public void onClick(MouseEvent me, Component comp) {
        MediaComponent.this.dispatchEvent("action", null);
      }
    });
    add(button_play);
  }
  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<div style='");
    if (width != 0) {
      html.append(" width:" + width + "px;");
    }
    if (height != 0) {
      html.append(" height:" + height + "px;");
    }
    html.append("'>");
    html.append("<div id='" + getID() + "s1' style='display: inline-block; position: relative;");
    if (width != 0) {
      html.append(" width:" + width + "px;");
    }
    if (height != 0) {
      html.append(" height:" + height + "px;");
    }
    html.append("'>");
    html.append("<");
    html.append(getTag());
    html.append(getAttrs());
    if (src != null) {
      html.append(" src='" + src + "'");
    }
    html.append(">");
    html.append("</");
    html.append(getTag());
    html.append(">");
    html.append("<table id='" + getID() + "s2' style='position:absolute; left:0px; top:0px; width: 100%; height: 100%; background-color: grey;'>");
    html.append("<tr height=50%></tr>");
    html.append("<tr>");
    html.append("<td width=50%></td>");
    html.append("<td>");
    html.append(button_play.html());
    html.append("</td>");
    html.append("<td width=50%></td>");
    html.append("</tr>");
    html.append("<tr height=50%></tr>");
    html.append("</table>");
    html.append("</div>");
    html.append("</div>");
    return html.toString();
  }
  public void setSource(String url) {
    src = url;
  }
  public void setLiveSource(String codecs) {
    sendEvent("media_set_live_source", new String[] {"codecs=" + codecs});
  }
  public void setCapture(boolean doVideo, boolean doAudio) {
    sendEvent("media_set_capture", new String[] {"audio=" + doAudio, "video=" + doVideo});
    state = STATE_CAPTURE;
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
  public void onEvent(String event, String[] args) {
    switch (event) {
      case "onplay": onplay(); break;
      case "onpause": onpause(); break;
      case "onstop": onstop(); break;
    }
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
  public boolean isPlaying() {
    return state == STATE_PLAY;
  }
}
