package jfcontrols.functions;

/** Compiler
 *
 * @author pquiring
 */

import java.util.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.controls.*;

import jfcontrols.logic.*;
import jfcontrols.panels.*;
import jfcontrols.tags.*;
import jfcontrols.db.*;

public class FunctionCompiler {
  public static String error;
  public static LogicFunction generateFunction(int fid, long revision) {
    error = null;

    LogicFunction func = new LogicFunction();
    func.id = fid;
    func.revision = revision;

    //append code from jfc_rungs
    RungRow rungs[] = Database.getRungsById(fid, true);
    int norungs = rungs.length;
    Node.resetDebug();
    LogicRung rung = null;
    for(int rid=0;rid<norungs;rid++) {
      if (rung == null) {
        rung = new LogicRung();
        func.root = rung;
      } else {
        rung.next = new LogicRung();
        rung = rung.next;
      }
      LogicBlock logic = new Wire();
      logic.func = func;
      logic.rung = rung;
      rung.root = logic;
      String logics[] = rungs[rid].logic.split("[|]");
      BlockRow blocks[] = Database.getRungBlocksById(fid, rid);
      NodeRoot root = buildNodes(fid, rid, logics, blocks);
      if (root == null) return null;
      Node.resetStack();
      Node node = root;
      while (node != null) {
        logic.next = node.getLogic();
        if (logic.next == null) {
          JFLog.log("Error:logic==null for node:" + node.toString());
        }
        logic = logic.next;
        logic.func = func;
        logic.rung = rung;
        node = node.next;
      }
      if (Node.getStackSize() > 0) {
        Node.resetStack();
        error = "Error:Unclosed flow blocks";
        return null;
      }
    }
    func.debug_en = new boolean[Node.debug_en_idx][2];
    func.debug_tv = new String[Node.debug_tv_idx];
    return func;
  }
  public static NodeRoot buildNodes(int fid, int rid, String logic[], BlockRow blocks[]) {
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
          int bid = Integer.valueOf(part);
          for(int a=0;a<blocks.length;a++) {
            if (blocks[a].bid == bid) {
              name = blocks[a].name;
              tags = blocks[a].tags;
              break;
            }
          }
          if (name == null) {
            error = "Error:Block not found:rid=" + rid + ":bid=" + part;
            return null;
          }
          LogicBlock blk = null;
          try {
            Class cls = Class.forName("jfcontrols.logic." + name.toUpperCase());
            Constructor ctor = cls.getConstructor();
            blk = (LogicBlock)ctor.newInstance();
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
