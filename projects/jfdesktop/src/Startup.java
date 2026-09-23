import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;
import javaforce.linux.wl.*;
import javaforce.api.linux.*;
import static javaforce.linux.Linux.*;

/** jfDesktop startup.
 *
 * Created : July 10, 2012
 *
 * @author pquiring
 */

public class Startup implements ShellProcessListener, WLNotify {
  public static Startup instance;

  public static boolean use_proxy = true;

  private Properties props;
  private boolean is_wayland = false;
  private boolean is_nested = false;
  private String window_mgr = "openbox";
  private ShellProcess window_mgr_process;
  private Wayland wayland;
  private String user;
  private WLProxy proxy;

  private int LOG_DEFAULT = 0;
  private int LOG_DISPLAY = 1;

  public int taskbar_height = 57;

  public void load_config() {
    props = Linux.getJavaForceProperties();
    is_wayland = getProperty("wayland").equals("true");
    if (is_wayland) {
      is_nested = getProperty("nested_compositor").equals("true");
      window_mgr = getProperty("window_manager");
      if (window_mgr.length() == 0) {
        window_mgr = "labwc";
      }
      JFLog.log("wayland:window_manager=" + window_mgr);
    }
  }

  private boolean tty = false;

  public static void main(String args[]) {
    new Startup().run();
  }

  public void run() {
    instance = this;
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop-system.log", true);
    JFLog.init(LOG_DISPLAY, JF.getUserPath() + "/.jfdesktop-display.log", true);
    JFLog.log("jfDesktop:Startup");
    log_env();
    log_runtime_dir();
    user = System.getenv("USER");
    Linux.init();
    load_config();
    if (tty) {
      int sid = LinuxAPI.getInstance().getSID();
      JFLog.log("old sid=" + sid);
      LinuxAPI.getInstance().setSID();
      sid = LinuxAPI.getInstance().getSID();
      JFLog.log("new sid=" + sid);
      LinuxAPI.getInstance().ttyTakeOwnership();
    }
    try {
      if (!is_wayland) {
        /* Setup X11 display */
        Monitor cfg[] = Linux.x11_rr_load_user();
        cfg = Linux.x11_rr_get_setup(cfg);
        Linux.x11_rr_set(cfg);
      }
      start();
    } catch (Exception e) {
      JFLog.log(e);
      System.exit(0);
    }
    if (is_wayland && use_proxy) {
      //create wayland proxy server
      WLProxy.debug = true;
      proxy = new WLProxy(this);
      if (!proxy.start("wayland-0")) {
        JFLog.log("Failed to start wayland proxy");
        use_proxy = false;
      }
    }
    /* Start PulseAudio */
    if (new File("/usr/bin/pulseaudio").exists()) {
      JFLog.log("Starting pulseaudio");
      try {
        Runtime.getRuntime().exec(new String[] {"/usr/bin/pulseaudio", "-nF", "/etc/pulse/default.pa"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    JFLog.log("jfDesktop:starting UI session");
    try {
      int uid = LinuxAPI.getInstance().getUID();
      JFLog.log("uid=" + uid);
      if (is_wayland) {
        startUI(
          new String[] {
            "/usr/bin/jfdesktop-session"
          },
          new String[] {
            "XDG_RUNTIME_DIR=/run/user/" + uid,
            "XDG_SESSION_TYPE=wayland",
            "WAYLAND_DISPLAY=wayland-" + (use_proxy ? "99" : "0"),
            "WAYLAND_PID=" + window_mgr_process.getProcess().pid(),
          }
        );
      } else {
        startUI(
          new String[] {
            "/usr/bin/jfdesktop-session"
          },
          new String[] {
            "XDG_RUNTIME_DIR=/run/user/" + uid,
            "XDG_SESSION_TYPE=x11",
            //XAUTHORITY and DISPLAY set in Logon
          }
        );
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
    JFLog.log("jfDesktop:session has ended");
    try {
      stop();
    } catch (Throwable t) {
      JFLog.log(t);
    }
    if (is_wayland) {
      if (proxy != null) {
        JFLog.log("Stopping WLProxy");
        proxy.stop();
        proxy = null;
      }
    }
    JF.sleep(1000);
    JFLog.log("exit");
    System.exit(0);
  }

  private void startUI(String[] cmds, String[] envs) throws Exception {
    ShellProcess process = new ShellProcess();
    process.keepOutput(false);
    process.addListener(this);
    if (envs != null) {
      for(String e : envs) {
        int idx = e.indexOf('=');
        if (idx == -1) continue;
        String name = e.substring(0, idx);
        String value = e.substring(idx + 1);
        process.addEnvironmentVariable(name, value);
      }
    }
    JFLog.log("Starting Desktop Session...");
    process.run(cmds, true);
    JFLog.log("Desktop Session has ended");
  }

  //interface ShellProcessListener

  public void shellProcessOutput(String out) {
    JFLog.log(LOG_DISPLAY, out);
  }

  private void wait_wayland_socket_opened() {
    String socket = System.getenv("XDG_RUNTIME_DIR") + "/wayland-0";
    JFLog.log("Waiting for wayland socket to open:" + socket);
    for(int a=0;a<10;a++) {
      JF.sleep(1000);
      if (new File(socket).exists()) return;
    }
    JFLog.log("wayland socket not opened");
    JF.sleep(1000);
  }

  private void loginctl() {
    ShellProcess sp = new ShellProcess();
    sp.keepOutput(true);
    String loginctl = sp.run(new String[] {"/usr/bin/loginctl"}, true);
    JFLog.log("loginctl:\n" + loginctl);
  }

  private void start() throws Exception {
    JFLog.log("Starting window manager:" + window_mgr);
    loginctl();
    switch (window_mgr) {
      case "openbox":
        config_openbox();
        start(
          new String[] {
            "/usr/bin/openbox"
          }
        );
        break;
      case "weston":
        config_weston();
        start(
          new String[] {
            "/usr/bin/systemd-run",
            "--scope",
            "--user",
            "--unit=jfdesktop_window_manager_" + user,
            "/usr/bin/weston",
            "--modules",
            "jf-desktop-shell.so"
          }
        );
        wait_wayland_socket_opened();
        break;
      case "labwc":
        config_labwc();
        start(
          new String[] {
            "/usr/bin/systemd-run",
            "--scope",
            "--user",
            "--unit=jfdesktop_window_manager_" + user,
            "/usr/bin/labwc",
            "-d",  //enable debugging : view with journalctl -u jfdesktop_window_manager_$LOGNAME
          }
        );
        wait_wayland_socket_opened();
        break;
      case "sway":
        config_sway();
        start(
          new String[] {
            "/usr/bin/systemd-run",
            "--scope",
            "--user",
            "--unit=jfdesktop_window_manager_" + user,
            "/usr/bin/sway"
          }
        );
        wait_wayland_socket_opened();
        break;
      case "javaforce":
        config_jf_wayland();
        start_jf_wayland();
        break;
    }
  }

  private void start(String[] cmds) {
    new Thread() {
      public void run() {
        window_mgr_process = new ShellProcess();
        window_mgr_process.keepOutput(false);
        window_mgr_process.addListener(new Startup());
        JFLog.log("Starting Window Manager...");
        window_mgr_process.run(cmds, true);
      }
    }.start();
  }

  private void wait_wayland_socket_closed() {
    String socket = System.getenv("XDG_RUNTIME_DIR") + "/wayland-0";
    JFLog.log("Waiting for wayland socket to close:" + socket);
    for(int a=0;a<10;a++) {
      JF.sleep(1000);
      if (!new File(socket).exists()) return;
    }
    JFLog.log("wayland socket not closed");
    JF.sleep(1000);
  }

  public boolean stop() throws Exception {
    if (window_mgr_process == null) {
      JFLog.log("ERROR:stop():window manager not running");
      return false;
    }
    if (window_mgr_process != null) {
      JFLog.log("Stopping Window Manager...");
      if (!is_wayland) {
        window_mgr_process.destroy();
      } else {
        JF.exec(new String[] {
          "/usr/bin/systemctl",
          "--user",
          "stop",
          "jfdesktop_window_manager_" + user + ".scope",
        });
        if (is_wayland) {
          wait_wayland_socket_closed();
        }
      }
      window_mgr_process.waitFor();
      window_mgr_process = null;
      JFLog.log("Window Manager stopped...");
    }
    return true;
  }

  public boolean reconfig() {
    //NOTE : this runs in Session process
    JFLog.log(LOG_DISPLAY, "reconfig:taskbar_height=" + taskbar_height);
    switch (window_mgr) {
      case "openbox":
        config_openbox();
        reconfig(
          new String[] {"/usr/bin/openbox"},
          null
        );
        break;
      case "weston":
        config_weston();
        return false;  //not supported
      case "labwc":
        config_labwc();
        reconfig(
          new String[] {"/usr/bin/labwc", "--reconfigure"},
          new String[] {"LABWC_PID=" + System.getenv("WAYLAND_PID")}
        );
        break;
      case "sway":
        config_sway();
        reconfig(
          new String[] {"/usr/bin/swaymsg", "reload"},
          new String[] {}
        );
        break;
      case "javaforce":
        config_jf_wayland();
        start_jf_wayland();
        break;
    }
    return true;
  }

  private void reconfig(String[] cmds, String[] envs) {
    new Thread() {
      public void run() {
        ShellProcess process = new ShellProcess();
        process.keepOutput(false);
        process.addListener(new Startup());
        if (envs != null) {
          for(String e : envs) {
            int idx = e.indexOf('=');
            if (idx == -1) continue;
            String name = e.substring(0, idx);
            String value = e.substring(idx + 1);
            process.addEnvironmentVariable(name, value);
          }
        }
        JFLog.log("Reconfigure Window Manager...");
        process.run(cmds, true);
      }
    }.start();
  }

  private String getProperty(String name) {
    String prop = props.getProperty(name);
    if (prop == null) prop = "";
    return prop.trim();
  }

  private void start_jf_wayland() {
    new Thread() {
      public void run() {
        wayland.start();
      }
    }.start();
  }

  private void stop_jf_wayland() {
    wayland.stop();
  }

  private void copyAll(String src, String dst, String replace_find, String replace_with) {
    try {
      FileInputStream fis = new FileInputStream(src);
      byte[] data = fis.readAllBytes();
      fis.close();
      String str = new String(data).replace(replace_find, replace_with);
      FileOutputStream fos = new FileOutputStream(dst);
      fos.write(str.getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void config_weston() {
    JF.copyAll("/etc/jflogon/weston.ini", "/etc/xdg/weston/weston.ini");
  }
  private void config_labwc() {
    String labwc =  JF.getUserPath() + "/.config/labwc";
    new File(labwc).mkdirs();
    copyAll("/etc/jfdesktop/labwc-rc.xml", labwc + "/rc.xml", "$SIZE", Integer.toString(taskbar_height));
    JF.copyAll("/etc/jfdesktop/labwc-menu.xml", labwc + "/menu.xml");
  }
  private void config_sway() {
    String sway =  JF.getUserPath() + "/.config/sway";
    new File(sway).mkdirs();
  }
  private void config_jf_wayland() {
    wayland = new Wayland();
  }
  private void config_openbox() {
    String openbox =  JF.getUserPath() + "/openbox";
    new File(openbox).mkdir();
    JF.copyAll("/etc/jfdesktop/openbox-rc.xml", openbox + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/openbox-menu.xml", openbox + "/menu.xml");
  }
  private void log_env() {
    JFLog.log(LOG_DEFAULT, "Environment:");
    String[] envs = JF.getEnvironment();
    for(String e : envs) {
      JFLog.log(LOG_DEFAULT, e);
    }
  }
  private void log_runtime_dir() {
    String[] files = new File(System.getenv("XDG_RUNTIME_DIR")).list();
    JFLog.log("XDG_RUNTIME_DIR:");
    for(String file : files) {
      JFLog.log(file);
    }
  }

  private WLRegistry wl_registry;
  private WLRLayerShell wlr_layer_shell;

  int dock = -1;
  int desktop = -1;
  int window = -1;

  //interface WLNotify

  public void onRequest(String cls, String method, Object[] args) {
    proxy.log("onRequest:" + cls + "." + method);
    switch (cls) {
      case "wl_display": {
        switch (method) {
          case "get_registry":
            int new_id = (Integer)args[0];
            proxy.log("jfDesktop:get_registry");
            wl_registry = new WLRegistry(proxy.getInjector(), new_id);
            break;
        }
        break;
      }
    }
  }

  public void onEvent(String cls, String method, Object[] args) {
    proxy.log("onEvent:" + cls + "." + method);
    switch (cls) {
      case "wl_compositor": {
        switch (method) {
          case "create_surface": {
            //NOTE : create_surface is called multiple times per java.awt.Window but create_region is invoked after all surfaces are created
            int new_id = (Integer)args[0];
            if (window == -1) {
              if (dock == -1) {
                //creating dock
                dock = proxy.getClient().get_next_id();
                proxy.log("jfDesktop:get_layer_surface:dock");
                WLGlobal layer_shell = proxy.getClient().getGlobal("zwlr_layer_shell_v1");
                wlr_layer_shell = (WLRLayerShell)wl_registry.bind(layer_shell.name, layer_shell.iface, layer_shell.ver, dock);
                wlr_layer_shell.get_layer_surface(dock, new_id, 0, WLRLayerShell.LAYER_BOTTOM, "taskbar");
                wlr_layer_shell.destroy();  //TODO : block WLDisplay.delete_id() from reaching real client
              } else if (desktop == -1) {
                //creating desktop
                desktop = proxy.getClient().get_next_id();
                proxy.log("jfDesktop:get_layer_surface:desktop");
                WLGlobal layer_shell = proxy.getClient().getGlobal("zwlr_layer_shell_v1");
                wlr_layer_shell = (WLRLayerShell)wl_registry.bind(layer_shell.name, layer_shell.iface, layer_shell.ver, dock);
                wlr_layer_shell.get_layer_surface(desktop, new_id, 0, WLRLayerShell.LAYER_BACKGROUND, "desktop");
                wlr_layer_shell.destroy();  //TODO : block WLDisplay.delete_id() from reaching real client
              }
              window = 0;  //wait for create_region to reset window indicating a new window might be created
            }
            break;
          }
          case "create_region": {
            window = -1;  //end of create_surfaces for a Window
            int new_id = (Integer)args[0];
            break;
          }
        }
        break;
      }
      case "wl_registry": {
        switch (method) {
          case "global": {
            int name = (Integer)args[0];
            String iface = (String)args[1];
            int ver = (Integer)args[2];
            switch (iface) {
              case "zwlr_foreign_toplevel_manager_v1": {
                break;
              }
              case "wl_seat": {
                break;
              }
              case "zwlr_layer_shell_v1": {
                break;
              }
            }
            break;
          }
        }
        break;
      }
      case "zwlr_foreign_toplevel_manager_v1": {
        break;
      }
      case "zwlr_foreign_toplevel_handle_v1": {
        break;
      }
    }
  }
}
