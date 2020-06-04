package javaforce.utils;

/*/

 Find & Replace utility.

 Usage : "oldstr" "newstr" filespec [-i] [-asis] [-r]
 -i = case-insensitive
 -asis = no C-ctyle string processing
 -r = recursive

 Strings may contain C-style chars : \n\t\r \xHH \DDD \\

 /*/
import java.io.*;
import java.util.Random;

import javaforce.*;

public class FindReplace {

  public static void main(String[] args) {
    FindReplace x = new FindReplace();
    x.main2(args);
  }
  boolean b_icmp = false;
  boolean b_asis = false;
  boolean b_recursive = false;
  final int BUFSIZE = (64 * 1024); //buffer size
  final int THSIZE = (32 * 1024);  //thresh hold size
  String oldstr, newstr;

  void usage() {
    System.out.println("Find & Replace utility v" + JF.getVersion());
    System.out.println("Usage : jfr [options] oldstr newstr filespec");
    System.out.println(" strings may contain C-style codes (\\n\\r\\t\\xHH\\DDD)");
    System.out.println("   HH=hex DDD=decimal ascii codes");
    System.out.println(" place \"quotes\" around str if it starts with '-' or spaces");
    System.out.println(" -asis = do not process C-style codes");
    System.out.println(" -i = case insensitive comparison");
    System.out.println(" -r = recursive");
    System.exit(0);
  }
  byte[] ibuf;
  byte[] obuf;
  ParseArgs pa = new ParseArgs();

  void main2(String[] args) {
    int a;
    pa.arg_decoderefs = false;  //do not process @ref files
    pa.arg_parse(args);

    if (pa.arg_names.size() < 3) {
      usage();
    }

    for (a = 0; a < pa.arg_opts.size(); a++) {
      if (pa.arg_opts.get(a).equals("i")) {
        b_icmp = true;
        continue;
      }
      if (pa.arg_opts.get(a).equals("asis")) {
        b_asis = true;
        continue;
      }
      if (pa.arg_opts.get(a).equals("r")) {
        b_recursive = true;
        continue;
      }
      System.out.println("Option ignored : " + pa.arg_opts.get(a));
    }

    oldstr = convertstr(pa.arg_names.get(0));
    newstr = convertstr(pa.arg_names.get(1));

    System.out.println("Converting \"" + pa.arg_names.get(0) + "\" to \"" + pa.arg_names.get(1) + "\"");
    System.out.println("Converting [" + makestr(oldstr) + "] to [" + makestr(newstr) + "]");

    ibuf = new byte[BUFSIZE];
    obuf = new byte[BUFSIZE];

    for (a = 2; a < pa.arg_names.size(); a++) {
      File file = new File(pa.arg_names.get(a));
      if (file.isDirectory()) {
        doFolder(file);
      } else {
        doFile(file);
      }
    }
  }

  void error(String msg) {
    System.out.println("Error : " + msg);
    System.exit(1);
  }

  void doFolder(File folder) {
    File[] files = folder.listFiles();
    for(int a=0;a<files.length;a++) {
      File file = files[a];
      if (file.isDirectory()) {
        if (!b_recursive) continue;
        doFolder(file);
      } else {
        doFile(file);
      }
    }
  }

  void doFile(File fi) {
    if (!fi.exists()) {
      error("file not found:" + fi.getAbsolutePath());
      return;
    }
    File fo;  //out filename
    Random r = new Random();
    JF.randomize(r);
    String parent = fi.getParent();
    if (parent == null) {
      parent = "";
    }

    fo = new File(parent + "jfr-" + r.nextInt(0xffffff) + ".tmp");  //rename later to ifn

    int cnt = 0;

    FileInputStream fis = JF.fileopen(fi.getAbsolutePath());
    if (fis == null) {
      JF.msg("Unable to open : " + fi.getAbsolutePath());
      return;
    }

    FileOutputStream fos = JF.filecreate(fo.getAbsolutePath());

    int ifs;
    int ofs;

    ifs = JF.filelength(fis);
    ofs = 0;

    int ibs = 0, obs = 0;  //buffer sizes
    int ibp = 0;  //in buffer pos

    boolean res;

    while ((ifs > 0) || (ibs > 0)) {
      if ((ibs < THSIZE) && (ifs > 0)) {
        if (ibs > 0) {
          System.arraycopy(ibuf, ibp, ibuf, 0, ibs);  //memcpy(ibuf, ibuf + ibp, ibs);
        }
        ibp = 0;
        if (ifs >= THSIZE) {
          //read another block
          if (JF.read(fis, ibuf, ibs, THSIZE) != THSIZE) {
            error("unable to read file");
          }
          ibs += THSIZE;
          ifs -= THSIZE;
        } else {
          //read what's left
          if (JF.read(fis, ibuf, ibs, ifs) != ifs) {
            error("unable to read file");
          }
          ibs += ifs;
          ifs = 0;
        }
      }

      if (obs > THSIZE) {
        if (!JF.write(fos, obuf, 0, obs)) {
          error("unable to write file");
        }
        obs = 0;
      }
      //the in buffer will always be at least THSIZE in bytes
      if (b_icmp) {
        res = JF.memicmp(ibuf, ibp, oldstr.getBytes(), 0, oldstr.length());
      } else {
        res = JF.memcmp(ibuf, ibp, oldstr.getBytes(), 0, oldstr.length());
      }
      if (res) {
        //we got a match
        if (newstr.length() > 0) {
          System.arraycopy(newstr.getBytes(), 0, obuf, obs, newstr.length());
        }
        obs += newstr.length();
        ofs += newstr.length();
        ibp += oldstr.length();
        ibs -= oldstr.length();
        cnt++;
      } else {
        obuf[obs] = ibuf[ibp];
        obs++;
        ibp++;
        ibs--;
        ofs++;
      }
    }
    if (obs > 0) {
      if (!JF.write(fos, obuf, 0, obs)) {
        error("unable to write file");
      }
    }
    int read, allread = 0;
    if (cnt > 0) {
      //must copy new file into old (DO NOT RENAME!!!)
      //renaming is easier, but it messes up the FileFind procedures
      JF.fileclose(fis);
      JF.fileclose(fos);
      fis = JF.fileopen(fo.getAbsolutePath());   //note : reversed here to copy back
      fos = JF.filecreate(fi.getAbsolutePath());
      while (!JF.eof(fis)) {
        read = JF.read(fis, ibuf, 0, BUFSIZE);
        allread += read;
        if (!JF.write(fos, ibuf, 0, read)) {
          error("unable to write file");
        }
      }
      if (allread != ofs) {
        error("unable to write file");
      }
      JF.fileclose(fis);
      JF.fileclose(fos);
      fo.delete();
      System.out.println("Replaced " + cnt + " occurances of string in file " + fi.getAbsolutePath());
    } else {
      JF.fileclose(fis);
      JF.fileclose(fos);
      fo.delete();
      System.out.println("String not found in " + fi.getAbsolutePath());
    }
  }

  String convertstr(String in) {
    String s = "";
    char c;
    int v1, v2, v3, x;
    for (int a = 0; a < in.length(); a++) {
      c = in.charAt(a);
      if ((c == '\\') && (!b_asis)) {
        x = in.charAt(a + 1);
        if ((x >= '0') && (x <= '9')) {
          //\DDD
          v1 = x;
          v2 = in.charAt(a + 2);
          if (!((v2 >= '0') && (v2 <= '9'))) {
            s += c;
            continue;
          }
          v3 = in.charAt(a + 3);
          if (!((v3 >= '0') && (v3 <= '9'))) {
            s += c;
            continue;
          };
          v1 -= '0';
          v2 -= '0';
          v3 -= '0';
          x = v1 * 100 + v2 * 10 + v3;
          if (x > 127) {
            x -= 256;  //make is signed 8bit (everything in Java is signed)
          }
          c = (char) x;
          s += c;
          a += 3;
          continue;
        }
        switch (x) {
          case '\\':
            s += "\\";
            break;
          case '\'':
            s += "\'";
            break;
          case '\"':
            s += "\"";
            break;
          case '-':
            s += "-";
            break;
          case 'b':
            s += "\b";
            break;
          case 'f':
            s += "\f";
            break;
          case 'n':
            s += "\n";
            break;
          case 'r':
            s += "\r";
            break;
          case 't':
            s += "\t";
            break;
          case 'x': {
            //\xHH
            v1 = in.charAt(a + 2);
            v2 = in.charAt(a + 3);
            if ((v1 >= '0') && (v1 <= '9')) {
              v1 -= '0';
            } else if ((v1 >= 'a') && (v1 <= 'f')) {
              v1 -= ('a' - 10);
            } else if ((v1 >= 'A') && (v1 <= 'F')) {
              v1 -= ('A' - 10);
            } else {
              s += c;
              continue;
            };
            if ((v2 >= '0') && (v2 <= '9')) {
              v2 -= '0';
            } else if ((v2 >= 'a') && (v2 <= 'f')) {
              v2 -= ('a' - 10);
            } else if ((v2 >= 'A') && (v2 <= 'F')) {
              v2 -= ('A' - 10);
            } else {
              s += c;
              continue;
            };
            x = v1 * 16 + v2;
            s += (char) x;
            a += 3;
            continue;
          }
        }
        a++;
      } else {
        s += c;
      }
    }
    return s;
  }

  String makestr(String str) {
    String ret = "";
    char ch;
    for (int a = 0; a < str.length(); a++) {
      ch = str.charAt(a);
      if (ch < ' ') {
        ret = ret + "\\x" + Integer.toHexString(ch);
      } else {
        ret = ret + ch;
      }
    }
    return ret;
  }
}
