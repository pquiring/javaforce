package service;

/** Config Service.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.23";
  public WebUIServer server;
  private KeyMgmt keys;
  private byte[] cameraicon;

  public void start() {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, 443, keys);
    try {
      cameraicon = this.getClass().getClassLoader().getResourceAsStream("camera.png").readAllBytes();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
  }

  private void initSecureWebKeys() {
    String keyfile = Paths.dataPath + "/jfdvr.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfdvr.sourceforge.net, O=server, OU=webserver, C=CA";
    if (new File(keyfile).exists()) {
      //load existing keys
      keys = new KeyMgmt();
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        keys.open(fis, password);
        fis.close();
      } catch (Exception e) {
        if (!keys.isValid()) {
          //generate random keys
          keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
        }
        JFLog.log(e);
      }
    } else {
      //generate random keys
      keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
    }
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

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    Label lbl;
    Row row, row2;
    Pad pad;
    int cnt;

    client.setProperty("view", "cameras");

    Panel panel = new Panel();
    SplitPanel split = new SplitPanel(SplitPanel.VERTICAL);
    Panel left = new Panel();
    Panel right = new Panel();
    split.setLeftComponent(left);
    split.setRightComponent(right);
    panel.add(split);
    //left side
    ListBox list = new ListBox();
    cnt = Config.current.cameras.length;
    for(int a=0;a<cnt;a++) {
      list.add("Camera:" + Config.current.cameras[a].name);
    }
    cnt = Config.current.groups.length;
    for(int a=0;a<cnt;a++) {
      list.add("Group:" + Config.current.groups[a].name);
    }
    left.add(list);

    //right side
    row = new Row();
    right.add(row);
    Label label = new Label("jfDVR/" + version);
    row.add(label);

    row = new Row();
    right.add(row);
    HTMLContainer html = new HTMLContainer("hr");
    html.setText("");
    html.setStyle("width", "100%");
    row.add(html);

    row = new Row();
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);
    right.add(row);

    row = new Row();
    right.add(row);
    Button b_new_camera = new Button("New Camera");
    row.add(b_new_camera);
    Button b_new_group = new Button("New Group");
    row.add(b_new_group);
    Button b_save = new Button("Save");
    row.add(b_save);

    Button b_delete = new Button("Delete");
    row.add(b_delete);

    Button b_help = new Button("Help");
    row.add(b_help);

    Button b_shutdown = new Button("Shutdown");
    if (System.getProperty("java.debug") != null) {
      row.add(b_shutdown);
    }

    row = new Row();
    right.add(row);
    InnerPanel camera_panel = new InnerPanel("Camera");
    row.add(camera_panel);

    row = new Row();
    camera_panel.add(row);
    Label camera_lbl_name = new Label("Name:");
    row.add(camera_lbl_name);
    TextField camera_name = new TextField("");
    row.add(camera_name);

    row = new Row();
    camera_panel.add(row);
    Label camera_lbl_url = new Label("URL:");
    row.add(camera_lbl_url);
    TextField camera_url = new TextField("");
    camera_url.setWidth(300);
    row.add(camera_url);
    Label camera_lbl_url_desc = new Label("(high quality : viewer, recording)");
    row.add(camera_lbl_url_desc);

    row = new Row();
    camera_panel.add(row);
    Label camera_lbl_url_low = new Label("URL:");
    row.add(camera_lbl_url_low);
    TextField camera_url_low = new TextField("");
    camera_url_low.setWidth(300);
    row.add(camera_url_low);
    Label camera_lbl_url_low_desc = new Label("(low quality : decoder, motion detection, preview)");
    row.add(camera_lbl_url_low_desc);

    row = new Row();
    camera_panel.add(row);
    CheckBox camera_enabled = new CheckBox("Enabled");
    camera_enabled.setSelected(true);
    row.add(camera_enabled);

    InnerPanel camera_video_panel = new InnerPanel("Video Options (rtsp)");
    camera_panel.add(camera_video_panel);

    row = new Row();
    camera_video_panel.add(row);
    CheckBox camera_record_motion = new CheckBox("Motion Recording");
    camera_record_motion.setSelected(true);
    row.add(camera_record_motion);

    row = new Row();
    camera_video_panel.add(row);
    lbl = new Label("Motion Threshold:");
    row.add(lbl);
    Slider camera_threshold = new Slider(Slider.HORIZONTAL, 0, 100, 15);
    row.add(camera_threshold);
    Label threshold_lbl = new Label("20");
    camera_threshold.addChangedListener((Component c) -> {
      threshold_lbl.setText(Integer.toString(camera_threshold.getPos()));
    });
    row.add(threshold_lbl);

    row = new Row();
    camera_video_panel.add(row);
    lbl = new Label("Motion Recording Off Delay (sec):");
    row.add(lbl);
    Slider camera_motion_off_delay = new Slider(Slider.HORIZONTAL, 0, 60, 5);
    row.add(camera_motion_off_delay);
    Label motion_off_delay_lbl = new Label("5");
    camera_motion_off_delay.addChangedListener((Component c) -> {
      motion_off_delay_lbl.setText(Integer.toString(camera_motion_off_delay.getPos()));
    });
    row.add(motion_off_delay_lbl);

    InnerPanel pic_panel = new InnerPanel("Picture Options (http)");
    camera_panel.add(pic_panel);

    row = new Row();
    pic_panel.add(row);
    lbl = new Label("Controller:");
    row.add(lbl);
    TextField controller = new TextField("");
    row.add(controller);

    row = new Row();
    pic_panel.add(row);
    lbl = new Label("Trigger Tag:");
    row.add(lbl);
    TextField tag_trigger = new TextField("");
    row.add(tag_trigger);

    row = new Row();
    pic_panel.add(row);
    lbl = new Label("Program Tag:");
    row.add(lbl);
    TextField tag_value = new TextField("");
    row.add(tag_value);
    lbl = new Label("(optional)");
    row.add(lbl);

    row = new Row();
    pic_panel.add(row);
    CheckBox pos_edge = new CheckBox("Pos Edge Trigger");
    pos_edge.setSelected(true);
    row.add(pos_edge);

    InnerPanel storage_panel = new InnerPanel("Storage");
    camera_panel.add(storage_panel);

    row = new Row();
    storage_panel.add(row);
    lbl = new Label("Max file size (MB) (10-4096):");
    row.add(lbl);
    TextField max_file_size = new TextField("1024");
    row.add(max_file_size);

    row = new Row();
    storage_panel.add(row);
    lbl = new Label("Max folder size (GB) (1-4096):");
    row.add(lbl);
    TextField max_folder_size = new TextField("100");
    row.add(max_folder_size);

    InnerPanel preview_panel = new InnerPanel("Preview");
    camera_panel.add(preview_panel);

    row = new Row();
    preview_panel.add(row);
    lbl = new Label("Motion Value:");
    row.add(lbl);
    ProgressBar motion_bar = new ProgressBar(ProgressBar.HORIZONTAL, 100, 10);
    row.add(motion_bar);
    Label motion_value = new Label("");
    row.add(motion_value);

    row = new Row();
    preview_panel.add(row);
    lbl = new Label("Low Quality Preview:");
    row.add(lbl);

    row = new Row();
    preview_panel.add(row);
    Image img = new Image((Resource)null);
    row.add(img);
    preview_panel.setVisible(false);

    row = new Row();
    right.add(row);
    InnerPanel group_panel = new InnerPanel("Group");
    right.add(group_panel);
    group_panel.setVisible(false);

    row = new Row();
    group_panel.add(row);
    Label group_lbl_name = new Label("Name:");
    row.add(group_lbl_name);
    TextField group_name = new TextField("");
    row.add(group_name);

    row = new Row();
    group_panel.add(row);

    InnerPanel group_cameras = new InnerPanel("Group Cameras");
    row.add(group_cameras);

    InlineBlock blk1 = new InlineBlock();
    InnerPanel ipanel1 = new InnerPanel("Available");
    ListBox group_list_avail = new ListBox();
    ipanel1.add(group_list_avail);
    blk1.add(ipanel1);
    row.add(blk1);

    InlineBlock blk2 = new InlineBlock();

    row2 = new Row();
    blk2.add(row2);
    pad = new Pad();
    pad.setHeight(25);
    row2.add(pad);

    row2 = new Row();
    blk2.add(row2);
    Button b_group_add_camera = new Button("Add >>");
    b_group_add_camera.setWidth(75);
    row2.add(b_group_add_camera);

    row2 = new Row();
    blk2.add(row2);
    Button b_group_remove_camera = new Button("<< Remove");
    b_group_remove_camera.setWidth(75);
    row2.add(b_group_remove_camera);

    row.add(blk2);

    InlineBlock blk3 = new InlineBlock();
    InnerPanel ipanel3 = new InnerPanel("Selected");
    ListBox group_list_selected = new ListBox();
    ipanel3.add(group_list_selected);
    blk3.add(ipanel3);
    row.add(blk3);

    InlineBlock blk4 = new InlineBlock();

    row2 = new Row();
    blk4.add(row2);
    pad = new Pad();
    pad.setHeight(25);
    row2.add(pad);

    row2 = new Row();
    blk4.add(row2);
    Button b_group_camera_move_up = new Button("Up");
    b_group_camera_move_up.setWidth(75);
    row2.add(b_group_camera_move_up);

    row2 = new Row();
    blk4.add(row2);
    Button b_group_camera_move_down = new Button("Down");
    b_group_camera_move_down.setWidth(75);
    row2.add(b_group_camera_move_down);

    row.add(blk4);

    list.addChangedListener((Component x) -> {
      errmsg.setText("");
      String opt = list.getSelectedItem();
      if (opt == null) return;
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);
      switch (opt_type) {
        case "Camera": {
          //select camera
          int camera_cnt = Config.current.cameras.length;
          Camera camera = null;
          for(int a=0;a<camera_cnt;a++) {
            if (Config.current.cameras[a].name.equals(opt_name)) {
              camera = Config.current.cameras[a];
              break;
            }
          }
          if (camera == null) break;
          camera_name.setText(camera.name);
          camera_url.setText(camera.url);
          camera_url_low.setText(camera.url_low);
          camera_enabled.setSelected(camera.enabled);
          camera_record_motion.setSelected(camera.record_motion);
          camera_threshold.setPos(camera.record_motion_threshold);
          threshold_lbl.setText(Integer.toString(camera.record_motion_threshold));
          if (camera.record_motion_after < 0) camera.record_motion_after = 0;
          if (camera.record_motion_after > 60) camera.record_motion_after = 60;
          camera_motion_off_delay.setPos(camera.record_motion_after);
          motion_off_delay_lbl.setText(Integer.toString(camera.record_motion_after));
          max_file_size.setText(Integer.toString(camera.max_file_size));
          max_folder_size.setText(Integer.toString(camera.max_folder_size));
          controller.setText(camera.controller);
          tag_trigger.setText(camera.tag_trigger);
          tag_value.setText(camera.tag_value);
          pos_edge.setSelected(camera.pos_edge);
          stopTimer(client);
          client.setProperty("camera", camera);
          Camera camera_cap = camera;
          Timer timer = new Timer();
          timer.schedule(new TimerTask() {
            public void run() {
              motion_bar.setValue(camera_cap.motion_value);
              motion_value.setText(Integer.toString((int)camera_cap.motion_value));
              img.refresh();
            }
          }, 500, 500);
          client.setProperty("timer", timer);
          preview_panel.setVisible(true);
          camera.viewing = true;
          camera.update_preview = true;
          camera_panel.setVisible(true);
          group_panel.setVisible(false);
          client.setProperty("view", "cameras");
          break;
        }
        case "Group": {
          //select group
          int group_cnt = Config.current.groups.length;
          Group group = null;
          for(int a=0;a<group_cnt;a++) {
            if (Config.current.groups[a].name.equals(opt_name)) {
              group = Config.current.groups[a];
              break;
            }
          }
          if (group == null) break;
          group_name.setText(group.name);
          update_group_lists(group, group_list_avail, group_list_selected);
          camera_panel.setVisible(false);
          group_panel.setVisible(true);
          client.setProperty("view", "groups");
          break;
        }
      }
    });
    b_new_camera.addClickListener((MouseEvent e, Component button) -> {
      list.setSelectedIndex(-1);
      camera_name.setText("");
      camera_url.setText("");
      camera_url_low.setText("");
      camera_enabled.setSelected(true);
      controller.setText("");
      tag_trigger.setText("");
      tag_value.setText("");
      stopTimer(client);
      motion_bar.setValue(0);
      preview_panel.setVisible(false);
      camera_panel.setVisible(true);
      group_panel.setVisible(false);
      client.setProperty("view", "cameras");
    });
    b_new_group.addClickListener((MouseEvent e, Component button) -> {
      list.setSelectedIndex(-1);
      group_name.setText("");
      stopTimer(client);
      preview_panel.setVisible(false);
      camera_panel.setVisible(false);
      group_panel.setVisible(true);
      update_group_lists(new Group(), group_list_avail, group_list_selected);
      client.setProperty("view", "groups");
      client.setProperty("new_group", new Group());
    });
    b_save.addClickListener((MouseEvent e, Component button) -> {
      errmsg.setText("");
      String view = (String)client.getProperty("view");
      switch (view) {
        case "cameras": {
          //save camera
          String _name = camera_name.getText();
          _name = cleanName(_name);
          camera_name.setText(_name);
          if (_name.length() == 0 || _name.equals("new")) {
            errmsg.setText("Invalid Name");
            return;
          }
          String _url = camera_url.getText();
          if (!validURL(_url)) {
            errmsg.setText("Invalid URL (high quality)");
            return;
          }
          String _url_low = camera_url_low.getText();
          if (!validURL(_url_low)) {
            errmsg.setText("Invalid URL (low quality)");
            return;
          }
          boolean reload = false;
          Camera camera = null;
          if (list.getSelectedIndex() == -1) {
            //create new camera
            int ccnt = Config.current.cameras.length;
            for(int a=0;a<ccnt;a++) {
              if (Config.current.cameras[a].name.equals(_name)) {
                errmsg.setText("That name already exists");
                return;
              }
            }
            camera = new Camera();
            int listIdx = Config.current.cameras.length;
            list.add(listIdx, "Camera:" + _name);
            Config.current.addCamera(camera);
            list.setSelectedIndex(listIdx);
          } else {
            //update existing camera
            String opt = list.getSelectedItem();
            if (opt == null) return;
            int idx = opt.indexOf(':');
            String opt_type = opt.substring(0, idx);
            String opt_name = opt.substring(idx+1);
            //update existing camera
            int camera_cnt = Config.current.cameras.length;
            for(int a=0;a<camera_cnt;a++) {
              if (Config.current.cameras[a].name.equals(opt_name)) {
                camera = Config.current.cameras[a];
                break;
              }
            }
            if (camera == null) break;
            //check if name/url are the same
            if (camera.name.equals(_name) && camera.url.equals(_url)) {
              reload = true;
            } else {
              DVRService.dvrService.stopCamera(camera);
            }
            Config.current.renamedCamera(camera.name, _name);
            int listIdx = list.getSelectedIndex();
            list.remove(listIdx);
            list.add(listIdx, "Camera:" + _name);
            list.setSelectedIndex(listIdx);
          }
          camera.name = _name;
          camera.url = _url;
          camera.url_low = _url_low;
          camera.enabled = camera_enabled.isSelected();
          camera.record_motion = camera_record_motion.isSelected();
          camera.record_motion_threshold = camera_threshold.getPos();
          threshold_lbl.setText(Integer.toString(camera.record_motion_threshold));
          camera.record_motion_after = camera_motion_off_delay.getPos();
          motion_off_delay_lbl.setText(Integer.toString(camera.record_motion_after));
          camera.max_file_size = Integer.valueOf(max_file_size.getText());
          if (camera.max_file_size < 5) camera.max_file_size = 5;
          if (camera.max_file_size > 4096) camera.max_file_size = 4096;
          camera.max_folder_size = Integer.valueOf(max_folder_size.getText());
          if (camera.max_folder_size < 0) camera.max_folder_size = 1;
          if (camera.max_folder_size > 4096) camera.max_folder_size = 4096;
          camera.controller = controller.getText();
          camera.tag_trigger = tag_trigger.getText();
          camera.tag_value = tag_value.getText();
          camera.pos_edge = pos_edge.isSelected();
          if (reload) {
            if (camera.enabled) {
              DVRService.dvrService.reloadCamera(camera);
            } else {
              DVRService.dvrService.stopCamera(camera);
            }
          } else {
            if (camera.enabled) {
              DVRService.dvrService.startCamera(camera);
            }
          }
          Config.current.save();
          stopTimer(client);
          break;
        }
        case "groups": {
          //save group
          String _name = group_name.getText();
          _name = cleanName(_name);
          group_name.setText(_name);
          if (_name.length() == 0 || _name.equals("new")) {
            errmsg.setText("Invalid Name");
            return;
          }

          Group group = null;
          if (list.getSelectedIndex() == -1) {
            //create new group
            int ccnt = Config.current.groups.length;
            for(int a=0;a<ccnt;a++) {
              if (Config.current.groups[a].name.equals(_name)) {
                errmsg.setText("That name already exists");
                return;
              }
            }
            group = (Group)client.getProperty("new_group");
            if (group == null) break;
            int listIdx = list.count();
            list.add("Group:" + _name);
            list.setSelectedIndex(listIdx);
            Config.current.addGroup(group);
            client.setProperty("new_group", null);
          } else {
            //update existing group
            String opt = list.getSelectedItem();
            if (opt == null) return;
            int idx = opt.indexOf(':');
            String opt_type = opt.substring(0, idx);
            String opt_name = opt.substring(idx+1);
            //update existing group
            int group_cnt = Config.current.groups.length;
            for(int a=0;a<group_cnt;a++) {
              if (Config.current.groups[a].name.equals(opt_name)) {
                group = Config.current.groups[a];
                break;
              }
            }
            if (group == null) break;
            int listIdx = list.getSelectedIndex();
            list.remove(listIdx);
            list.add(listIdx, "Group:" + _name);
            list.setSelectedIndex(listIdx);
          }
          group.name = _name;
          update_group_lists(group, group_list_avail, group_list_selected);
          break;
        }
      }
      Config.save();
    });

    b_group_add_camera.addClickListener((MouseEvent e, Component button) -> {
      String opt = list.getSelectedItem();
      if (opt == null) opt = "group:new";
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);

      Group group = null;

      int group_cnt = Config.current.groups.length;
      for(int a=0;a<group_cnt;a++) {
        if (Config.current.groups[a].name.equals(opt_name)) {
          group = Config.current.groups[a];
          break;
        }
      }
      if (group == null) {
        group = (Group)client.getProperty("new_group");
      }

      String camera = group_list_avail.getSelectedItem();
      if (camera == null) return;

      group.add(camera);
      update_group_lists(group, group_list_avail, group_list_selected);
    });

    b_group_remove_camera.addClickListener((MouseEvent e, Component button) -> {
      String opt = list.getSelectedItem();
      if (opt == null) return;
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);

      Group group = null;

      int group_cnt = Config.current.groups.length;
      for(int a=0;a<group_cnt;a++) {
        if (Config.current.groups[a].name.equals(opt_name)) {
          group = Config.current.groups[a];
          break;
        }
      }
      if (group == null) {
        group = (Group)client.getProperty("new_group");
      }

      String camera = group_list_selected.getSelectedItem();
      if (camera == null) return;

      group.remove(camera);
      update_group_lists(group, group_list_avail, group_list_selected);
    });

    b_group_camera_move_up.addClickListener((MouseEvent e, Component button) -> {
      String opt = list.getSelectedItem();
      if (opt == null) opt = "group:new";
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);

      Group group = null;

      int group_cnt = Config.current.groups.length;
      for(int a=0;a<group_cnt;a++) {
        if (Config.current.groups[a].name.equals(opt_name)) {
          group = Config.current.groups[a];
          break;
        }
      }
      if (group == null) {
        group = (Group)client.getProperty("new_group");
      }

      int camidx = group_list_selected.getSelectedIndex();
      if (camidx == 0) return;

      group.moveUp(camidx);

      update_group_lists(group, group_list_avail, group_list_selected);
    });

    b_group_camera_move_down.addClickListener((MouseEvent e, Component button) -> {
      String opt = list.getSelectedItem();
      if (opt == null) opt = "group:new";
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);

      Group group = null;

      int group_cnt = Config.current.groups.length;
      for(int a=0;a<group_cnt;a++) {
        if (Config.current.groups[a].name.equals(opt_name)) {
          group = Config.current.groups[a];
          break;
        }
      }
      if (group == null) {
        group = (Group)client.getProperty("new_group");
      }

      int camidx = group_list_selected.getSelectedIndex();
      if (camidx == group.cameras.length - 1) return;

      group.moveDown(camidx);

      update_group_lists(group, group_list_avail, group_list_selected);
    });

    PopupPanel popup = new PopupPanel("Confirm");
    popup.setModal(true);
    Label popup_label = new Label("Are you sure?");
    popup.add(popup_label);
    Button popup_b_delete = new Button("Delete");
    popup.add(popup_b_delete);
    popup_b_delete.addClickListener((MouseEvent e, Component button) -> {
      errmsg.setText("");
      String view = (String)client.getProperty("view");
      int selidx = list.getSelectedIndex();
      String opt = list.getSelectedItem();
      if (opt == null) return;
      int idx = opt.indexOf(':');
      String opt_type = opt.substring(0, idx);
      String opt_name = opt.substring(idx+1);
      switch (view) {
        case "cameras":
          //delete camera
          Camera camera = null;
          int camera_cnt = Config.current.cameras.length;
          for(int a=0;a<camera_cnt;a++) {
            if (Config.current.cameras[a].name.equals(opt_name)) {
              camera = Config.current.cameras[a];
              break;
            }
          }
          if (camera == null) break;
          DVRService.dvrService.stopCamera(camera);
          Config.current.removeCamera(camera);
          Config.current.save();
          list.remove(selidx);
          popup.setVisible(false);
          stopTimer(client);
          break;
        case "groups":
          //delete group
          int group_cnt = Config.current.groups.length;
          Group group = null;
          for(int a=0;a<group_cnt;a++) {
            if (Config.current.groups[a].name.equals(opt_name)) {
              group = Config.current.groups[a];
              break;
            }
          }
          if (group == null) break;
          Config.current.removeGroup(group);
          Config.current.save();
          list.remove(selidx);
          popup.setVisible(false);
          stopTimer(client);
          break;
      }
      Config.save();
    });
    Button popup_b_cancel = new Button("Cancel");
    popup.add(popup_b_cancel);
    popup_b_cancel.addClickListener((MouseEvent e, Component button) -> {
      popup.setVisible(false);
    });
    b_delete.addClickListener((MouseEvent e, Component button) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      popup.setVisible(true);
    });
    b_help.addClickListener((MouseEvent e, Component button) -> {
      client.openURL("http://jfdvr.sourceforge.net/help.html");
    });
    if (System.getProperty("java.debug") != null) {
      b_shutdown.addClickListener((MouseEvent e, Component button) -> {
        DVRService.serviceStop();
      });
    }

    panel.add(popup);
    split.setDividerPosition(150);

    return panel;
  }

  private void update_group_lists(Group group, ListBox avail, ListBox selected) {
    while (avail.count() > 0) {
      avail.remove(0);
    }
    while (selected.count() > 0) {
      selected.remove(0);
    }
    {
      //update selected
      int cnt = group.cameras.length;
      for(int a=0;a<cnt;a++) {
        String camera = group.cameras[a];
        selected.add(camera);
      }
    }
    {
      //update available
      int cnt = Config.current.cameras.length;
      for(int a=0;a<cnt;a++) {
        Camera camera = Config.current.cameras[a];
        if (!group.contains(camera.name)) {
          avail.add(camera.name);
        }
      }
    }
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
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
    byte[] result = camera.preview;
    if (result == null) {
      result = cameraicon;
    }
    return result;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
    stopTimer(client);
  }
}
