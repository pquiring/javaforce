package jfcontrols.api;

/** JFC API Service
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.TagType;

import jfcontrols.functions.*;
import jfcontrols.tags.*;
import jfcontrols.sql.*;

public class APIService extends Thread {
  public static void main() {
    new APIService().start();
  }

  private ServerSocket ss;
  private static volatile boolean active;

  private static final int minVersion = 0x100;
  private static final int serverVersion = 0x100;

  public void run() {
    try {
      ss = new ServerSocket(33100);
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    active = true;
    while (active) {
      try {
        Socket s = ss.accept();
        new Session(s).start();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private static class APIException extends Exception {
    public int cmd;
    public int id;
    public int reason;
    public APIException(int cmd, int id, int reason, String msg) {
      super(msg);
      this.cmd = 0x0200 | cmd;
      this.id = id;
      this.reason = reason;
    }
  }

  //error codes
  private static final int ERR_UNKNOWN_CMD = 0x0401;
  private static final int ERR_BAD_VERSION = 0x0402;
  private static final int ERR_DATA_SHORT = 0x0403;
  private static final int ERR_FUNC_NOT_FOUND = 0x404;

  private static class Session extends Thread implements TagBaseListener {
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private Object writeLock = new Object();
    private ArrayList<String> subs = new ArrayList<String>();

    private int clientVersion = 0x100;

    public Session(Socket s) {
      this.s = s;
    }
    public void run() {
      byte header[] = new byte[8];
      int size = 0;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        while (s.isConnected()) {
          int read = is.read(header, size, 8 - size);
          if (read > 0) {
            size += read;
          }
          if (size < 8) continue;
          int cmd = LE.getuint16(header, 0);
          int id = LE.getuint16(header, 2);
          int len = LE.getuint32(header, 4);
          byte data[] = JF.readAll(is, len);
          if (data == null || data.length != len) break;
          doCommand(cmd, id, data);
          size = 0;
        }
      } catch (APIException e) {
        JFLog.log(e);
        try {
          byte reply[] = new byte[8 + 2];
          setupError(reply, e.cmd, e.id, e.reason);
          synchronized(writeLock) {
            os.write(reply);
          }
          os.flush();
        } catch (Exception ex) {}
      } catch (Exception e) {
        JFLog.log(e);
      }
      try {s.close();} catch (Exception e) {}
    }
    private void doCommand(int cmd, int id, byte data[]) throws Exception {
      byte reply[] = null;
      int len = data.length;
      int cnt, pos = 0, size = 0, type;
      String tagName;
      TagBase tag;
      MonitoredTag mtag;
      TagsQuery q;
      switch (cmd) {
        default:
          throw new APIException(cmd, id, ERR_UNKNOWN_CMD, "Error:API:unknown cmd");
        case 0x0001:  //connect
          if (len < 4) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          clientVersion = LE.getuint16(data, 0);
          if (clientVersion < minVersion) {
            throw new APIException(cmd, id, ERR_BAD_VERSION, "Error:API:client not supported");
          }
          reply = new byte[8 + 2];
          setupSuccess(reply, cmd, id);
          LE.setuint16(data, 8, serverVersion);
          break;
        case 0x0002:  //ping
          reply = new byte[8];
          setupSuccess(reply, cmd, id);
          break;
        case 0x0003:  //read tag(s)
          if (len < 2) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          cnt = LE.getuint16(data, pos); len -= 2; pos += 2;
          q = new TagsQuery(cnt);
          size = 2;
          for(int a=0;a<cnt;a++) {
            if (len < 1) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            int strlen = LE.getuint8(data, pos); len--; pos++;
            if (len < strlen) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            byte str[] = new byte[strlen];
            System.arraycopy(data, pos, str, 0, strlen);
            len -= strlen;
            pos += strlen;
            tagName = new String(str);
            tag = TagsService.getTag(tagName);
            q.tags[a] = tag;
            size += 4;  //type / size
            if (tag == null) {
              q.sizes[a] = tag.getSize();
              size += q.sizes[a];
            }
          }
          //build query to function service
          FunctionService.addReadQuery(q);
          //build reply
          reply = new byte[8 + size];
          setupSuccess(reply, cmd, id);
          pos = 8;
          LE.setuint16(data, pos, cnt); pos += 2;
          for(int a=0;a<cnt;a++) {
            tag = q.tags[a];
            if (tag == null) {
              LE.setuint16(data, pos, TagType.unknown); pos += 2;
              LE.setuint16(data, pos, 0); pos += 2;
            } else {
              type = tag.getType();
              LE.setuint16(data, pos, type); pos += 2;
              LE.setuint16(data, pos, q.sizes[a]); pos += 2;
              TagBase.encode(type, q.values[a], data, pos); pos += q.sizes[a];
            }
          }
          break;
        case 0x0004:  //write tag(s)
          if (len < 2) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          cnt = LE.getuint16(data, pos); len -= 2; pos += 2;
          q = new TagsQuery(cnt);
          for(int a=0;a<cnt;a++) {
            if (len < 1) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            int strlen = LE.getuint8(data, pos); len--; pos++;
            if (len < strlen) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            byte str[] = new byte[strlen];
            System.arraycopy(data, pos, str, 0, strlen);
            len -= strlen;
            pos += strlen;
            tagName = new String(str);
            tag = TagsService.getTag(tagName);
            q.tags[a] = tag;
            if (len < 2) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            type = LE.getuint16(data, pos); len -= 2; pos += 2;
            size = LE.getuint16(data, pos); len -= 2; pos += 2;
            if (size != TagBase.getSize(type)) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            q.sizes[a] = size;
            if (len < size) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            q.values[a] = TagBase.decode(type, data, pos); pos += size; len -= size;
          }
          //build query to function service
          FunctionService.addWriteQuery(q);
          //build reply
          reply = new byte[8];
          setupSuccess(reply, cmd, id);
          break;
        case 0x0005:  //sub tag(s)
          if (len < 2) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          cnt = LE.getuint16(data, pos); len -= 2; pos += 2;
          q = new TagsQuery(cnt);
          for(int a=0;a<cnt;a++) {
            if (len < 1) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            int strlen = LE.getuint8(data, pos); len--; pos++;
            if (len < strlen) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            byte str[] = new byte[strlen];
            System.arraycopy(data, pos, str, 0, strlen);
            len -= strlen;
            pos += strlen;
            tagName = new String(str);
            mtag = (MonitoredTag)TagsService.getTag(tagName);
            if (mtag == null) {
              if (!subs.contains(tagName)) {
                mtag.addListener(this);
                subs.add(tagName);
              }
            }
          }
          reply = new byte[8];
          setupSuccess(reply, cmd, id);
          break;
        case 0x0006:  //unsub tag(s)
          if (len < 2) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          cnt = LE.getuint16(data, pos); len -= 2; pos += 2;
          q = new TagsQuery(cnt);
          for(int a=0;a<cnt;a++) {
            if (len < 1) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            int strlen = LE.getuint8(data, pos); len--; pos++;
            if (len < strlen) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
            byte str[] = new byte[strlen];
            System.arraycopy(data, pos, str, 0, strlen);
            len -= strlen;
            pos += strlen;
            tagName = new String(str);
            mtag = (MonitoredTag)TagsService.getTag(tagName);
            if (mtag == null) {
              if (subs.contains(tagName)) {
                mtag.removeListener(this);
                subs.remove(tagName);
              }
            }
          }
          reply = new byte[8];
          setupSuccess(reply, cmd, id);
          break;
        case 0x0007:  //call func
          if (len < 1) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          int strlen = LE.getuint8(data, pos); len--; pos++;
          if (len < strlen) throw new APIException(cmd, id, ERR_DATA_SHORT, "Error:API:data short");
          byte str[] = new byte[strlen];
          System.arraycopy(data, pos, str, 0, strlen);
          len -= strlen;
          pos += strlen;
          String funcName = new String(str);
          SQL sql = SQLService.getSQL();
          String sfid = sql.select1value("select fid from funcs where name=" + SQL.quote(funcName));
          sql.close();
          if (sfid == null) {
            reply = new byte[10];
            setupError(reply, cmd, id, ERR_FUNC_NOT_FOUND);
          } else {
            int fid = Integer.valueOf(sfid);
            FunctionService.functionRequest(fid);
            reply = new byte[8];
            setupSuccess(reply, cmd, id);
          }
          break;
      }
      synchronized(writeLock) {
        os.write(reply);
      }
      os.flush();
    }

    private void setupSuccess(byte reply[], int cmd, int id) {
      LE.setuint16(reply, 0, 0x100 | cmd);
      LE.setuint16(reply, 2, id);
      LE.setuint32(reply, 4, reply.length - 8);
    }

    private void setupError(byte reply[], int cmd, int id, int error) {
      LE.setuint16(reply, 0, 0x200 | cmd);
      LE.setuint16(reply, 2, id);
      LE.setuint32(reply, 4, reply.length - 8);
      LE.setuint16(reply, 8, error);
    }

    public void tagChanged(TagBase tag, String value) {
      int size = tag.getSize();
      int type = tag.getType();
      byte reply[] = new byte[8 + 2 + 2 + size];
      setupSuccess(reply, 0x0003, 0);  //read cmd
      LE.setuint16(reply, 8, 1);  //count
      LE.setuint16(reply, 10, type);  //type
      TagBase.encode(type, value, reply, 12);
      synchronized(writeLock) {
        try { os.write(reply); } catch (Exception e) {}
      }
    }
  }
}
