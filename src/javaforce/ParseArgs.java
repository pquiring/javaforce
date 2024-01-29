/*

 Arguments Parser

 v1.0.0 - based on dfparse.hpp/1.2.8 from DF

 Usage :
 arg_parse(String args[]);
 then the following will be filled out:
 Vector<String> arg_opts; //Options (ie: /XXX[=yyy])
 Vector<String> arg_vals; //Options set value (ie: /xxx=YYY) ["" if none]
 Vector<String> arg_names; //filenames/commands (ie: XXX)
 you can also parse files:
 arg_parse_file(InputStream);
 */
package javaforce;

import javaforce.*;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Vector;

/**
 * This is a simple class for parsing command line options.
 *
 * @author Peter Quiring
 */
public class ParseArgs extends JF {

  public Vector<String> arg_opts;
  public Vector<String> arg_vals;
  public Vector<String> arg_names;
  public boolean arg_decoderefs = true;  //set to false to ignore @reffiles
  public boolean arg_stripenter = true;  //set to false to keep ENTER codes in arguments (see \examples\winmsg)
  public boolean arg_decodeopts = true;  //set to false to use arg_names only

  public ParseArgs() {
    arg_opts = new Vector<String>();
    arg_vals = new Vector<String>();
    arg_names = new Vector<String>();
  }

  public void arg_parse(String[] args) {
    for (int a = 0; a < args.length; a++) {
      arg_parse_element(args[a].toCharArray());
    }
  }

  public void arg_parse_file(InputStream is) {
    char[] str;
    while (!eof(is)) {
      str = arg_readln(is); //read one line (removes leading/trailing spaces)
      if ((str != null) && (str.length > 0)) {
        arg_parse_string(str);
      }
    }
  }

//private members
  private void arg_parse_element(char[] str) {
    FileInputStream fis;
    char[] tmp;

    if (str.length == 0) {
      arg_names.add(createString(str));
      return;
    }

    if ((str[0] == '-') && (arg_decodeopts)) {
      int p = 0;
      for (int a = 0; a < str.length; a++) {
        if (str[a] == ':') {
          str[a] = '=';
        }
      }
      //option (arg_opts)
      p++;
      tmp = null;
      while ((p < str.length) && (str[p] != '=')) {
        tmp = strcat(tmp, str[p++]);
      }
      if (tmp == null) {
        return;  //bad arg
      }
      if ((p < str.length) && (str[p] == '=')) {
        //do equals thingy
        arg_opts.add(createString(tmp));
        p++;
        tmp = null;
        while (p < str.length) {
          tmp = strcat(tmp, str[p++]);
        }
        if (tmp == null) {
          arg_vals.add("");
        } else {
          arg_vals.add(createString(tmp));
        }
      } else {
        //check if /xxx[- or +] option
        switch (tmp[tmp.length - 1]) {
          case '-':
            tmp = truncstr(tmp, tmp.length - 1);
            arg_opts.add(createString(tmp));
            arg_vals.add("F");
            break;
          case '+':
            tmp = truncstr(tmp, tmp.length - 1);
            arg_opts.add(createString(tmp));
            arg_vals.add("T");
            break;
          default:
            arg_opts.add(createString(tmp));
            arg_vals.add("");
            break;
        }
      }
    } else if ((str[0] == '@') && (arg_decoderefs)) {
      //reference file
      tmp = new char[str.length - 1];
      for (int a = 1; a < str.length; a++) {
        tmp[a - 1] = str[a];
      }
      fis = fileopen(createString(tmp));
      if (fis == null) {
        return;  //can't open file
      }
      arg_parse_file(fis);
    } else {
      arg_names.add(createString(str));
    }
  }

  private void arg_parse_string(char[] str) {
    boolean quote = false;
    int p = 0;
    char[] tmp;
    while (p < str.length) {
      tmp = null;
      while (p < str.length) {
        if (str[p] == '\"') {
          if (quote) {
            quote = false;
          } else {
            quote = true;
          }
        }
        if ((!quote) && (str[p] == ' ')) {
          p++;
          break;
        }
        tmp = strcat(tmp, str[p++]);
      }
      arg_parse_element(tmp);
    }
  }

  private char[] arg_readln(InputStream fis) {
    char[] ret = null;
    char ch;
    while (!eof(fis)) {
      ch = readchar(fis);
      if (arg_stripenter) {
        if (ch == 13) {
          continue;
        }
        if (ch == 10) {
          break;  //End of line (and option) (strip \n)
        }
      }
      ret = strcat(ret, ch);
    }
    return ret;
  }
}
