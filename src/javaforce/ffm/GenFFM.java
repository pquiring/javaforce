package javaforce.ffm;

import java.io.*;
import java.util.*;

import javaforce.*;

/** Generates FFM code from API class.
 *
 * @author pquiring
 */

public class GenFFM {
  public static void main(String[] args) {
    int idx;
    if (args.length != 5) {
      System.out.println("Usage: JNI2FFM filein.java fileout.java package(s) implements_extends baseclass");
      return;
    }

    String fin = args[0].replaceAll("\\\\", "/");
    String fout = args[1].replaceAll("\\\\", "/");
    String pack = args[2];
    String i_e = args[3];
    String basecls = args[4];

    System.out.println("Converting " + fin + " to " + fout);

    String cls_in = fin;
    idx = fin.lastIndexOf('/');
    if (idx != -1) {
      cls_in = fin.substring(idx + 1);
    }
    idx = cls_in.indexOf('.');
    cls_in = cls_in.substring(0, idx);

    String cls_out = fout;
    idx = fout.lastIndexOf('/');
    if (idx != -1) {
      cls_out = fout.substring(idx + 1);
    }
    idx = cls_out.indexOf('.');
    cls_out = cls_out.substring(0, idx);

    String[] lns;

    StringBuilder src = new StringBuilder();
    StringBuilder ctor = new StringBuilder();

    ArrayList<String> funcs = new ArrayList<>();

    try {
      FileInputStream fis = new FileInputStream(fin);
      lns = new String(fis.readAllBytes()).split("\n");
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }

    int lnidx = 0;

    try {

      src.append("package javaforce.ffm;\n");
      src.append("\n");
      src.append("import java.lang.foreign.*;\n");
      src.append("import java.lang.invoke.*;\n");
      src.append("import static java.lang.foreign.ValueLayout.*;\n");
      src.append("\n");
      src.append("import javaforce.*;\n");
      src.append("import javaforce.ffm.*;\n");
      src.append("\n");
      src.append("/** " + basecls + " FFM implementation.\n");
      src.append(" *\n");
      src.append(" * NON-AI MACHINE GENERATED CODE - DO NOT EDIT\n");
      src.append(" */\n");
      src.append("\n");
      String[] packs = pack.split(",");
      for(String p : packs) {
        src.append("import javaforce." + p + ".*;\n");
      }
      src.append("\n");
      src.append("public class " + cls_out + " " + i_e + " " + basecls + " {\n");
      src.append("\n");
//      src.append("  private Arena arena;\n");
      src.append("  private FFM ffm;\n");
      src.append("\n");
      src.append("  private static " + cls_out + " instance;\n");
      src.append("  public static " + cls_out + " getInstance() {\n");
      src.append("    if (instance == null) {\n");
      src.append("      instance = new " + cls_out + "();\n");
      src.append("      if (!instance.ffm_init()) {\n");
      src.append("        JFLog.log(\"" + cls_out + " init failed!\");\n");
      src.append("        instance = null;\n");
      src.append("      }\n");
      src.append("    }\n");
      src.append("    return instance;\n");
      src.append("  }\n");
      src.append("\n");
      ctor.append("  private boolean ffm_init() {\n");
//      ctor.append("    JFLog.log(\"" + cls_out + " init\");\n");
      ctor.append("    MethodHandle init;\n");
      ctor.append("    ffm = FFM.getInstance();\n");
//      ctor.append("    arena = Arena.ofAuto();\n");
      ctor.append("    init = ffm.getFunction(\"" + basecls + "init\", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));\n");
      ctor.append("    if (init == null) return false;\n");
      ctor.append("    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}\n");
      ctor.append("\n");
      for(String ln : lns) {
        lnidx++;
        ln = ln.trim();
        String[] f = ln.split(" +");
        if (!f[0].equals("public")) continue;
        if (f.length < 2) continue;
        switch (f[1]) {
          case "interface":
          case "static":
          case "class":
          case "synchronized":
            continue;
          case "native":
            ln = ln.substring(14, ln.length() - 1);  //remove "public native" and ";" at end
            break;
          default:
            ln = ln.substring(7, ln.length() - 1);  //remove "public " and ";" at end
            break;
        }
        idx = ln.indexOf(' ');
        String java_ret_type = ln.substring(0, idx);
        String ValueLayout_ret_type = null;
        if (java_ret_type.equals("String")) {
          ValueLayout_ret_type = "ADDRESS";
        } else {
          ValueLayout_ret_type = "JAVA_" + java_ret_type.toUpperCase();
        }
        boolean isRetVoid = java_ret_type.equals("void");
        boolean isRetArray = java_ret_type.endsWith("[]");
        if (isRetArray) {
          java_ret_type = java_ret_type.substring(0, java_ret_type.length() - 2);
        }
        ln = ln.substring(idx + 1);
        idx = ln.indexOf('(');
        String func_name = ln.substring(0, idx);
        boolean isDup = false;
        if (funcs.contains(func_name)) {
          isDup = true;
        } else {
          funcs.add(func_name);
        }
        String func_args = ln.substring(idx);
        String[] pairs = func_args.substring(1, func_args.length() - 1).split(",");
        if (!isDup) {
          src.append("  private MethodHandle " + func_name + ";\n");
        }
        src.append("  public " + java_ret_type);
        if (isRetArray) {
          src.append("[]");
        }
        src.append(" " + func_name + "(");  //add args

        StringBuilder ctor2 = new StringBuilder();

        StringBuilder method = new StringBuilder();
        StringBuilder arrays = new StringBuilder();
        method.append("{ try { ");
        boolean arena_needed = false;
        int before_invoke = method.length();
        if (!isRetVoid) {
          if (isRetArray) {
            method.append("FFM.createFFMArray();");
          } else {
            if (java_ret_type.equals("String")) {
              method.append("String _ret_value_ = FFM.getString((MemorySegment)");
            } else {
              method.append(java_ret_type + " _ret_value_ = (" + java_ret_type + ")");
            }
          }
        }
        boolean first_method = true;
        boolean first_src = true;
        boolean first_ctor = true;
        method.append(func_name);
        method.append(".invokeExact(");

        ctor2.append("    " + func_name + " = ffm.getFunctionPtr");
        ctor2.append("(\"_" + func_name + "\", ffm.getFunctionDesciptor");
        if (isRetVoid) {
          ctor2.append("Void(");
        } else {
          if (isRetArray) {
            ctor2.append("Void");
          }
          ctor2.append("(");
          if (isRetArray) {
            //nop
          } else {
            ctor2.append(ValueLayout_ret_type);
            first_ctor = false;
          }
        }
        ArrayList<String> array_names = new ArrayList<>();
        for(String pair : pairs) {
          pair = pair.trim();
          if (pair.length() == 0) continue;
          idx = pair.indexOf(' ');
          String java_type = pair.substring(0, idx);
          String ValueLayout_type = null;
          String arg_name = pair.substring(idx + 1);
          boolean isArray = false;
          String array_type = null;
          if (java_type.endsWith("[]")) {
            isArray = true;
            String base_type = java_type.substring(0, java_type.length() - 2);
            ValueLayout_type = "ADDRESS";
            array_type = "JAVA_" + base_type.toUpperCase();
          } else {
            if (isPrimitiveType(java_type)) {
              ValueLayout_type = "JAVA_" + java_type.toUpperCase();
            } else {
              ValueLayout_type = "ADDRESS";
            }
          }
          if (first_src) {
            first_src = false;
          } else {
            src.append(",");
          }
          if (first_method) {
            first_method = false;
          } else {
            method.append(",");
          }
          if (first_ctor) {
            first_ctor = false;
          } else {
            ctor2.append(",");
          }
          src.append(java_type + " " + arg_name);
          if (isArray) {
            array_names.add(arg_name);
            String segment_name = "_array_" + arg_name;
            arrays.append("MemorySegment " + segment_name + " = FFM.toMemory(");
            arrays.append("arena, ");
            arena_needed = true;
            arrays.append(arg_name + ");");
            method.append(segment_name);
          } else {
            if (java_type.equals("MediaIO")) {
              method.append("FFM.upcall_MediaIO");
              method.insert(before_invoke, "FFM.setMediaIO(" + arg_name + ");");
            } else if (java_type.equals("FolderListener")) {
              method.append("FFM.upcall_FolderListener_folderChangeEvent");
              method.insert(before_invoke, "FFM.setFolderListener(" + arg_name + ");");
            } else if (java_type.equals("UIEvents")) {
              method.append("FFM.upcall_UIEvents_dispatchEvent");
              method.insert(before_invoke, "FFM.setUIEvents(" + arg_name + ");");
            } else if (java_type.equals("String")) {
              method.append("arena.allocateFrom(" + arg_name + ")");
              arena_needed = true;
            } else {
              method.append(arg_name);
            }
          }
          ctor2.append(ValueLayout_type);
        }
        if (isRetArray) {
          method.append(");");
        } else {
          if (java_ret_type.equals("String")) {
            method.append("));");
          } else {
            method.append(");");
          }
        }
        if (arrays.length() > 0) {
          //insert arrays after "{ try { "
          method.insert(8, arrays);
        }
        if (arena_needed) {
          //insert areana after "{ try { "
          method.insert(8, "Arena arena = Arena.ofAuto(); ");
        }
        for(String arg_name : array_names) {
          String segment_name = "_array_" + arg_name;
          method.append("FFM.copyBack(" + segment_name + "," + arg_name + ");");
        }
        if (!isRetVoid) {
          method.append("return ");
          if (isRetArray) {
            method.append("(");
            method.append(java_ret_type);
            method.append("[])");
            method.append("FFM.getArray()");
          } else {
            method.append("_ret_value_");
          }
          method.append(";");
        }
        method.append(" } catch (Throwable t) { JFLog.log(t); ");
        if (!isRetVoid) {
          //must return a value
          method.append(" return ");
          if (isRetArray) {
            method.append("null");
          } else {
            switch (java_ret_type) {
              case "String": method.append("null"); break;
              case "boolean": method.append("false"); break;
              default: method.append("-1"); break;
            }
          }
          method.append(";");
        }
        method.append("} }\n");
        src.append(") ");
        src.append(method);  //{ invoke... }
        src.append("\n");
        ctor2.append(")");
        ctor2.append(");\n");
        if (!isDup) {
          ctor.append(ctor2);
        }
      }
      ctor.append("    return true;\n");
      ctor.append("  }\n");
      src.append("\n");
      src.append(ctor);
      src.append("}\n");

    } catch (Exception e) {
      JFLog.log("Exception on Line:" + lnidx);
      JFLog.log(e);
      return;
    }

    try {
      FileOutputStream fos = new FileOutputStream(fout);
      fos.write(src.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
  }
  private static String capitalize(String in) {
    char first_cap = Character.toUpperCase(in.charAt(0));
    return first_cap + in.substring(1);
  }
  private static boolean isPrimitiveType(String type) {
    switch (type) {
      case "boolean": return true;
      case "char": return true;
      case "byte": return true;
      case "short": return true;
      case "int": return true;
      case "long": return true;
      case "float": return true;
      case "double": return true;
      default: return false;
    }
  }
}
