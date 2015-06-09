package javaforce.voip;

/**
 * Generates DTMF tones for local playback.
 */
public class DTMF {

  private final double vol = 9000.0;
  private int rowOff, colOff;
  private int row[] = {697, 770, 852, 941};
  private int col[] = {1209, 1336, 1477};

  public void reset() {
    rowOff = colOff = 0;
  }

  public short[] getSamples(char dtmf) {
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
    short buf[] = new short[160];
    for (int a = 0; a < 160; a++) {
      buf[a] = (short) (Math.sin((2.0 * Math.PI / (8000.0 / row[rowIdx])) * (a + rowOff)) * vol);
    }
    rowOff += 160;
    if (rowOff == 8000) {
      rowOff = 0;
    }
    //col
    for (int a = 0; a < 160; a++) {
      buf[a] += (short) (Math.sin((2.0 * Math.PI / (8000.0 / col[colIdx])) * (a + colOff)) * vol);
    }
    colOff += 160;
    if (colOff == 8000) {
      colOff = 0;
    }
    return buf;
  }
}
