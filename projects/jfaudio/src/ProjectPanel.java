/**
 * Created : Jun 15, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;

public class ProjectPanel extends javax.swing.JPanel {

  /**
   * Creates new form ProjectPanel
   */
  public ProjectPanel(String filename) {
    initComponents();
    tracks.setLayout(new TracksLayout());
    if (filename == null) {
      Random r = new Random();
      this.path = Paths.getNewProjectPath();
      new File(this.path).mkdirs();
      new File(this.path + "/" + "undo").mkdirs();
      new File(this.path + "/" + "cb").mkdirs();  //clipboard
    } else {
      this.filename = filename;
      int idx = filename.lastIndexOf(".");
      this.path = filename.substring(0, idx) + "_data";
      loadTracks();
      deleteUndo();
    }
    JFLog.log("project:" + path);
    calcMaxLength();
    timeLine.add(new TimeLine());
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        timeScroll = new javax.swing.JScrollBar();
        tracks = new javax.swing.JPanel();
        timeLine = new javax.swing.JPanel();
        tracksScroll = new javax.swing.JScrollBar();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        timeScroll.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        timeScroll.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                timeScrollAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout tracksLayout = new javax.swing.GroupLayout(tracks);
        tracks.setLayout(tracksLayout);
        tracksLayout.setHorizontalGroup(
            tracksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        tracksLayout.setVerticalGroup(
            tracksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 675, Short.MAX_VALUE)
        );

        timeLine.setLayout(new java.awt.GridLayout(1, 1));

        tracksScroll.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                tracksScrollAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(timeLine, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(timeScroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tracks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tracksScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(timeLine, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tracks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tracksScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

  private void timeScrollAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_timeScrollAdjustmentValueChanged
    setOffset(timeScroll.getValue());
  }//GEN-LAST:event_timeScrollAdjustmentValueChanged

  private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
    resizeTracks();
  }//GEN-LAST:event_formComponentResized

  private void tracksScrollAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_tracksScrollAdjustmentValueChanged
    tracks.doLayout();
  }//GEN-LAST:event_tracksScrollAdjustmentValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel timeLine;
    private javax.swing.JScrollBar timeScroll;
    private javax.swing.JPanel tracks;
    private javax.swing.JScrollBar tracksScroll;
    // End of variables declaration//GEN-END:variables

  public String filename;  //project xml file (.sndxml) (may be null)
  public String path;  //path to store project files
  public double scale = 128.0;  //# px per second
  public double offset = 0.0;  //in seconds
  public double selectStart = 0.0;  //in seconds
  public double selectStop = 0.0;  //in seconds
  public double maxLength = 10.0;  //in seconds
  public volatile boolean paused = false;
  public final Object pausedLock = new Object();
  public ArrayList<TrackPanel> list = new ArrayList<TrackPanel>();
  public Config config = new Config();

  public static class Config {
    public int no_options_yet;
  }

  public void loadConfig() {
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(filename);
      xml.read(fis);
      xml.writeClass(config);
    } catch (FileNotFoundException e1) {
      defaultConfig();
    } catch (Exception e2) {
      JFLog.log(e2);
      defaultConfig();
    }
  }

  public void defaultConfig() {
    config = new Config();
  }

  public void saveConfig() {
    try {
      XML xml = new XML();
      FileOutputStream fos = new FileOutputStream(filename);
      xml.readClass("audio", config);
      xml.write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getTabName() {
    int i1 = filename.lastIndexOf("/");
    int i2 = filename.lastIndexOf(".");
    return filename.substring(i1+1, i2);
  }

  public int genTrackID() {
    int tid = list.size();
    for(int a=0;a<list.size();) {
      if (list.get(a).tid == tid) {tid++; a = 0;} else {a++;}
    }
    return tid;
  }

  boolean use_transcoder = true;

  public void importFile(String fn) {
//    if (fn.toLowerCase().endsWith(".wav")) {
//      importWav(fn);  //does not support all wav types (ie: non-PCM)
//    } else {
      if (use_transcoder) importFileTranscoder(fn); else importFileDecoder(fn);
//    }
  }

  public Transcoder transcoder;
  public boolean transcoderSuccess;
  public File transcoderOutFile;
  public String transcoderInFile;

  public void importFileTranscoder(String fn) {
    try {
      transcoder = new Transcoder();
      transcoderOutFile = Paths.getTempFile("temp", ".wav");
      transcoderInFile = fn;
      JFTask task = new JFTask() {
        public boolean work() {
          this.setProgress(-1);  //indeterminate
          this.setTitle("Progress");
          this.setLabel("Transcoding file...");
          transcoderSuccess = transcoder.transcode(transcoderInFile, transcoderOutFile.getAbsolutePath()
            , "wav");
          return true;
        }
      };
      ProgressDialog dialog = new ProgressDialog(null, true, task);
      dialog.setAutoClose(true);
      dialog.setVisible(true);
      if (!transcoderSuccess) {
        transcoderOutFile.delete();
        throw new Exception("error");
      }
      importWav(transcoderOutFile.getAbsolutePath());
      transcoderOutFile.delete();
    } catch (Exception e) {
      JFLog.log(e);
      JFAWT.showError("Error", "Import failed");
    }
  }

  public void importFileDecoder(String fn) {
    try {
      File tempFile = Paths.getTempFile("temp", ".wav");
      FileOutputStream fos = new FileOutputStream(tempFile);
      Decoder decoder = new Decoder();
      decoder.decode(fn);
      //write wav header
      TrackPanel.writeWavHeader(fos, decoder.getChannels(), decoder.getSampleRate(), 16, 2, 0x7fffffff);
      do {
        short samples[] = decoder.getSamples();
        if (samples == null) break;
        fos.write(LE.shortArray2byteArray(samples, null));
      } while (true);
      fos.close();
      importWav(tempFile.getAbsolutePath());
      tempFile.delete();
    } catch (Exception e) {
      JFLog.log(e);
      JFAWT.showError("Error", "Import failed");
    }
  }

  public void importWav(String fn) {
    //create a new TrackID # and convert WAV into managable files
    int tid = genTrackID();
    Wav wav = new Wav();
    if (!wav.load(fn)) {
      JFAWT.showError("Error", wav.getError());
      return;
    }
    String trackPath = path + "/" + tid;
    File file = new File(trackPath);
    file.mkdirs();
    TrackPanel track = new TrackPanel(this, tid, wav);
    wav.close();
    list.add(track);
    tracks.add(track);
    resizeTracks();
    validate();
  }

  public void loadTracks() {
    File file = new File(path);
    File trackFolders[] = file.listFiles();
    if (trackFolders == null) return;
    for(int a=0;a<trackFolders.length;a++) {
      String num = trackFolders[a].getName();
      if (num.charAt(0) < '0') continue;
      if (num.charAt(0) > '9') continue;
      if (!trackFolders[a].isDirectory()) continue;
      loadTrack(JF.atoi(num));
    }
    validate();
    repaint();
  }

  public void loadTrack(int tid) {
    TrackPanel track = new TrackPanel(this, tid);
    tracks.add(track);
    list.add(track);
  }

  public void close() {
    if (Paths.isTempPath(path)) {
      JFLog.log("deleting temp project:" + path);
      try {Paths.deleteFolderEx(path); } catch (Exception e) {}
    }
  }

  private class TimeLine extends JComponent {
    public void paint(Graphics g) {
//      JFLog.log("TimeLine:scale=" + scale);
      int x = getWidth();
      int y = getHeight();
      g.setColor(Color.GRAY);
      g.fillRect(0,0,x,y);
      double point_n = 1.0;
      double point_d = scale;
      if (point_d > 256.0) {
        while (point_d > 256.0) {
          point_n /= 2.0;
          point_d /= 2.0;
        }
      } else {
        while (point_d < 64.0) {
          point_n *= 2.0;
          point_d *= 2.0;
        }
      }
      double toff = offset;
      double toffDec = toff % point_n;
      int poff = (int)(-scale * toffDec);
      toff -= toffDec;
      int h = getHeight();
      g.setColor(Color.BLACK);
      FontMetrics fm = this.getFontMetrics(this.getFont());
      int fh = fm.getHeight();
      String txt;
      while (poff < getWidth()) {
        int min = (int)(toff / 60.0);
        int sec = (int)(toff - (min * 60.0));
        int sec100 = (int)((toff - (min * 60.0) - sec + .005) * 100.0);
        if (toff >= 60.0) {
          txt = String.format("%d:%02d:%02d", min, sec, sec100);
        } else {
          txt = String.format("%d:%02d", sec, sec100);
        }
        int fw = fm.stringWidth(txt) / 2;
        g.drawBytes(txt.getBytes(), 0, txt.length(), poff - fw, h/2 - 5);
        g.drawLine(poff,h/2,poff,h);
        if (selectStart >= toff && selectStart < toff + point_n) {
          int soff = poff + (int)((selectStart - toff) * scale);
          g.drawLine(soff, 0, soff, h);
        }
        if (selectStop >= toff && selectStop < toff + point_n) {
          int soff = poff + (int)((selectStop - toff) * scale);
          g.drawLine(soff, 0, soff, h);
        }
        toff += point_n;
        poff += point_d;
      }
    }
    public Dimension getPreferredSize() {
      return new Dimension(ProjectPanel.this.getWidth(), 32);
    }
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }
  public void zoom(int dir) {
    switch (dir) {
      case -1:
        if (scale <= 0.25) return;
        scale /= 2.0;
        break;
      case 1:
        if (scale >= 32.0 * 1024.0) return;
        scale *= 2.0;
        break;
    }
    calcMaxLength();
    repaint();
  }
  public void setOffset(int value) {
    offset = value / scale;
    repaint();
  }
  public void calcMaxLength() {
    maxLength = getWidth() / scale;
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      double trackLength = track.totalLength / track.rate;  //in seconds
      if (trackLength > maxLength) maxLength = trackLength;
    }
    maxLength += 10.0f;  //show a bit beyond the end
//    JFLog.log("maxLength=" + maxLength);
    timeScroll.setMinimum(0);
    timeScroll.setMaximum((int)(maxLength * scale));
//    JFLog.log(" max=" + maxLength * scale);
//    timeScroll.setBlockIncrement(?);
//    JFLog.log("view=" + getWidth() / scale);
    timeScroll.setVisibleAmount(getWidth());
  }
  public void autoZoom() {
    //make sure to call calcMaxLength() first
    //find scale that will show maxLength
    //TODO
  }
  public void record() {
    //create a new track and add samples to it
    paused = false;
    TrackPanel track = new TrackPanel(this, genTrackID(), Settings.current.channels, Settings.current.freq, 16);
    list.add(track);
    tracks.add(track);
    resizeTracks();
    tracksScroll.setValue(totalTracksHeight());
//    setRecVol();
    track.record();
    validate();
  }
  public void stop() {
    paused = false;
    for(int a=0;a<list.size();a++) {
      list.get(a).stop();
    }
  }
  public void stopped(int tid) {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.recording) return;
      if (track.playing) return;
    }
    //tracks all done
    MainPanel.main.stopped();
  }
  public void play() {
    paused = false;
//    setPlayVol();
    for(int a=0;a<list.size();a++) {
      list.get(a).play();
    }
  }
  public void pause() {
    paused = !paused;
    synchronized(pausedLock) {
      pausedLock.notifyAll();
    }
  }
  public void selectStart(double pos) {
    selectStart = pos;
    selectStop = selectStart;
    repaint();
  }
  public void selectStop(double pos) {
    selectStop = pos;
    repaint();
  }
  public void selectTrack(TrackPanel track, boolean mute) {
    //unselect all other tracks
    for(int a=0;a<list.size();a++) {
      TrackPanel otherTrack = list.get(a);
      if (track.tid != otherTrack.tid) otherTrack.unselectAll();
    }
    MainPanel.main.selectTrack(track);
  }
  /** ensure offset (in seconds) is visible */
  public void showOffset(long offset) {
    int value = timeScroll.getValue();
    int newValue = (int)(offset * scale);
    int start = value;
    int end = (int)(value + getWidth());
    if (newValue >= start && newValue <= end) return;
    timeScroll.setValue(newValue);
  }

  public void deleteTrack() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        if (track.playing || track.recording) return;
        try {
          Paths.deleteFolderEx(path + "/" + track.tid);
        } catch (Exception e) {
          JFAWT.showError("Error", "Failed to delete track from project data folder");
          JFLog.log(e);
          return;
        }
        list.remove(a);
        tracks.remove(track);
        selectStart = selectStop = 0.0;
        calcMaxLength();
        resizeTracks();
        gotoHome();
        validate();
        repaint();
        return;
      }
    }
  }

  public void gotoHome() {
    showOffset(0);
  }

  public void gotoEnd() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        showOffset(track.totalLength);
        return;
      }
    }
  }
  public void mute() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        track.mute();
        return;
      }
    }
  }
  public void unmute() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        track.unmute();
        return;
      }
    }
  }
  public void solo() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        track.unmute();
      } else {
        track.mute();
      }
    }
  }
  public boolean isTrackSelected() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) return true;
    }
    return false;
  }
  public void exportFile(String fn, boolean selection) {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        track.exportFile(fn, selection);
        return;
      }
    }
    JFAWT.showError("Error", "Select a track to export");
  }
  public TrackPanel getSelectedTrack() {
    for(int a=0;a<list.size();a++) {
      TrackPanel track = list.get(a);
      if (track.selected) {
        return track;
      }
    }
    JFAWT.showError("Error", "Select a track");
    return null;
  }
  public void deleteUndo() {
    deleteFiles(path + "/undo/");
  }
  public static void deleteClipboard() {
    deleteFiles(MainPanel.clipboardPath + "/");
  }
  private static void deleteFiles(String path) {
    File files[] = new File(path).listFiles();
    if (files == null) return;
    for(int a=0;a<files.length;a++) {
      files[a].delete();
    }
  }
  private void resizeTracks() {
//    JFLog.log("resizeTracks");
    tracksScroll.setMinimum(0);
    tracksScroll.setMaximum(totalTracksHeight());
    tracksScroll.setVisibleAmount(tracks.getHeight());
  }
  public int getTracksWidth() {
    return tracks.getWidth();
  }
  private Dimension layoutSize;
  private class TracksLayout implements LayoutManager {
//    private Vector<Component> list = new Vector<Component>();
    public void addLayoutComponent(String string, Component cmp) {
//      list.add(cmp);
    }

    public void removeLayoutComponent(Component cmp) {
//      list.remove(cmp);
    }

    public Dimension preferredLayoutSize(Container c) {
      if (layoutSize == null) layoutContainer(c);
      return layoutSize;
    }

    public Dimension minimumLayoutSize(Container c) {
      return new Dimension(1,1);
    }

    public void layoutContainer(Container c) {
      int cnt = c.getComponentCount();
      int x = tracks.getWidth();
      int cx = 0;
      int cy = -tracksScroll.getValue();
      for(int a=0;a<cnt;a++) {
        Component child = c.getComponent(a);
        Dimension d = child.getPreferredSize();
        child.setBounds(cx, cy, x, d.height);
        cy += d.height;
      }
      c.setPreferredSize(new Dimension(x, cy));
      layoutSize = new Dimension(x, cy);
    }
  }

  public void undo() {
    getSelectedTrack().undo();
  }

  public void mixTracks() {
    if (list.size() < 2) {
      JFAWT.showError("Error", "Require at least two tracks to mix");
      return;
    }
    MixTracks dialog = new MixTracks(null, true, this, list);
    dialog.setVisible(true);
    if (!dialog.accepted) return;
  }

  public int totalTracksHeight() {
    int height = 0;
    for(int a=0;a<list.size();a++) {
      height += list.get(a).channels * 127;
    }
    return height;
  }

  public void newTrack(int chs) {
    int tid = genTrackID();
    String trackPath = path + "/" + tid;
    File file = new File(trackPath);
    file.mkdirs();
    TrackPanel track = new TrackPanel(this, tid, chs, Settings.current.freq, 16);
    list.add(track);
    tracks.add(track);
    resizeTracks();
    validate();
  }
}
