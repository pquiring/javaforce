package javaforce.webui;

/** Upload Button
 *
 * @author pquiring
 */

import javaforce.webui.tasks.*;

public class UploadButton extends Button {

  public UploadButton(String text) {
    super(text);
  }
  public UploadButton(Resource img) {
    super(img);
  }
  public UploadButton(Icon icon) {
    super(icon);
  }
  public UploadButton(Resource img, String text) {
    super(img, text);
  }
  public UploadButton(Icon icon, String text) {
    super(icon, text);
  }

  public void init() {
    super.init();
    addEvent("onchange", "onchangeUpload(event, this, '" + id + "');");
    setStyle("display", "none");
  }

  public String getUploadFolder() {
    return client.getUploadFolder();
  }

  public void setUploadFolder(String folder) {
    client.setUploadFolder(folder);
  }

  public Status getUploadStatus() {
    return client.getUploadStatus();
  }

  public void setUploadStatus(Status status) {
    client.setUploadStatus(status);
  }

  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<form method='post' id='f" + id + "' enctype='multipart/form-data' target='upload'>");
    html.append("<input type='hidden' id='s" + id + "' name='size'>");
    html.append("<input type='hidden' name='client' value='" + client.hash + "'>");
    html.append("<input type='file' name='file' " + getAttrs() + ">");  //'multiple' not supported yet
    html.append("<label for='" + id + "' class='upload'>");
    if (img != null) {
      html.append(img.html());
    }
    if (icon != null) {
      html.append(icon.html());
    }
    if (text != null) {
      html.append(text);
    }
    html.append("</label>");
    html.append("</form>");
    return html.toString();
  }
}
