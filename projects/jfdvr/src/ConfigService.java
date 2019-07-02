/**
 *
 * @author pquiring
 */

import javaforce.JFLog;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigService implements WebUIHandler {
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

  public Panel getRootPanel(WebUIClient client) {
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
    Button b_new = new Button("New");
    row.add(b_new);
    b_new.setName("new");
    Button b_save = new Button("Save");
    row.add(b_save);
    b_save.setName("save");
    //TODO : delete (need confirm dialog)

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

    CheckBox record_motion = new CheckBox("Motion Recording");
    record_motion.setSelected(true);
    row = new Row();
    right.add(row);
    row.add(record_motion);
    lbl = new Label("Threshold:");
    row.add(lbl);
    Slider threshold = new Slider(Slider.HORIZONTAL, 0, 100, 20);
    row.add(threshold);
    lbl = new Label("Recording Off Delay (sec):");
    row.add(lbl);
    Slider motion_off_delay = new Slider(Slider.HORIZONTAL, 0, 60, 2);
    row.add(motion_off_delay);

    row = new Row();
    right.add(row);
    lbl = new Label("Max file size (MB) (10-4096):");
    row.add(lbl);
    TextField max_file_size = new TextField("1024");
    row.add(max_file_size);
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
      if (camera.record_motion_after < 0) camera.record_motion_after = 0;
      if (camera.record_motion_after > 60) camera.record_motion_after = 60;
      motion_off_delay.setPos(camera.record_motion_after);
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
      if (_name.length() == 0) return;
      String _url = url.getText();
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
      camera.record_motion_after = motion_off_delay.getPos();
      camera.max_file_size = Integer.valueOf(max_file_size.getText());
      if (camera.max_file_size < 10) camera.max_file_size = 10;
      if (camera.max_file_size > 4096) camera.max_file_size = 4096;
      camera.max_folder_size = Integer.valueOf(max_folder_size.getText());
      if (camera.max_folder_size < 0) camera.max_folder_size = 1;
      if (camera.max_folder_size > 4096) camera.max_folder_size = 4096;
      if (idx == -1) {
        DVRService.dvrService.addCamera(camera);
      } else {
        DVRService.dvrService.startCamera(camera);
        list.update(idx, _name);
      }
      Config.current.save();
    });
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
