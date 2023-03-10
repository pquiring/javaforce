package jfapps;

/**
 * Created : Mar 2, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.linux.*;

public class App extends javax.swing.JPanel {

  /**
   * Creates new form GraphicsSection
   */
  public App() {
    initComponents();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        name = new javax.swing.JLabel();
        desc = new javax.swing.JLabel();
        install = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        setMaximumSize(new java.awt.Dimension(32767, 102));

        name.setText("Name");

        desc.setText("Short Description");

        install.setText("Install / Remove");
        install.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(desc, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(install)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(name)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(desc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(install)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

  private void installActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installActionPerformed
    if (pkg.startsWith("#web#")) {
      if (installed)
        Linux.removePackage(pkg.substring(5), name.getText());
      else
        downloadPackage(pkg.substring(5), name.getText());
    } else {
      if (installed)
        Linux.removePackage(pkg, name.getText());
      else
        Linux.installPackage(pkg, name.getText());
    }
    MainPanel.reload();
  }//GEN-LAST:event_installActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel desc;
    private javax.swing.JButton install;
    private javax.swing.JLabel name;
    // End of variables declaration//GEN-END:variables

  private String pkg;
  private boolean installed;
  private static String errmsg;
  private static final boolean wget = false;  //test

  public void setup(String name, String desc, String pkg, boolean installed) {
    this.pkg = pkg;
    this.installed = installed;
    this.name.setText(name);
    this.desc.setText(desc);
    if (installed) {
      this.install.setText("Remove");
    } else {
      this.install.setText("Install");
    }
  }

  private void downloadPackage(String pkg, String desc) {
    String url = null;
    if (pkg.equals("pdf")) {
      Linux.detectDistro();
      switch (Linux.distro) {
        case Ubuntu:
          switch (Linux.detectBits()) {
            case 32: url = "http://ardownload.adobe.com/pub/adobe/reader/unix/9.x/9.5.1/enu/AdbeRdr9.5.1-1_i386linux_enu.deb"; break;
            case 64: url = null; break;
          }
          break;
        case Fedora:
          switch (Linux.detectBits()) {
            case 32: url = "http://ardownload.adobe.com/pub/adobe/reader/unix/9.x/9.5.1/enu/AdbeRdr9.5.1-1_i486linux_enu.rpm"; break;
            case 64: url = null; break;
          }
          break;
      }
    } else if (pkg.equals("chrome")) {
      Linux.detectDistro();
      switch (Linux.distro) {
        case Ubuntu:
          switch (Linux.detectBits()) {
            case 32: url = "https://dl.google.com/linux/direct/google-chrome-stable_current_i386.deb"; break;
            case 64: url = "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb"; break;
          }
          break;
        case Fedora:
          switch (Linux.detectBits()) {
            case 32: url = "https://dl.google.com/linux/direct/google-chrome-stable_current_i386.rpm"; break;
            case 64: url = "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.rpm"; break;
          }
          break;
      }
    } else if (pkg.equals("flash")) {
      Linux.detectDistro();
      switch (Linux.distro) {
        case Ubuntu:
          switch (Linux.detectBits()) {
            case 32: url = null; break;
            case 64: url = null; break;
          }
          break;
        case Fedora:
          switch (Linux.detectBits()) {
            case 32: url = "http://linuxdownload.adobe.com/adobe-release/adobe-release-i386-1.0-1.noarch.rpm"; break;
            case 64: url = "http://linuxdownload.adobe.com/adobe-release/adobe-release-x86_64-1.0-1.noarch.rpm"; break;
          }
          break;
      }
    }
    if (url == null) {
      JFAWT.showError("Error", "That package can't be found for your platform");
      return;
    }
    //download URL to /tmp and run it as root
    try {
      JFTask task = new JFTask() {
        public boolean work() {
          try {
            String urlString = (String)this.getProperty("url");
            String pkg = (String)this.getProperty("pkg");
            String desc = (String)this.getProperty("desc");
            String output;
            this.setLabel("Downloading " + desc);
            this.setProgress(5);
            int idx = urlString.lastIndexOf(".");
            String ext = urlString.substring(idx).toLowerCase();
            File tmpFile = File.createTempFile("pkg", ext, new File("/tmp"));
            JFLog.log("url=" + urlString);
            if (wget) {
              ShellProcess sp = new ShellProcess();
              output = sp.run(new String[] {"wget", urlString, "-O", tmpFile.getAbsolutePath()}, true);
              if (sp.getErrorLevel() != 0) {
                JFLog.log("output=" + output);
                throw new Exception("download failed");
              }
            } else {
              //TODO:this is not working!!!
              URL url = new URL(urlString);
              HttpURLConnection uc = (HttpURLConnection)url.openConnection();
              this.setProgress(25);
              uc.connect();
              long length = uc.getContentLength();
              if (length <= 0) throw new Exception("unknown length");
              InputStream fis = uc.getInputStream();
              FileOutputStream fos = new FileOutputStream(tmpFile);
              if (uc.getResponseCode() != 200) throw new Exception("Error:" + uc.getResponseCode() + ":" + uc.getResponseMessage());
              if (!JF.copyAll(fis, fos, length)) throw new Exception("download failed");
              fis.close();
              fos.close();
            }
            this.setProgress(50);
            this.setLabel("Installing " + desc);
            boolean ok = false;
            if (ext.equals(".deb")) {
              ShellProcess sp = new ShellProcess();
              sp.removeEnvironmentVariable("TERM");
              sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
              output = sp.run(new String[] {"sudo", "-E" ,"dpkg", "-i", tmpFile.getAbsolutePath()}, true);
              if (sp.getErrorLevel() == 0) ok = true;
            } else if (ext.equals(".rpm")) {
              ShellProcess sp = new ShellProcess();
              sp.removeEnvironmentVariable("TERM");
              sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
              output = sp.run(new String[] {"sudo", "-E" ,"rpm", "-i", tmpFile.getAbsolutePath()}, true);
              if (sp.getErrorLevel() == 0) ok = true;
            } else {
              //just execute it as root
              Runtime.getRuntime().exec(new String[] {"chmod", "+x", tmpFile.getAbsolutePath()});
              ShellProcess sp = new ShellProcess();
              output = sp.run(new String[] {"sudo" ,tmpFile.getAbsolutePath()}, true);
              if (sp.getErrorLevel() == 0) ok = true;
            }
            if (!ok) {
              errmsg = "Failed to install package";
              JFLog.log("Failed to install:" + pkg);
              JFLog.log("output=" + output);
            }
//            tmpFile.delete();  //test

            //some apps need other packages, "apt -f install" will fix that
            if (Linux.distro == Linux.DistroTypes.Ubuntu) {
              ShellProcess sp = new ShellProcess();
              sp.removeEnvironmentVariable("TERM");
              sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
              sp.run(new String[] {"sudo", "apt", "-f", "--yes", "install"}, false);
            }

            this.setLabel("Complete");
            this.setProgress(100);
            errmsg = null;
            return true;
          } catch (Exception e) {
            errmsg = e.toString();
            return false;
          }
        }
      };
      task.setProperty("url", url);
      task.setProperty("pkg", pkg);
      task.setProperty("desc", desc);

      ProgressDialog dialog = new ProgressDialog(null, true, task);
      dialog.setVisible(true);
      if (errmsg != null) {
        JFAWT.showError("Error", "Exception:" + errmsg);
      }
    } catch (Exception e) {
      JFAWT.showError("Error", "Exception:" + e);
    }
  }
}
