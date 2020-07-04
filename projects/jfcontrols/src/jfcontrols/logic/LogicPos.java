package jfcontrols.logic;

/** Logic Position
 *
 * @author pquiring
 */

import java.util.*;

public class LogicPos {
  public LogicPos[] stack;
  public int stackpos, stackmax;
  public LogicFunction func;
  public LogicRung rung;
  public LogicBlock block;
  public boolean enabled;

  public LogicPos() {}
  public LogicPos(int stacksize) {
    stack = new LogicPos[stacksize];
    for(int a=0;a<stacksize;a++) {
      stack[a] = new LogicPos();
    }
    stackmax = stacksize;
  }

  public void push() {
    LogicPos pos = new LogicPos();
    pos.func = func;
    pos.rung = rung;
    pos.block = block;
    stack[stackpos++] = pos;
  }
  public void pop() {
    LogicPos pos = stack[--stackpos];
    func = pos.func;
    rung = pos.rung;
    block = pos.block;
  }
}
