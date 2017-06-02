package jfcontrols.functions;

/** Compiler
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

import jfcontrols.logic.*;
import jfcontrols.panels.*;

public class FunctionCompiler {
  public static String error;
  public static String generateFunction(int fid, SQL sql) {
    error = null;
    StringBuilder sb = new StringBuilder();
    sb.append("import jfcontrols.tags.*;\r\n");
    sb.append("public class func_" + fid + " extends jfcontrols.functions.FunctionRuntime {\r\n");
    sb.append("  public static boolean code(TagAddr args[]) {\r\n");
    sb.append("    boolean enabled = true;\r\n");
    sb.append("    boolean en[] = new boolean[256];\r\n");
    sb.append("    int eidx = 0;\r\n");
    sb.append("    en[eidx] = enabled;\r\n");
    sb.append("    TagAddr tags[] = new TagAddr[33];\r\n");

    //append code from rungs
    String rungs[][] = sql.select("select logic from rungs where fid=" + fid);
    int norungs = rungs.length;
    for(int rid=0;rid<norungs;rid++) {
      String rung[] = rungs[rid];
      String logic[] = rung[0].split("[|]");
      String blocks[][] = sql.select("select bid,name,tags from blocks where fid=" + fid + " and rid=" + rid + " order by bid");
      NodeRoot root = buildNodes(fid, rid, logic, blocks, sql);
      Node node = root, upper;
      String func = null;
      int cnt;
      while (node != null) {
        switch (node.type) {
          case 't':
            switch (node.next.type) {
              case 'a':
              case 'c':
                sb.append("en[eidx+1] = en[eidx];\r\n");
                sb.append("eidx++");
                break;
            }
            break;
          case 'a':
          case 'c':
            cnt = 0;
            upper = node.upper;
            while (upper != null) {
              cnt++;
              upper = upper.upper;
            }
            sb.append("en[eidx+1] = en[eidx-" + cnt + "];\r\n");
            sb.append("eidx++");
            break;
          case 'b':
            //nothing
            break;
          case 'd':
            upper = node.upper;
            while (upper != null) {
              sb.append("  en[eidx-1] |= en[eidx];\r\n");
              sb.append("  eidx--;\r\n");
              upper = upper.upper;
            };
            break;
          case '#': {
            for(int t=1;t<node.tags.length;t++) {
              String tag = node.tags[t];
              char type = tag.charAt(0);
              String ag = tag.substring(1);
              switch (type) {
                case 't':
                  sb.append("tags[" + t + "] = TagAddr.decode(\"" + ag + "\");\r\n");
                  break;
                case 'i':
                  sb.append("tags[" + t + "] = TagAddr.tempValue(\"" + ag + "\");");
                  break;
                case 'f':
                  func = ag;
                  break;
              }
            }
            sb.append("  enabled = en[eidx];\r\n");
            if (node.blk.getName().equals("Call")) {
              sb.append(node.blk.getCode(func));
            } else {
              sb.append(node.blk.getCode());
            }
            sb.append("  en[eidx] = enabled;\r\n");
            break;
          }
        }
        node = node.next;
      }
    }

    sb.append("    return en[eidx];\r\n");
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
            JFLog.log("Error:corrupt logic");
            return null;
          }
          x = upper.x;
          y = upper.getSegmentMaxY(node) + 1;
          nodes.add(node = node.insertLinkUpper(upper, 'a', x, y));
          break;
        }
        case "b": {
          //b can only be under t,b
          Node upper = Node.findLastOpenNode(nodes, "tb");
          if (upper == null) {
            JFLog.log("Error:corrupt logic");
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
            JFLog.log("Error:corrupt logic");
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
            JFLog.log("Error:corrupt logic");
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
            JFLog.log("Error:Block not found:rid=" + rid + ":bid=" + part + ":name=");
            continue;
          }
          Logic blk = null;
          try {
            Class cls = Class.forName("jfcontrols.logic." + name.toUpperCase());
            blk = (Logic)cls.newInstance();
          } catch (Exception e) {
            JFLog.log(e);
          }
          if (blk == null) {
            JFLog.log("Error:Block not found:rid=" + rid + ":bid=" + part);
            continue;
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
