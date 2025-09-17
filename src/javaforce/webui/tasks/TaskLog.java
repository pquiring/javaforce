package javaforce.webui.tasks;

/** Task Log
 *
 * Historical log of tasks completed.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class TaskLog {
  private String folder;
  private ArrayList<TaskEvent> processing = new ArrayList<>();
  private Object lock = new Object();
  public void setFolder(String folder) {
    this.folder = folder;
  }
  public void add(TaskEvent event) {
    synchronized (lock) {
      processing.add(event);
    }
  }
  public void complete(TaskEvent event) {
    //move from processing to disk
    try {
      Calendar now = Calendar.getInstance();
      int year = now.get(Calendar.YEAR);
      int month = now.get(Calendar.MONTH) + 1;
      synchronized (lock) {
        processing.remove(event);
        String filename = getFilename(year, month);
        RandomAccessFile file_io = new RandomAccessFile(filename, "rw");
        byte[] data = event.toByteArray();
        file_io.seek(file_io.length());
        file_io.write(data);
        file_io.close();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private String getFilename(int year, int month) {
    return String.format("%s/%d-%02d.tasks", folder, year, month);
  }

  /** Get all currently processing tasks. */
  public TaskEvent[] getEventsInProcess() {
    TaskEvent[] list;
    synchronized (lock) {
      list = processing.toArray(TaskEvent.ArrayType);
    }
    return list;
  }

  /** Get all completed task events from year/month. */
  public TaskEvent[] getEvents(int year, int month) {
    byte[] data = new byte[4096];
    try {
      ArrayList<TaskEvent> events = new ArrayList<>();
      synchronized (lock) {
        String filename = getFilename(year, month);
        RandomAccessFile file_io = new RandomAccessFile(filename, "rw");
        long pos = 0;
        long length = file_io.length();
        while (pos < length) {
          int event_length = file_io.read(data, 0, 4);
          while (event_length > data.length) {
            data = new byte[data.length << 1];
          }
          file_io.read(data, 4, event_length);
          events.add(TaskEvent.fromByteArray(data, 4, event_length));
          pos += event_length;
        }
        file_io.close();
      }
      return events.toArray(TaskEvent.ArrayType);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
}
