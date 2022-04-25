/**
 *
 * @author pquiring
 */

import java.util.Timer;
import java.util.TimerTask;

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.media.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.12";
  public WebUIServer server;
  public void start() {
    server = new WebUIServer();
    server.start(this, 80, false);
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
  }

  private String cleanName(String name) {
    char ca[] = name.toCharArray();
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      if (Character.isAlphabetic(ch)) {
        sb.append(ch);
      } else if (Character.isDigit(ch)) {
        sb.append(ch);
      } else {
        switch (ch) {
          case '-':
          case '_':
            sb.append(ch);
        }
      }
    }
    return sb.toString();
  }

  private boolean validURL(String url) {
    if (url.startsWith("rtsp://")) return true;
    if (url.startsWith("http://")) return true;
    return false;
  }

  private void stopTimer(WebUIClient client) {
    Timer timer = (Timer)client.getProperty("timer");
    if (timer != null) {
      timer.cancel();
      client.setProperty("timer", null);
    }
    Camera camera = (Camera)client.getProperty("camera");
    if (camera != null) {
      camera.viewing = false;
      camera.preview = null;
    }
  }

  public Panel getRootPanel(WebUIClient client) {
    if (!MediaCoder.loaded) {
      MediaCoder.init();
    }
    if (!MediaCoder.loaded) {
      return MediaCoder.downloadWebUI();
    }
    Label lbl;
    Row row;

    Panel panel = new Panel();
    SplitPanel split = new SplitPanel(SplitPanel.VERTICAL);
    Panel left = new Panel();
    Panel right = new Panel();
    split.setLeftComponent(left);
    split.setRightComponent(right);
    panel.add(split);
    //left side
    ListBox list = new ListBox();
    list.setName("list");
    int cnt = Config.current.cameras.length;
    for(int a=0;a<cnt;a++) {
      list.add(Config.current.cameras[a].name);
    }
    left.add(list);

    //right side
    row = new Row();
    right.add(row);
    Label label = new Label("jfDVR/" + version);
    row.add(label);

    row = new Row();
    right.add(row);
    HTML html = new HTML("hr");
    html.setText("");
    html.setStyle("width", "100%");
    row.add(html);

    row = new Row();
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    right.add(row);

    row = new Row();
    right.add(row);
    Button b_new = new Button("New");
    row.add(b_new);
    b_new.setName("new");
    Button b_save = new Button("Save");
    row.add(b_save);
    b_save.setName("save");

    Button b_delete = new Button("Delete");
    row.add(b_delete);
    b_delete.setName("delete");

    Button b_help = new Button("Help");
    row.add(b_help);
    b_help.setName("help");

    row = new Row();
    right.add(row);
    Label lname = new Label("Name:");
    row.add(lname);
    TextField name = new TextField("");
    name.setName("name");
    row.add(name);

    row = new Row();
    right.add(row);
    Label lurl = new Label("URL:");
    row.add(lurl);
    TextField url = new TextField("");
    name.setName("url");
    row.add(url);

    InnerPanel video = new InnerPanel("Video Options (rtsp)");
    right.add(video);

    row = new Row();
    video.add(row);
    CheckBox record_motion = new CheckBox("Motion Recording");
    record_motion.setSelected(true);
    row.add(record_motion);

    row = new Row();
    video.add(row);
    lbl = new Label("Motion Threshold:");
    row.add(lbl);
    Slider threshold = new Slider(Slider.HORIZONTAL, 0, 100, 15);
    row.add(threshold);
    Label threshold_lbl = new Label("20");
    threshold.addChangedListener((Component c) -> {
      threshold_lbl.setText(Integer.toString(threshold.getPos()));
    });
    row.add(threshold_lbl);

    row = new Row();
    video.add(row);
    lbl = new Label("Motion Recording Off Delay (sec):");
    row.add(lbl);
    Slider motion_off_delay = new Slider(Slider.HORIZONTAL, 0, 60, 5);
    row.add(motion_off_delay);
    Label motion_off_delay_lbl = new Label("5");
    motion_off_delay.addChangedListener((Component c) -> {
      motion_off_delay_lbl.setText(Integer.toString(motion_off_delay.getPos()));
    });
    row.add(motion_off_delay_lbl);

    InnerPanel pic = new InnerPanel("Picture Options (http)");
    right.add(pic);

    row = new Row();
    pic.add(row);
    lbl = new Label("Controller:");
    row.add(lbl);
    TextField controller = new TextField("");
    row.add(controller);

    row = new Row();
    pic.add(row);
    lbl = new Label("Trigger Tag:");
    row.add(lbl);
    TextField tag_trigger = new TextField("");
    row.add(tag_trigger);

    row = new Row();
    pic.add(row);
    lbl = new Label("Program Tag:");
    row.add(lbl);
    TextField tag_value = new TextField("");
    row.add(tag_value);
    lbl = new Label("(optional)");
    row.add(lbl);

    row = new Row();
    pic.add(row);
    CheckBox pos_edge = new CheckBox("Pos Edge Trigger");
    pos_edge.setSelected(true);
    row.add(pos_edge);

    row = new Row();
    right.add(row);
    lbl = new Label("Max file size (MB) (10-4096):");
    row.add(lbl);
    TextField max_file_size = new TextField("1024");
    row.add(max_file_size);

    row = new Row();
    right.add(row);
    lbl = new Label("Max folder size (GB) (1-4096):");
    row.add(lbl);
    TextField max_folder_size = new TextField("100");
    row.add(max_folder_size);

    Column preview_panel = new Column();
    right.add(preview_panel);

    row = new Row();
    preview_panel.add(row);
    lbl = new Label("Motion Value:");
    row.add(lbl);
    ProgressBar bar = new ProgressBar(ProgressBar.HORIZONTAL, 100, 10);
    row.add(bar);
    Label motion_value = new Label("");
    row.add(motion_value);

    row = new Row();
    preview_panel.add(row);
    lbl = new Label("Low Quality Preview:");
    row.add(lbl);

    row = new Row();
    preview_panel.add(row);
    Image img = new Image(null);
    row.add(img);
    preview_panel.setVisible(false);

    list.addChangedListener((Component x) -> {
      int idx = list.getSelectedIndex();
      JFLog.log("list:idx=" + idx);
      if (idx < 0 || idx >= Config.current.cameras.length) return;
      Camera camera = Config.current.cameras[idx];
      name.setText(camera.name);
      url.setText(camera.url);
      record_motion.setSelected(camera.record_motion);
      threshold.setPos(camera.record_motion_threshold);
      threshold_lbl.setText(Integer.toString(camera.record_motion_threshold));
      if (camera.record_motion_after < 0) camera.record_motion_after = 0;
      if (camera.record_motion_after > 60) camera.record_motion_after = 60;
      motion_off_delay.setPos(camera.record_motion_after);
      motion_off_delay_lbl.setText(Integer.toString(camera.record_motion_after));
      max_file_size.setText(Integer.toString(camera.max_file_size));
      max_folder_size.setText(Integer.toString(camera.max_folder_size));
      controller.setText(camera.controller);
      tag_trigger.setText(camera.tag_trigger);
      tag_value.setText(camera.tag_value);
      pos_edge.setSelected(camera.pos_edge);
      stopTimer(client);
      client.setProperty("camera", camera);
      Timer timer = new Timer();
      timer.schedule(new TimerTask() {
        public void run() {
          bar.setValue(camera.motion_value);
          motion_value.setText(Integer.toString((int)camera.motion_value));
          img.refresh();
        }
      }, 500, 500);
      client.setProperty("timer", timer);
      preview_panel.setVisible(true);
      camera.viewing = true;
      camera.update_preview = true;
    });
    b_new.addClickListener((MouseEvent e, Component button) -> {
      list.setSelectedIndex(-1);
      name.setText("");
      url.setText("");
      controller.setText("");
      tag_trigger.setText("");
      tag_value.setText("");
      stopTimer(client);
      bar.setValue(0);
      preview_panel.setVisible(false);
    });
    b_save.addClickListener((MouseEvent e, Component button) -> {
      int idx = list.getSelectedIndex();
      String _name = name.getText();
      _name = cleanName(_name);
      name.setText(_name);
      if (_name.length() == 0) {
        errmsg.setText("Invalid Name");
        return;
      }
      String _url = url.getText();
      if (!validURL(_url)) {
        errmsg.setText("Invalid URL");
        return;
      }
      Camera camera;
      boolean reload = false;
      if (idx == -1) {
        //create new camera
        int ccnt = Config.current.cameras.length;
        for(int a=0;a<ccnt;a++) {
          if (Config.current.cameras[a].name.equals(_name)) {
            errmsg.setText("That name already exists");
            return;
          }
        }
        camera = new Camera();
        Config.current.addCamera(camera);
        list.add(_name);
      } else {
        //update existing camera
        camera = Config.current.cameras[idx];
        //check if name/url are the same
        if (camera.name.equals(_name) && camera.url.equals(_url)) {
          reload = true;
        } else {
          DVRService.dvrService.stopCamera(camera);
        }
      }
      camera.name = _name;
      camera.url = _url;
      camera.record_motion = record_motion.isSelected();
      camera.record_motion_threshold = threshold.getPos();
      threshold_lbl.setText(Integer.toString(camera.record_motion_threshold));
      camera.record_motion_after = motion_off_delay.getPos();
      motion_off_delay_lbl.setText(Integer.toString(camera.record_motion_after));
      camera.max_file_size = Integer.valueOf(max_file_size.getText());
      if (camera.max_file_size < 10) camera.max_file_size = 10;
      if (camera.max_file_size > 4096) camera.max_file_size = 4096;
      camera.max_folder_size = Integer.valueOf(max_folder_size.getText());
      if (camera.max_folder_size < 0) camera.max_folder_size = 1;
      if (camera.max_folder_size > 4096) camera.max_folder_size = 4096;
      camera.controller = controller.getText();
      camera.tag_trigger = tag_trigger.getText();
      camera.tag_value = tag_value.getText();
      camera.pos_edge = pos_edge.isSelected();
      if (!reload) {
        DVRService.dvrService.startCamera(camera);
      } else {
        DVRService.dvrService.reloadCamera(camera);
      }
      Config.current.save();
      button.client.refresh();  //list not working yet
      stopTimer(client);
    });
    PopupPanel popup = new PopupPanel("Confirm");
    popup.setName("popup");
    popup.setModal(true);
    Label popup_label = new Label("Are you sure?");
    popup.add(popup_label);
    Button popup_b_delete = new Button("Delete");
    popup.add(popup_b_delete);
    popup_b_delete.addClickListener((MouseEvent e, Component button) -> {
      int idx = list.getSelectedIndex();
      if (idx < 0 || idx >= Config.current.cameras.length) return;
      Camera camera = Config.current.cameras[idx];
      DVRService.dvrService.stopCamera(camera);
      Config.current.removeCamera(camera);
      Config.current.save();
      list.remove(idx);  //not working yet
      popup.setVisible(false);
      button.client.refresh();  //list not working yet
      stopTimer(client);
    });
    Button popup_b_cancel = new Button("Cancel");
    popup.add(popup_b_cancel);
    popup_b_cancel.addClickListener((MouseEvent e, Component button) -> {
      popup.setVisible(false);
    });
    b_delete.addClickListener((MouseEvent e, Component button) -> {
      int idx = list.getSelectedIndex();
      if (idx < 0 || idx >= Config.current.cameras.length) return;
      popup.setVisible(true);
    });
    b_help.addClickListener((MouseEvent e, Component button) -> {
      client.openURL("http://jfdvr.sourceforge.net/help.html");
    });

    panel.add(popup);
    split.setDividerPosition(150);

    return panel;
  }

  public byte[] getResource(String url) {
    //url = /user/hash/component_id/count
    String pts[] = url.split("/");
    String hash = pts[2];
    WebUIClient client = server.getClient(hash);
    if (client == null) {
      JFLog.log("ConfigServer.getResouce() : WebUIClient not found for hash:" + hash);
      return null;
    }
    Camera camera = (Camera)client.getProperty("camera");
    if (camera == null) {
      JFLog.log("ConfigServer.getResouce() : camera == null");
      return null;
    }
    camera.update_preview = true;
    return camera.preview;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
    stopTimer(client);
  }
}
