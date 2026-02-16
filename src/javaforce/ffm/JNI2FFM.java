package javaforce.ffm;

/** JNI to FFM conversion.
 *
 * Generates FFM code from JNI class.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class JNI2FFM {
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

    boolean flag_nofreestring = false;
    boolean flag_nocopyback = false;

    try {

      src.append("package javaforce.ffm;\n");
      src.append("\n");
      src.append("/** " + basecls + " FFM implementation.\n");
      src.append(" * NON-AI MACHINE GENERATED CODE - DO NOT EDIT\n");
      src.append(" */\n");
      src.append("\n");
      src.append("import java.lang.foreign.*;\n");
      src.append("import java.lang.invoke.*;\n");
      src.append("import static java.lang.foreign.ValueLayout.*;\n");
      src.append("\n");
      src.append("import javaforce.*;\n");
      src.append("import javaforce.ffm.*;\n");
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
        ln = ln.trim();
        if (ln.equals("@NoFreeString")) {
          flag_nofreestring = true;
          continue;
        }
        if (ln.equals("@NoCopyBack")) {
          flag_nocopyback = true;
          continue;
        }
        if (!ln.startsWith("public native")) continue;
        //public native void glActiveTexture(int i1);
        ln = ln.substring(14, ln.length() - 1);
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
        if (!isRetVoid) {
          if (isRetArray) {
            method.append(java_ret_type + "[] _ret_value_ = FFM.toArray" + capitalize(java_ret_type) + "((MemorySegment)");
          } else {
            if (java_ret_type.equals("String")) {
              if (flag_nofreestring) {
                method.append("String _ret_value_ = FFM.getStringNoFree((MemorySegment)");
              } else {
                method.append("String _ret_value_ = FFM.getString((MemorySegment)");
              }
            } else {
              method.append(java_ret_type + " _ret_value_ = (" + java_ret_type + ")");
            }
          }
        }
        method.append(func_name);
        method.append(".invokeExact(");

        ctor2.append("    " + func_name + " = ffm.getFunctionPtr(\"_" + func_name + "\", ffm.getFunctionDesciptor");
        boolean first = true;
        boolean first_ctor = true;
        if (isRetVoid) {
          ctor2.append("Void(");
        } else {
          ctor2.append("(");
          if (isRetArray) {
            ctor2.append("ADDRESS");
          } else {
            ctor2.append(ValueLayout_ret_type);
          }
          first_ctor = false;
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
          if (first) {
            first = false;
          } else {
            src.append(",");
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
            arrays.append("MemorySegment " + segment_name + " = FFM.toMemory(arena, " + arg_name + ");");
            method.append(segment_name);
            arena_needed = true;
          } else {
            if (java_type.equals("MediaIO")) {
              method.append("FFM.toMemory(arena, ");
              method.append(arg_name + ".store(");
              method.append("new MemorySegment[] {");
              method.append("ffm.getFunctionUpCall(" + arg_name + ", \"read\", int.class, new Class[] {MemorySegment.class, int.class}, arena)");
              method.append(", ffm.getFunctionUpCall(" + arg_name + ", \"write\", int.class, new Class[] {MemorySegment.class, int.class}, arena)");
              method.append(", ffm.getFunctionUpCall(" + arg_name + ", \"seek\", long.class, new Class[] {long.class, int.class}, arena)");
              method.append("}))");
              arena_needed = true;
            } else if (java_type.equals("FolderListener")) {
              method.append("ffm.getFunctionUpCall(" + arg_name + ", \"folderChangeEvent\", void.class, new Class[] {MemorySegment.class, MemorySegment.class}, arena)");
              arena_needed = true;
            } else if (java_type.equals("UIEvents")) {
              method.append(arg_name + ".store(");
              method.append("ffm.getFunctionUpCall(" + arg_name + ", \"dispatchEvent\", void.class, new Class[] {int.class, int.class, int.class}, arena)");
              method.append(")");
              arena_needed = true;
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
          method.append("));");
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
        if (!flag_nocopyback) {
          for(String arg_name : array_names) {
            String segment_name = "_array_" + arg_name;
            method.append("FFM.copyBack(" + segment_name + "," + arg_name + ");");
          }
        }
        if (!isRetVoid) {
          method.append("return _ret_value_;");
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
        ctor2.append("));\n");
        if (!isDup) {
          ctor.append(ctor2);
        }
        flag_nofreestring = false;
        flag_nocopyback = false;
      }
      ctor.append("    return true;\n");
      ctor.append("  }\n");
      src.append("\n");
      src.append(ctor);
      src.append("}\n");

    } catch (Exception e) {
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
