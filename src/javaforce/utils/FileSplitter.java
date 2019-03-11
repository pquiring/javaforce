package javaforce.utils;

/*/

 File splitter

 Usage : filein fileout1 fileout2 [size_of_fileout1]

 /*/
import java.io.*;
import java.util.Random;

import javaforce.*;

public class FileSplitter {

  public static void main(String args[]) {
    FileSplitter x = new FileSplitter();
    x.main2(args);
  }
  final int BUFSIZ = (64 * 1024); //buffer size

  void usage() {
    System.out.println("File splitter utility");
    System.out.println("Usage : jfs filein fileout1 fileout2 [size]");
    System.out.println("  size = size of fileout1 (default = 1/2 of filein)");
    System.exit(0);
  }

  void error(String msg) {
    System.out.println("Error : " + msg);
    System.exit(1);
  }

  void main2(String args[]) {
    ParseArgs pa = new ParseArgs();

    pa.arg_decoderefs = false;  //do not process @ref files
    pa.arg_parse(args);

    if (pa.arg_names.size() < 3) {
      usage();
    }

    String fni, fno1, fno2;
    FileInputStream fi;
    FileOutputStream fo1, fo2;

    fni = pa.arg_names.get(0);
    fno1 = pa.arg_names.get(1);
    fno2 = pa.arg_names.get(2);

    int size = 0;

    if (pa.arg_names.size() == 4) {
      size = JF.atoi(pa.arg_names.get(3));
    }

    byte buf[] = new byte[BUFSIZ];

    fi = JF.fileopen(fni);
    if (fi == null) {
      JF.msg("Unable to open : " + fni);
      return;
    }

    int ifs = JF.filelength(fi);

    if (size > ifs) {
      JF.msg("size > sizeof(filein)");
      return;
    }
    if (size == 0) {
      size = ifs / 2;
    }

    fo1 = JF.filecreate(fno1);
    fo2 = JF.filecreate(fno2);

    //write fileout1
    int os = 0;
    int read;
    while (os < size) {
      if (os + BUFSIZ < size) {
        read = JF.read(fi, buf, 0, BUFSIZ);
      } else {
        read = JF.read(fi, buf, 0, size - os);
      }
      if (!JF.write(fo1, buf, 0, read)) {
        JF.msg("write failed on fileout1");
        return;
      }
      os += read;
    }

    //write fileout2
    size = ifs - size;
    os = 0;
    while (os < size) {
      if (os + BUFSIZ < size) {
        read = JF.read(fi, buf, 0, BUFSIZ);
      } else {
        read = JF.read(fi, buf, 0, size - os);
      }
      if (!JF.write(fo2, buf, 0, read)) {
        JF.msg("write failed on fileout2");
        return;
      }
      os += read;
    }

    JF.msg("Ok!");
  }
}
