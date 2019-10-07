package javaforce.voip;

/**
 * Generates DTMF tones for local playback.
 */

public class DTMF {

  private static final double vol = 9000.0;
  private static final double pi2 = Math.PI * 2.0;

  private int offset;
  private double rowPos, colPos;
  private double row[] = new double[] {697, 770, 852, 941};
  private double col[] = new double[] {1209, 1336, 1477};
  private int iRate;
  private double dRate;
  private short buf[];
  private char last;

  public DTMF(int rate) {
    setSampleRate(rate);
  }

  public void reset() {
    offset = 0;
    rowPos = 0;
    colPos = 0;
  }

  public void setSampleRate(int rate) {
    iRate = rate;
    dRate = rate;
    buf = new short[rate / 50];  //create 20ms buffer
  }

  public short[] getSamples(char dtmf) {
    if (dtmf != last) {
      last = dtmf;
      reset();
    }
    int rowIdx, colIdx;
    switch (dtmf) {
      case '1':
        rowIdx = 0;
        colIdx = 0;
        break;
      case '2':
        rowIdx = 0;
        colIdx = 1;
        break;
      case '3':
        rowIdx = 0;
        colIdx = 2;
        break;
      case '4':
        rowIdx = 1;
        colIdx = 0;
        break;
      case '5':
        rowIdx = 1;
        colIdx = 1;
        break;
      case '6':
        rowIdx = 1;
        colIdx = 2;
        break;
      case '7':
        rowIdx = 2;
        colIdx = 0;
        break;
      case '8':
        rowIdx = 2;
        colIdx = 1;
        break;
      case '9':
        rowIdx = 2;
        colIdx = 2;
        break;
      case '*':
        rowIdx = 3;
        colIdx = 0;
        break;
      case '0':
        rowIdx = 3;
        colIdx = 1;
        break;
      case '#':
        rowIdx = 3;
        colIdx = 2;
        break;
      default:
        return null;
    }
    //row
    double rowTheta = pi2 * row[rowIdx] / dRate;
    for (int a = 0; a < buf.length; a++) {
      buf[a] = (short)(Math.sin(rowPos) * vol);
      rowPos += rowTheta;
    }
    //col
    double colTheta = pi2 * col[colIdx] / dRate;
    for (int a = 0; a < buf.length; a++) {
      buf[a] += (short) (Math.sin(colPos) * vol);
      colPos += colTheta;
    }

    offset += buf.length;
    if (offset == iRate) reset(); //1 sec complete
    return buf;
  }
}
