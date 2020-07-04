package jfcontrols.logic;

/** LogicRung
 *
 * @author pquiring
 */

public class LogicRung {
  public LogicBlock root;
  public void execute(LogicPos pos) throws Exception {
    pos.block = root;
    pos.enabled = true;
    LogicBlock blk;
    while (pos.block != null) {
      blk = pos.block;
      if (blk.debug_en_idx != -1) {
        blk.func.debug_en[blk.debug_en_idx][0] = pos.enabled;
      }
      pos.enabled = blk.execute(pos.enabled);
      if (blk.debug_en_idx != -1) {
        blk.func.debug_en[blk.debug_en_idx][1] = pos.enabled;
      }
      if (blk.func.debug) {
        blk.updateDebugTags();
      }
      blk.moveNext(pos);
    }
  }
  public LogicRung next;
  public void moveNext(LogicPos pos) {
    pos.rung = next;
  }
}
