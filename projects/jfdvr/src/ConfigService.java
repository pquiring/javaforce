/**
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.media.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.2";
  public WebUIServer server;
  public void start() {
    server = new WebUIServer();
    server.start(this, 34002, false);
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

  private String cleanURL(String url) {
    if (!url.startsWith("rtsp://")) return "rtsp://" + url;
    //TODO : more validation
    return url;
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
    List list = new List();
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

    row = new Row();
    right.add(row);
    CheckBox record_motion = new CheckBox("Motion Recording");
    record_motion.setSelected(true);
    row.add(record_motion);

    row = new Row();
    right.add(row);
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
    right.add(row);
    lbl = new Label("Motion Recording Off Delay (sec):");
    row.add(lbl);
    Slider motion_off_delay = new Slider(Slider.HORIZONTAL, 0, 60, 5);
    row.add(motion_off_delay);
    Label motion_off_delay_lbl = new Label("5");
    motion_off_delay.addChangedListener((Component c) -> {
      motion_off_delay_lbl.setText(Integer.toString(motion_off_delay.getPos()));
    });
    row.add(motion_off_delay_lbl);

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
    });
    b_new.addClickListener((MouseEvent e, Component button) -> {
      list.setSelectedIndex(-1);
      name.setText("");
      url.setText("");
    });
    b_save.addClickListener((MouseEvent e, Component button) -> {
      int idx = list.getSelectedIndex();
      String _name = name.getText();
      _name = cleanName(_name);
      name.setText(_name);
      if (_name.length() == 0) return;
      String _url = url.getText();
      _url = cleanURL(_url);
      url.setText(_url);
      if (_url.length() == 0) return;
      Camera camera;
      if (idx == -1) {
        //create new camera
        int ccnt = Config.current.cameras.length;
        for(int a=0;a<ccnt;a++) {
          if (Config.current.cameras[a].name.equals(_name)) {
            //TODO : signal error
            return;
          }
        }
        camera = new Camera();
        Config.current.addCamera(camera);
        list.add(_name);
      } else {
        //update existing camera
        camera = Config.current.cameras[idx];
        DVRService.dvrService.stopCamera(camera);
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
      DVRService.dvrService.startCamera(camera);
      Config.current.save();
      button.client.refresh();  //list not working yet
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

    panel.add(popup);
    split.setDividerPosition(150);

    return panel;
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
