package javaforce.ansi.server.games;

/** ANSI Tetris
 *
 * @author pquiring
 */

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import javaforce.*;
import javaforce.ansi.server.*;

public class Tetris extends TimerTask implements KeyEvents {
  private ANSI ansi;
  private int height;
  private int width;
  private int padding;
  private int[][] lines;
  private int score;
  private int thisPiece, nextPiece;
  private Random r;
  private Timer timer;
  private int posx;
  private int posy;
  private int rotation;
  private boolean active;
  private boolean gameover;
  private boolean rotate;
  private boolean moveLeft, moveRight, moveDown;
  private boolean pause;
  private boolean toggle;
  private boolean redraw;
  private boolean refresh;
  private int[] colors = {
    Color.red.getRGB(),
    Color.green.getRGB(),
    Color.blue.getRGB(),
    Color.yellow.getRGB(),
    Color.pink.getRGB(),
    Color.orange.getRGB(),
    Color.magenta.getRGB(),
    Color.cyan.getRGB()
  };
  private int[] maxShift = {3, 1, 1, 1, 1, 1, 1, 1};
  private int[][][] shapes = {  //[piece] [y] [x]
    //'I', 'L', 'J', 'T', 'S', 'Z', 'O', 'V'
    {{1,0,0,0}, {1,0,0,0}, {1,0,0,0}, {1,0,0,0}},
    {{1,0,0,0}, {1,0,0,0}, {1,1,0,0}, {0,0,0,0}},
    {{0,1,0,0}, {0,1,0,0}, {1,1,0,0}, {0,0,0,0}},
    {{1,1,1,0}, {0,1,0,0}, {0,0,0,0}, {0,0,0,0}},
    {{0,1,1,0}, {1,1,0,0}, {0,0,0,0}, {0,0,0,0}},
    {{1,1,0,0}, {0,1,1,0}, {0,0,0,0}, {0,0,0,0}},
    {{1,1,0,0}, {1,1,0,0}, {0,0,0,0}, {0,0,0,0}},
    {{1,1,0,0}, {1,0,0,0}, {0,0,0,0}, {0,0,0,0}},  //non-standard piece
  };
  private int[][][][] rotations;  //[piece] [rotation] [y] [x]

  public static void main(String[] args) {
    Tetris game = new Tetris();
    ANSI.enableConsoleMode();
    ANSI ansi = new ANSI(game);
    ansi.getConsoleSize();
    game.run(ansi);
    ANSI.disableConsoleMode();
  }
  public void run(ANSI ansi) {
    this.ansi = ansi;
    r = new Random();
    width = 1 + 10 + 7;
    height = ansi.height - 2;
    padding = (ansi.width - width) / 2;
    initRotations();
    initLevel();
    initPosition();
    drawLevel();
    startTimer();
    nextPiece = r.nextInt(shapes.length);
    getNextPiece();
    ansi.setBackColor(0x000000);
    active = true;
    while (active) {
      if (ansi.kbhit()) {
        ansi.process();
      }
      if (gameover) {
        ansi.gotoPos(padding + 5, height / 2);
        ansi.setForeColor(colors[r.nextInt(colors.length)]);
        System.out.print("GAME OVER");
        JF.sleep(10);
        continue;
      }
      if (pause) {
        ansi.gotoPos(padding + 6, height / 2);
        ansi.setForeColor(colors[r.nextInt(colors.length)]);
        System.out.print("PAUSED");
        JF.sleep(10);
        continue;
      }
      if (!moveLeft && !moveRight && !moveDown && !rotate && !toggle) {
        JF.sleep(10);
        continue;
      }
      if (rotate) {
        if (rotatePiece()) {
          redraw = true;
        }
        rotate = false;
      }
      if (moveLeft) {
        if (!pieceCollide(posx - 1, posy, rotation)) {
          erasePiece(thisPiece, posx, posy, rotation);
          posx--;
          redraw = true;
        }
        moveLeft = false;
      }
      if (moveRight) {
        if (!pieceCollide(posx + 1, posy, rotation)) {
          erasePiece(thisPiece, posx, posy, rotation);
          posx++;
          redraw = true;
        }
        moveRight = false;
      }
      if (toggle) {
        erasePiece(thisPiece, posx, posy, rotation);
        thisPiece++;
        if (thisPiece == shapes.length) thisPiece = 0;
        toggle = false;
      }
      if (moveDown) {
        if (!pieceCollide(posx, posy + 1, rotation)) {
          erasePiece(thisPiece, posx, posy, rotation);
          posy++;
        } else {
          //piece dropped into place
          placePiece();
          checkCompletedLines();
          getNextPiece();
        }
        redraw = true;
        moveDown = false;
      }
      if (refresh) {
        drawLevel();
        redraw = true;
        refresh = false;
      }
      if (redraw) {
        drawPiece(thisPiece, posx, posy, rotation);
      }
    }
    stopTimer();
    ansi.setForeColor(Color.white.getRGB());
    ansi.setBackColor(Color.black.getRGB());
  }

  private int[][] deepCopy(int[][] array) {
    int[][] ret = new int[4][4];
    for(int y=0;y<4;y++) {
      for(int x=0;x<4;x++) {
        ret[y][x] = array[y][x];
      }
    }
    return ret;
  }

  //inplace rotation of 4x4 array
  //see https://www.geeksforgeeks.org/inplace-rotate-square-matrix-by-90-degrees/
  private void rotate(int[][] array) {
    int tmp;
    for(int x=0;x<2;x++) {
      for(int y=x;y<4-x-1;y++) {
        //store current cell
        tmp = array[y][x];
        //move right to top
        array[y][x] = array[4-1-x][y];
        //move bottom to right
        array[4-1-x][y] = array[4-1-y][4-1-x];
        //move left to bottom
        array[4-1-y][4-1-x] = array[x][4-1-y];
        //move temp to left
        array[x][4-1-y] = tmp;
      }
    }
  }

  private boolean hasSpacingTop(int[][] array) {
    for(int x=0;x<4;x++) {
      if (array[0][x] == 1) return false;
    }
    return true;
  }

  private boolean hasSpacingLeft(int[][] array) {
    for(int y=0;y<4;y++) {
      if (array[y][0] == 1) return false;
    }
    return true;
  }

  //rotation may require piece to move to top left corner of array
  private void moveTopLeft(int[][] array) {
    while (hasSpacingTop(array)) {
      //shift up
      for(int y=1;y<4;y++) {
        for(int x=0;x<4;x++) {
          array[y-1][x] = array[y][x];
        }
      }
      for(int x=0;x<4;x++) {
        array[3][x] = 0;
      }
    }
    while (hasSpacingLeft(array)) {
      //shift left
      for(int y=0;y<4;y++) {
        for(int x=1;x<4;x++) {
          array[y][x-1] = array[y][x];
        }
      }
      for(int y=0;y<4;y++) {
        array[y][3] = 0;
      }
    }
  }

  private void initRotations() {
    rotations = new int[shapes.length][4][][];
    for(int shape=0;shape<shapes.length;shape++) {
      for(int rotate=0;rotate<4;rotate++) {
        switch (rotate) {
          case 0:
            rotations[shape][rotate] = deepCopy(shapes[shape]);
            break;
          case 1:
            rotations[shape][rotate] = deepCopy(shapes[shape]);
            rotate(rotations[shape][rotate]);
            moveTopLeft(rotations[shape][rotate]);
            break;
          case 2:
            rotations[shape][rotate] = deepCopy(shapes[shape]);
            rotate(rotations[shape][rotate]);
            rotate(rotations[shape][rotate]);
            moveTopLeft(rotations[shape][rotate]);
            break;
          case 3:
            rotations[shape][rotate] = deepCopy(shapes[shape]);
            rotate(rotations[shape][rotate]);
            rotate(rotations[shape][rotate]);
            rotate(rotations[shape][rotate]);
            moveTopLeft(rotations[shape][rotate]);
            break;
        }
      }
    }
  }

  private void initLevel() {
    lines = new int[height][width];
    for(int y=0;y<height;y++) {
      for(int x=0;x<width;x++) {
        if (y == height-1) {
          //bottom line
          lines[y][x] = 1;
        } else if (x > 0 && x < 11) {
          //playing area
          lines[y][x] = -1;
        } else {
          if (y > 3 && y < 9) {
            //next piece lines
            if (x == 0 || x == 11)
              //walls of playing area
              lines[y][x] = 1;
            else
              //next piece area
              lines[y][x] = -1;
          } else {
            //all other lines excluding playing area
            lines[y][x] = 1;
          }
        }
      }
    }
  }

  private void drawLevel() {
    ansi.setForeColor(Color.white.getRGB());
    ansi.setBackColor(Color.black.getRGB());
    ansi.drawBox1(padding, 1, width + 2, height + 2);
    StringBuilder sb = new StringBuilder();
    for(int y=0;y<height;y++) {
      ansi.gotoPos(padding + 1, y + 2);
      int clr = lines[y][0];
      sb.setLength(0);
      sb.append(ansi.makeForeColor(colors[clr]));
      for(int x=0;x<width;x++) {
        if (lines[y][x] == -1) {
          sb.append(' ');
        } else {
          if (lines[y][x] != clr) {
            clr = lines[y][x];
            sb.append(ansi.makeForeColor(colors[clr]));
          }
          sb.append(ASCII8.convert(219));
        }
      }
      switch (y) {
        case 0:
          sb.replace(sb.length() - 6, sb.length() - 1, "Score");
          break;
        case 1:
          String ss = Integer.toString(score);
          sb.replace(sb.length() - 6, sb.length() - 6 + ss.length(), ss);
          break;
        case 2:
          sb.replace(sb.length() - 6, sb.length() - 2, "Next");
          break;
      }
      System.out.print(sb.toString());
    }
    ansi.drawBox1(padding + 13, 5, 6, 6);
  }

  private void drawScore() {
    if (score > 99999) {
      score = 0;
    }
    ansi.gotoPos(padding + 13, 3);
    ansi.setForeColor(colors[1]);
    System.out.print(Integer.toString(score));
  }

  private void initPosition() {
    posx = 5;
    posy = 0;
    rotation = 0;
  }

  private void getNextPiece() {
    thisPiece = nextPiece;
    nextPiece = r.nextInt(shapes.length);
    initPosition();
    if (pieceCollide(posx, posy, rotation)) {
      //game over
      gameover = true;
    } else {
      drawNextPiece();
    }
  }

  private void drawNextPiece() {
    drawPiece(nextPiece, 13, 4, 0);
  }

  private void drawPiece(int piece, int x, int y, int rotation) {
    drawPiece(piece, x, y, ASCII8.convert(219), rotation);
  }

  private void erasePiece(int piece, int x, int y, int rotation) {
    drawPiece(piece, x, y, (char)32, rotation);
  }

  private void drawPiece(int piece, int px, int py, char ch, int rotation) {
    int[][] shape = getShape(piece, rotation);
    ansi.setForeColor(colors[piece]);
    for(int y=0;y<4;y++) {
      for(int x=0;x<4;x++) {
        if (shape[y][x] == 1) {
          ansi.gotoPos(padding + 1 + px + x, py + y + 2);
          System.out.print(ch);
        }
      }
    }
    ansi.gotoPos(1, 1);
  }

  private void placePiece() {
    int[][] shape = getShape(thisPiece, rotation);
    for(int y=0;y<4;y++) {
      for(int x=0;x<4;x++) {
        if (shape[y][x] == 0) continue;
        lines[posy + y][posx + x] = thisPiece;
      }
    }
    drawLevel();
    score++;
    drawScore();
    ansi.flushConsole();
  }

  private boolean pieceCollide(int px, int py, int rotation) {
    if (py > height) return true;
    int[][] shape = getShape(thisPiece, rotation);
    for(int y=0;y<4;y++) {
      for(int x=0;x<4;x++) {
        if (shape[y][x] == 0) continue;
        if (py + y > lines.length) return true;
        if (px + x >= width) return true;
        if (lines[py + y][px + x] != -1) return true;
      }
    }
    return false;
  }

  private int[][] getShape(int piece, int rotation) {
    return rotations[piece][rotation];
  }

  private boolean rotatePiece() {
    int newrotation = rotation + 1;
    if (newrotation == 4) newrotation = 0;
    int mx = maxShift[thisPiece];
    for(int dx=0;dx<=mx;dx++) {
      if (!pieceCollide(posx + dx, posy, newrotation)) {
        erasePiece(thisPiece, posx, posy, rotation);
        posx += dx;
        rotation = newrotation;
        return true;
      }
      if (!pieceCollide(posx - dx, posy, newrotation)) {
        erasePiece(thisPiece, posx, posy, rotation);
        posx -= dx;
        rotation = newrotation;
        return true;
      }
    }
    return false;
  }

  private void startTimer() {
    timer = new Timer();
    timer.schedule(this, 500, 500);
  }

  private void stopTimer() {
    timer.cancel();
    timer = null;
  }

  private void checkCompletedLines() {
    int total = 0;
    for(int y=0;y<height-1;y++) {
      int cnt = 0;
      for(int x=1;x<11;x++) {
        if (lines[y][x] == -1) break;
        cnt++;
      }
      if (cnt == 10) {
        //full line
        deleteLine(y);
        total++;
      }
    }
    if (total == 4) {
      //TETRIS!!!! - TODO : do a little russian dance
      score += 100;
      drawScore();
    }
    if (total > 0) {
      drawLevel();
    }
  }

  private void deleteLine(int py) {
    for(int a=0;a<15;a++) {
      ansi.gotoPos(padding + 2, py + 2);
      ansi.setForeColor(colors[r.nextInt(colors.length)]);
      System.out.print(ANSI.repeat(10, ASCII8.convert(219)));
      JF.sleep(10);
      ansi.gotoPos(1, 1);
    }
    for(int y=py;y>0;y--) {
      for(int x=1;x<11;x++) {
        lines[y][x] = lines[y-1][x];
      }
    }
    for(int x=1;x<11;x++) {
      lines[0][x] = -1;
    }
    drawLevel();
  }

  public void keyPressed(int keyCode, int keyMods) {
    if (keyMods != 0) return;
    switch (keyCode) {
      case KeyEvent.VK_LEFT:
        if (!pause) {
          moveLeft = true;
        }
        break;
      case KeyEvent.VK_RIGHT:
        if (!pause) {
          moveRight = true;
        }
        break;
      case KeyEvent.VK_DOWN:
        if (!pause) {
          moveDown = true;
        }
        break;
      case KeyEvent.VK_UP:
        if (!pause) {
          rotate = true;
        }
        break;
      case KeyEvent.VK_ESCAPE:
        active = false;
        break;
    }
  }

  public void keyTyped(char key) {
    switch (key) {
      case 'p':
        pause = !pause;
        refresh = true;
        break;
      case ' ':
        if (!pause) {
          rotate = true;
        }
        break;
      case 't':
        if (!pause) {
          toggle = true;
        }
        break;
      case 'n':
        initLevel();
        initPosition();
        pause = false;
        gameover = false;
        refresh = true;
        score = 0;
        break;
      case 'q':
        active = false;
        break;
    }
  }

  public void run() {
    //timer event
    if (!pause) {
      moveDown = true;
    }
  }
}
