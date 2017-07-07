package jfcontrols.functions;

/** Compiler
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.controls.*;

import jfcontrols.logic.*;
import jfcontrols.panels.*;
import jfcontrols.tags.*;

public class FunctionCompiler {
  public static String error;
  public static String generateFunction(int fid, SQL sql) {
    TagsCache tags = new TagsCache();
    error = null;
    StringBuilder sb = new StringBuilder();
    String revision = sql.select1value("select revision from jfc_funcs where id=" + fid);
    sb.append("import jfcontrols.tags.*;\r\n");
    sb.append("public class func_" + fid + " extends jfcontrols.functions.FunctionRuntime {\r\n");
    String blks[][] = sql.select("select rid,bid,tags from jfc_blocks where fid=" + fid);
    sb.append("  public static boolean debug_en[][] = new boolean[" + blks.length + "][2];\r\n");
    int tagcount = 0;
    for(int a=0;a<blks.length;a++) {
      tagcount += blks[a][2].split(",").length;
    }
    sb.append("  public static String debug_tv[] = new String[" + tagcount + "];\r\n");
    sb.append("  public static long revision = " + revision + ";\r\n");
    sb.append("  public boolean code(TagBase args[]) {\r\n");
    sb.append("    boolean enabled = true;\r\n");
    sb.append("    boolean en[] = new boolean[256];\r\n");
    sb.append("    int eidx = 0;\r\n");
    sb.append("    TagBase tags[] = new TagBase[33];\r\n");

    //append code from jfc_rungs
    String rungs[][] = sql.select("select logic from jfc_rungs where fid=" + fid + " order by rid");
    int norungs = rungs.length;
    ArrayList<String> stack = new ArrayList<>();
    int debug_en = 0;
    int debug_tv = 0;
    for(int rid=0;rid<norungs;rid++) {
      sb.append("//Rung #" + (rid+1) + "\r\n");
      sb.append("    enabled = true;\r\n");
      sb.append("    en[0] = enabled;\r\n");
      int pos = sb.length();
      String rung[] = rungs[rid];
      String logic[] = rung[0].split("[|]");
      String blocks[][] = sql.select("select bid,name,tags from jfc_blocks where fid=" + fid + " and rid=" + rid + " order by bid");
      NodeRoot root = buildNodes(fid, rid, logic, blocks, sql);
      if (root == null) return null;
      Node node = root, upper;
      String func = null;
      int cnt;
      int eidx = 0;
      while (node != null) {
        node.eidx = eidx;
        switch (node.type) {
          case 't':
            sb.append("    en[" + (eidx+1) + "] = en[" + eidx + "];\r\n");
            eidx++;
            break;
          case 'a':
          case 'c':
            cnt = 1;
            upper = node.upper;
            while (upper != null) {
              cnt++;
              upper = upper.upper;
            }
            sb.append("    en[" + eidx + "] = en[" + (eidx-cnt) + "];\r\n");
            break;
          case 'b':
            sb.append("    en[" + (eidx+1) + "] = en[" + eidx + "];\r\n");
            eidx++;
            break;
          case 'd':
            upper = node.upper;
            while (upper != null) {
              sb.append("    en[" + upper.eidx + "] |= en[" + eidx + "];\r\n");
              eidx = upper.eidx;
              upper = upper.upper;
            };
            sb.append("    en[" + (eidx-1) + "] = en[" + eidx + "];\r\n");
            eidx--;
            break;
          case '#': {
            if (node.blk.isFlowControl()) {
              String name = node.blk.getName();
              if (name.endsWith("_END")) {
                if (stack.size() == 0) {
                  error = "Error:" + name + " without starting block";
                  return null;
                }
                String start = stack.remove(stack.size() - 1);
                if (!node.blk.canClose(start)) {
                  error = "Error:" + name + " unmatching block";
                  return null;
                }
              } else {
                stack.add(name);
              }
            }
            int types[] = new int[node.tags.length];
            boolean array[] = new boolean[node.tags.length];
            boolean unsigned[] = new boolean[node.tags.length];
            for(int t=1;t<node.tags.length;t++) {
              String tag = node.tags[t];
              char type = tag.charAt(0);
              String value = tag.substring(1);
              int tagType = node.blk.getTagType(t);
              switch (type) {
                case 't':
                  sb.append("    tags[" + t + "] = getTag(\"" + value + "\");\r\n");
                  TagBase tagbase = tags.getTag(value);
                  if (tagType >= TagType.any) {
                    types[t] = tagbase.getType();
                  } else {
                    types[t] = tagType;
                  }
                  array[t] = tagbase.isArray();
                  unsigned[t] = tagbase.isUnsigned();
                  break;
                case 'i':
                  if (value.indexOf(".") == -1)
                    tagType = TagType.int32;
                  else
                    tagType = TagType.float32;
                  sb.append("    tags[" + t + "] = new TagTemp(" + tagType + ",\"" + value + "\");\r\n");
                  types[t] = tagType;
                  break;
                case 'f':
                  func = value;
                  types[t] = TagType.function;
                  break;
              }
            }
            sb.append("    enabled = en[" + eidx + "];\r\n");
            sb.append("    debug_en[" + debug_en + "][0]=enabled;\r\n");
            if (node.blk.getName().equals("CALL")) {
              sb.append(node.blk.getCode(func));
            } else {
              sb.append(node.blk.getCode(types, array, unsigned));
              String preCode = node.blk.getPreCode();
              if (preCode != null) {
                sb.insert(pos, preCode);
              }
            }
            for(int a=1;a<node.tags.length;a++) {
              sb.append("    debug_tv[" + debug_tv++ + "]=tags[" + a + "].getValue();\r\n");
            }
            sb.append("    debug_en[" + debug_en + "][1]=enabled;\r\n");
            debug_en++;
            sb.append("    en[" + eidx + "] = enabled;\r\n");
            break;
          }
        }
        node = node.next;
      }
    }

    if (stack.size() > 0) {
      error = "Error:Unclosed block:" + stack.get(stack.size() - 1);
      return null;
    }

    sb.append("    return en[0];\r\n");
    sb.append("  }\r\n");
    sb.append("}\r\n");
    return sb.toString();
  }
  public static NodeRoot buildNodes(int fid, int rid, String logic[], String blocks[][], SQL sql) {
    int x = 0;
    int y = 0;
    ArrayList<Node> nodes = new ArrayList<Node>();
    NodeRoot root = new NodeRoot(fid, rid);

    Node node = root;
    for(int p=0;p<logic.length;p++) {
      String part = logic[p];
      switch (part) {
        case "t": {
          nodes.add(node = node.insertNode('t', x, y));
          x++;
          break;
        }
        case "h":
          nodes.add(node = node.insertNode('h', x, y));
          x++;
          break;
        case "v":
          JFLog.log("Error:'v' found in logic");
          nodes.add(node = node.insertNode('v', x, y));
          y++;
          break;
        case "a": {
          //a can only be under t,a
          Node upper = Node.findFirstOpenNode(nodes, "ta");
          if (upper == null) {
            error = "Error:corrupt logic";
            return null;
          }
          x = upper.x;
          y = upper.getSegmentMaxY(node) + 1;
          nodes.add(node = node.insertLinkUpper(upper, 'a', x, y));
          break;
        }
        case "b": {
          //b can only be under t,b
          Node upper = Node.findFirstOpenNode(nodes, "tb");
          if (upper == null) {
            error = "Error:corrupt logic";
            return null;
          }
          if (upper.x < x) upper.x = x;
          if (upper.x > x) x = upper.x;
          nodes.add(node = node.insertLinkUpper(upper, 'b', x, y));
          break;
        }
        case "c": {
          //c can only be under t,a
          Node upper = Node.findFirstOpenNode(nodes, "ta");
          if (upper == null) {
            error = "Error:corrupt logic";
            return null;
          }
          x = upper.x;
          y = upper.getSegmentMaxY(node) + 1;
          nodes.add(node = node.insertLinkUpper(upper, 'c', x, y));
          break;
        }
        case "d": {
          //d can only be under t,b
          Node upper = Node.findFirstOpenNode(nodes, "tb");
          if (upper == null) {
            error = "Error:corrupt logic";
            return null;
          }
          if (upper.x < x) upper.x = x;
          if (upper.x > x) x = upper.x;
          nodes.add(node = node.insertLinkUpper(upper, 'd', x, y));
          break;
        }
        default: {
          String name = null;
          String tags = null;
          for(int a=0;a<blocks.length;a++) {
            if (blocks[a][0].equals(part)) {
              name = blocks[a][1];
              tags = blocks[a][2];
              break;
            }
          }
          if (name == null) {
            error = "Error:Block not found:rid=" + rid + ":bid=" + part;
            return null;
          }
          Logic blk = null;
          try {
            Class cls = Class.forName("jfcontrols.logic." + name.toUpperCase());
            blk = (Logic)cls.newInstance();
          } catch (Exception e) {
            JFLog.log(e);
          }
          if (blk == null) {
            error = "Error:Block not found:rid=" + rid + ":bid=" + part;
            return null;
          }
          nodes.add(node = node.insertLogic('#', x, y, blk, tags.split(",")));
          x+=3;
          break;
        }
      }
    }
    return root;
  }
}
