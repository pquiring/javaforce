//
//  Copyright (C) 2001,2002 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2001,2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// RfbProto.java
//

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.util.zip.*;

class RfbProto {

  final String versionMsg = "RFB 003.003\n";
  final static int ConnFailed = 0, NoAuth = 1, VncAuth = 2;
  final static int VncAuthOK = 0, VncAuthFailed = 1, VncAuthTooMany = 2;

  final static int FramebufferUpdate = 0, SetColourMapEntries = 1, Bell = 2,
    ServerCutText = 3;

  final int SetPixelFormat = 0, FixColourMapEntries = 1, SetEncodings = 2,
    FramebufferUpdateRequest = 3, KeyboardEvent = 4, PointerEvent = 5,
    ClientCutText = 6;

  final static int
    EncodingRaw            = 0,
    EncodingCopyRect       = 1,
    EncodingRRE            = 2,
    EncodingCoRRE          = 4,
    EncodingHextile        = 5,
    EncodingZlib           = 6,
    EncodingTight          = 7,
    EncodingCompressLevel0 = 0xFFFFFF00,
    EncodingQualityLevel0  = 0xFFFFFFE0,
    EncodingXCursor        = 0xFFFFFF10,
    EncodingRichCursor     = 0xFFFFFF11,
    EncodingPointerPos     = 0xFFFFFF18,
    EncodingLastRect       = 0xFFFFFF20,
    EncodingNewFBSize      = 0xFFFFFF21;

  final static int MaxNormalEncoding = 7;

  final int HextileRaw			= (1 << 0);
  final int HextileBackgroundSpecified	= (1 << 1);
  final int HextileForegroundSpecified	= (1 << 2);
  final int HextileAnySubrects		= (1 << 3);
  final int HextileSubrectsColoured	= (1 << 4);

  final static int TightExplicitFilter  = 0x04;
  final static int TightFill            = 0x08;
  final static int TightJpeg            = 0x09;
  final static int TightMaxSubencoding  = 0x09;
  final static int TightFilterCopy      = 0x00;
  final static int TightFilterPalette   = 0x01;
  final static int TightFilterGradient  = 0x02;

  final static int TightMinToCompress   = 12;

  String host;
  int port;
  Socket sock;
  DataInputStream is;
  OutputStream os;
  SessionRecorder rec;
  boolean inNormalProtocol = false;
  VncViewer viewer;

  // Java on UNIX does not call keyPressed() on some keys, for example
  // swedish keys To prevent our workaround to produce duplicate
  // keypresses on JVMs that actually works, keep track of if
  // keyPressed() for a "broken" key was called or not. 
  boolean brokenKeyPressed = false;

  // This will be set to true on the first framebuffer update
  // containing Zlib- or Tight-encoded data.
  boolean wereZlibUpdates = false;

  // This will be set to false if the startSession() was called after
  // we have received at least one Zlib- or Tight-encoded framebuffer
  // update.
  boolean recordFromBeginning = true;

  // This fields are needed to show warnings about inefficiently saved
  // sessions only once per each saved session file.
  boolean zlibWarningShown;
  boolean tightWarningShown;

  // Before starting to record each saved session, we set this field
  // to 0, and increment on each framebuffer update. We don't flush
  // the SessionRecorder data into the file before the second update. 
  // This allows us to write initial framebuffer update with zero
  // timestamp, to let the player show initial desktop before
  // playback.
  int numUpdatesInSession;

  // If true, informs that the RFB socket was closed.
  private boolean closed;

  //
  // Constructor. Make TCP connection to RFB server.
  //

  RfbProto(String h, int p, VncViewer v) throws IOException {
    viewer = v;
    host = h;
    port = p;

    if (viewer.socketFactory == null) {
      sock = new Socket(host, port);
    } else {
      try {
	Class factoryClass = Class.forName(viewer.socketFactory);
	SocketFactory factory = (SocketFactory)factoryClass.newInstance();
	if (viewer.inAnApplet)
	  sock = factory.createSocket(host, port, viewer);
	else
	  sock = factory.createSocket(host, port, viewer.mainArgs);
      } catch(Exception e) {
	e.printStackTrace();
	throw new IOException(e.getMessage());
      }
    }
    is = new DataInputStream(new BufferedInputStream(sock.getInputStream(),
						     16384));
    os = sock.getOutputStream();
  }


  synchronized void close() {
    try {
      sock.close();
      closed = true;
      System.out.println("RFB socket closed");
      if (rec != null) {
	rec.close();
	rec = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  synchronized boolean closed() {
    return closed;
  }

  //
  // Read server's protocol version message
  //

  int serverMajor, serverMinor;

  void readVersionMsg() throws Exception {

    byte[] b = new byte[12];

    is.readFully(b);

    if ((b[0] != 'R') || (b[1] != 'F') || (b[2] != 'B') || (b[3] != ' ')
	|| (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9')
	|| (b[6] < '0') || (b[6] > '9') || (b[7] != '.')
	|| (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
	|| (b[10] < '0') || (b[10] > '9') || (b[11] != '\n'))
    {
      throw new Exception("Host " + host + " port " + port +
			  " is not an RFB server");
    }

    serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
    serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');
  }


  //
  // Write our protocol version message
  //

  void writeVersionMsg() throws IOException {
    os.write(versionMsg.getBytes());
  }


  //
  // Find out the authentication scheme.
  //

  int readAuthScheme() throws Exception {
    int authScheme = is.readInt();

    switch (authScheme) {

    case ConnFailed:
      int reasonLen = is.readInt();
      byte[] reason = new byte[reasonLen];
      is.readFully(reason);
      throw new Exception(new String(reason));

    case NoAuth:
    case VncAuth:
      return authScheme;

    default:
      throw new Exception("Unknown authentication scheme from RFB server: " +
			  authScheme);

    }
  }


  //
  // Write the client initialisation message
  //

  void writeClientInit() throws IOException {
    if (viewer.options.shareDesktop) {
      os.write(1);
    } else {
      os.write(0);
    }
    viewer.options.disableShareDesktop();
  }


  //
  // Read the server initialisation message
  //

  String desktopName;
  int framebufferWidth, framebufferHeight;
  int bitsPerPixel, depth;
  boolean bigEndian, trueColour;
  int redMax, greenMax, blueMax, redShift, greenShift, blueShift;

  void readServerInit() throws IOException {
    framebufferWidth = is.readUnsignedShort();
    framebufferHeight = is.readUnsignedShort();
    bitsPerPixel = is.readUnsignedByte();
    depth = is.readUnsignedByte();
    bigEndian = (is.readUnsignedByte() != 0);
    trueColour = (is.readUnsignedByte() != 0);
    redMax = is.readUnsignedShort();
    greenMax = is.readUnsignedShort();
    blueMax = is.readUnsignedShort();
    redShift = is.readUnsignedByte();
    greenShift = is.readUnsignedByte();
    blueShift = is.readUnsignedByte();
    byte[] pad = new byte[3];
    is.readFully(pad);
    int nameLength = is.readInt();
    byte[] name = new byte[nameLength];
    is.readFully(name);
    desktopName = new String(name);

    inNormalProtocol = true;
  }


  //
  // Create session file and write initial protocol messages into it.
  //

  void startSession(String fname) throws IOException {
    rec = new SessionRecorder(fname);
    rec.writeHeader();
    rec.write(versionMsg.getBytes());
    rec.writeIntBE(NoAuth);
    rec.writeShortBE(framebufferWidth);
    rec.writeShortBE(framebufferHeight);
    byte[] fbsServerInitMsg =	{
      32, 24, 0, 1, 0,
      (byte)0xFF, 0, (byte)0xFF, 0, (byte)0xFF,
      16, 8, 0, 0, 0, 0
    };
    rec.write(fbsServerInitMsg);
    rec.writeIntBE(desktopName.length());
    rec.write(desktopName.getBytes());
    numUpdatesInSession = 0;

    if (wereZlibUpdates)
      recordFromBeginning = false;

    zlibWarningShown = false;
    tightWarningShown = false;
  }

  //
  // Close session file.
  //

  void closeSession() throws IOException {
    if (rec != null) {
      rec.close();
      rec = null;
    }
  }


  //
  // Set new framebuffer size
  //

  void setFramebufferSize(int width, int height) {
    framebufferWidth = width;
    framebufferHeight = height;
  }


  //
  // Read the server message type
  //

  int readServerMessageType() throws IOException {
    int msgType = is.readUnsignedByte();

    // If the session is being recorded:
    if (rec != null) {
      if (msgType == Bell) {	// Save Bell messages in session files.
	rec.writeByte(msgType);
	if (numUpdatesInSession > 0)
	  rec.flush();
      }
    }

    return msgType;
  }


  //
  // Read a FramebufferUpdate message
  //

  int updateNRects;

  void readFramebufferUpdate() throws IOException {
    is.readByte();
    updateNRects = is.readUnsignedShort();

    // If the session is being recorded:
    if (rec != null) {
      rec.writeByte(FramebufferUpdate);
      rec.writeByte(0);
      rec.writeShortBE(updateNRects);
    }

    numUpdatesInSession++;
  }

  // Read a FramebufferUpdate rectangle header

  int updateRectX, updateRectY, updateRectW, updateRectH, updateRectEncoding;

  void readFramebufferUpdateRectHdr() throws Exception {
    updateRectX = is.readUnsignedShort();
    updateRectY = is.readUnsignedShort();
    updateRectW = is.readUnsignedShort();
    updateRectH = is.readUnsignedShort();
    updateRectEncoding = is.readInt();

    if (updateRectEncoding == EncodingZlib ||
	updateRectEncoding == EncodingTight)
      wereZlibUpdates = true;

    // If the session is being recorded:
    if (rec != null) {
      if (numUpdatesInSession > 1)
	rec.flush();		// Flush the output on each rectangle.
      rec.writeShortBE(updateRectX);
      rec.writeShortBE(updateRectY);
      rec.writeShortBE(updateRectW);
      rec.writeShortBE(updateRectH);
      if (updateRectEncoding == EncodingZlib && !recordFromBeginning) {
	// Here we cannot write Zlib-encoded rectangles because the
	// decoder won't be able to reproduce zlib stream state.
	if (!zlibWarningShown) {
	  System.out.println("Warning: Raw encoding will be used " +
			     "instead of Zlib in recorded session.");
	  zlibWarningShown = true;
	}
	rec.writeIntBE(EncodingRaw);
      } else {
	rec.writeIntBE(updateRectEncoding);
	if (updateRectEncoding == EncodingTight && !recordFromBeginning &&
	    !tightWarningShown) {
	  System.out.println("Warning: Re-compressing Tight-encoded " +
			     "updates for session recording.");
	  tightWarningShown = true;
	}
      }
    }

    if (updateRectEncoding < 0 || updateRectEncoding > MaxNormalEncoding)
      return;

    if (updateRectX + updateRectW > framebufferWidth ||
	updateRectY + updateRectH > framebufferHeight) {
      throw new Exception("Framebuffer update rectangle too large: " +
			  updateRectW + "x" + updateRectH + " at (" +
			  updateRectX + "," + updateRectY + ")");
    }
  }

  // Read CopyRect source X and Y.

  int copyRectSrcX, copyRectSrcY;

  void readCopyRect() throws IOException {
    copyRectSrcX = is.readUnsignedShort();
    copyRectSrcY = is.readUnsignedShort();

    // If the session is being recorded:
    if (rec != null) {
      rec.writeShortBE(copyRectSrcX);
      rec.writeShortBE(copyRectSrcY);
    }
  }


  //
  // Read a ServerCutText message
  //

  String readServerCutText() throws IOException {
    byte[] pad = new byte[3];
    is.readFully(pad);
    int len = is.readInt();
    byte[] text = new byte[len];
    is.readFully(text);
    return new String(text);
  }


  //
  // Read an integer in compact representation (1..3 bytes).
  // Such format is used as a part of the Tight encoding.
  // Also, this method records data if session recording is active and
  // the viewer's recordFromBeginning variable is set to true.
  //

  int readCompactLen() throws IOException {
    int[] portion = new int[3];
    portion[0] = is.readUnsignedByte();
    int byteCount = 1;
    int len = portion[0] & 0x7F;
    if ((portion[0] & 0x80) != 0) {
      portion[1] = is.readUnsignedByte();
      byteCount++;
      len |= (portion[1] & 0x7F) << 7;
      if ((portion[1] & 0x80) != 0) {
	portion[2] = is.readUnsignedByte();
	byteCount++;
	len |= (portion[2] & 0xFF) << 14;
      }
    }

    if (rec != null && recordFromBeginning)
      for (int i = 0; i < byteCount; i++)
	rec.writeByte(portion[i]);

    return len;
  }


  //
  // Write a FramebufferUpdateRequest message
  //

  void writeFramebufferUpdateRequest(int x, int y, int w, int h,
				     boolean incremental)
       throws IOException
  {
    byte[] b = new byte[10];

    b[0] = (byte) FramebufferUpdateRequest;
    b[1] = (byte) (incremental ? 1 : 0);
    b[2] = (byte) ((x >> 8) & 0xff);
    b[3] = (byte) (x & 0xff);
    b[4] = (byte) ((y >> 8) & 0xff);
    b[5] = (byte) (y & 0xff);
    b[6] = (byte) ((w >> 8) & 0xff);
    b[7] = (byte) (w & 0xff);
    b[8] = (byte) ((h >> 8) & 0xff);
    b[9] = (byte) (h & 0xff);

    os.write(b);
  }


  //
  // Write a SetPixelFormat message
  //

  void writeSetPixelFormat(int bitsPerPixel, int depth, boolean bigEndian,
			   boolean trueColour,
			   int redMax, int greenMax, int blueMax,
			   int redShift, int greenShift, int blueShift)
       throws IOException
  {
    byte[] b = new byte[20];

    b[0]  = (byte) SetPixelFormat;
    b[4]  = (byte) bitsPerPixel;
    b[5]  = (byte) depth;
    b[6]  = (byte) (bigEndian ? 1 : 0);
    b[7]  = (byte) (trueColour ? 1 : 0);
    b[8]  = (byte) ((redMax >> 8) & 0xff);
    b[9]  = (byte) (redMax & 0xff);
    b[10] = (byte) ((greenMax >> 8) & 0xff);
    b[11] = (byte) (greenMax & 0xff);
    b[12] = (byte) ((blueMax >> 8) & 0xff);
    b[13] = (byte) (blueMax & 0xff);
    b[14] = (byte) redShift;
    b[15] = (byte) greenShift;
    b[16] = (byte) blueShift;

    os.write(b);
  }


  //
  // Write a FixColourMapEntries message.  The values in the red, green and
  // blue arrays are from 0 to 65535.
  //

  void writeFixColourMapEntries(int firstColour, int nColours,
				int[] red, int[] green, int[] blue)
       throws IOException
  {
    byte[] b = new byte[6 + nColours * 6];

    b[0] = (byte) FixColourMapEntries;
    b[2] = (byte) ((firstColour >> 8) & 0xff);
    b[3] = (byte) (firstColour & 0xff);
    b[4] = (byte) ((nColours >> 8) & 0xff);
    b[5] = (byte) (nColours & 0xff);

    for (int i = 0; i < nColours; i++) {
      b[6 + i * 6]     = (byte) ((red[i] >> 8) & 0xff);
      b[6 + i * 6 + 1] = (byte) (red[i] & 0xff);
      b[6 + i * 6 + 2] = (byte) ((green[i] >> 8) & 0xff);
      b[6 + i * 6 + 3] = (byte) (green[i] & 0xff);
      b[6 + i * 6 + 4] = (byte) ((blue[i] >> 8) & 0xff);
      b[6 + i * 6 + 5] = (byte) (blue[i] & 0xff);
    }
 
    os.write(b);
  }


  //
  // Write a SetEncodings message
  //

  void writeSetEncodings(int[] encs, int len) throws IOException {
    byte[] b = new byte[4 + 4 * len];

    b[0] = (byte) SetEncodings;
    b[2] = (byte) ((len >> 8) & 0xff);
    b[3] = (byte) (len & 0xff);

    for (int i = 0; i < len; i++) {
      b[4 + 4 * i] = (byte) ((encs[i] >> 24) & 0xff);
      b[5 + 4 * i] = (byte) ((encs[i] >> 16) & 0xff);
      b[6 + 4 * i] = (byte) ((encs[i] >> 8) & 0xff);
      b[7 + 4 * i] = (byte) (encs[i] & 0xff);
    }

    os.write(b);
  }


  //
  // Write a ClientCutText message
  //

  void writeClientCutText(String text) throws IOException {
    byte[] b = new byte[8 + text.length()];

    b[0] = (byte) ClientCutText;
    b[4] = (byte) ((text.length() >> 24) & 0xff);
    b[5] = (byte) ((text.length() >> 16) & 0xff);
    b[6] = (byte) ((text.length() >> 8) & 0xff);
    b[7] = (byte) (text.length() & 0xff);

    System.arraycopy(text.getBytes(), 0, b, 8, text.length());

    os.write(b);
  }


  //
  // A buffer for putting pointer and keyboard events before being sent.  This
  // is to ensure that multiple RFB events generated from a single Java Event 
  // will all be sent in a single network packet.  The maximum possible
  // length is 4 modifier down events, a single key event followed by 4
  // modifier up events i.e. 9 key events or 72 bytes.
  //

  byte[] eventBuf = new byte[72];
  int eventBufLen;


  // Useful shortcuts for modifier masks.

  final static int CTRL_MASK  = InputEvent.CTRL_MASK;
  final static int SHIFT_MASK = InputEvent.SHIFT_MASK;
  final static int META_MASK  = InputEvent.META_MASK;
  final static int ALT_MASK   = InputEvent.ALT_MASK;


  //
  // Write a pointer event message.  We may need to send modifier key events
  // around it to set the correct modifier state.
  //

  int pointerMask = 0;

  void writePointerEvent(MouseEvent evt) throws IOException {
    int modifiers = evt.getModifiers();

    int mask2 = 2;
    int mask3 = 4;
    if (viewer.options.reverseMouseButtons2And3) {
      mask2 = 4;
      mask3 = 2;
    }

    // Note: For some reason, AWT does not set BUTTON1_MASK on left
    // button presses. Here we think that it was the left button if
    // modifiers do not include BUTTON2_MASK or BUTTON3_MASK.

    if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
      if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
        pointerMask = mask2;
        modifiers &= ~ALT_MASK;
      } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
        pointerMask = mask3;
        modifiers &= ~META_MASK;
      } else {
        pointerMask = 1;
      }
    } else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
      pointerMask = 0;
      if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
        modifiers &= ~ALT_MASK;
      } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
        modifiers &= ~META_MASK;
      }
    }

    eventBufLen = 0;
    writeModifierKeyEvents(modifiers);

    int x = evt.getX();
    int y = evt.getY();

    if (x < 0) x = 0;
    if (y < 0) y = 0;

    eventBuf[eventBufLen++] = (byte) PointerEvent;
    eventBuf[eventBufLen++] = (byte) pointerMask;
    eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
    eventBuf[eventBufLen++] = (byte) (x & 0xff);
    eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
    eventBuf[eventBufLen++] = (byte) (y & 0xff);

    //
    // Always release all modifiers after an "up" event
    //

    if (pointerMask == 0) {
      writeModifierKeyEvents(0);
    }

    os.write(eventBuf, 0, eventBufLen);
  }


  //
  // Write a key event message.  We may need to send modifier key events
  // around it to set the correct modifier state.  Also we need to translate
  // from the Java key values to the X keysym values used by the RFB protocol.
  //

  void writeKeyEvent(KeyEvent evt) throws IOException {

    int keyChar = evt.getKeyChar();

    //
    // Ignore event if only modifiers were pressed.
    //

    // Some JVMs return 0 instead of CHAR_UNDEFINED in getKeyChar().
    if (keyChar == 0)
      keyChar = KeyEvent.CHAR_UNDEFINED;

    if (keyChar == KeyEvent.CHAR_UNDEFINED) {
      int code = evt.getKeyCode();
      if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT ||
          code == KeyEvent.VK_META || code == KeyEvent.VK_ALT)
        return;
    }

    //
    // Key press or key release?
    //

    boolean down = (evt.getID() == KeyEvent.KEY_PRESSED);

    int key;
    if (evt.isActionKey()) {

      //
      // An action key should be one of the following.
      // If not then just ignore the event.
      //

      switch(evt.getKeyCode()) {
      case KeyEvent.VK_HOME:      key = 0xff50; break;
      case KeyEvent.VK_LEFT:      key = 0xff51; break;
      case KeyEvent.VK_UP:        key = 0xff52; break;
      case KeyEvent.VK_RIGHT:     key = 0xff53; break;
      case KeyEvent.VK_DOWN:      key = 0xff54; break;
      case KeyEvent.VK_PAGE_UP:   key = 0xff55; break;
      case KeyEvent.VK_PAGE_DOWN: key = 0xff56; break;
      case KeyEvent.VK_END:       key = 0xff57; break;
      case KeyEvent.VK_INSERT:    key = 0xff63; break;
      case KeyEvent.VK_F1:        key = 0xffbe; break;
      case KeyEvent.VK_F2:        key = 0xffbf; break;
      case KeyEvent.VK_F3:        key = 0xffc0; break;
      case KeyEvent.VK_F4:        key = 0xffc1; break;
      case KeyEvent.VK_F5:        key = 0xffc2; break;
      case KeyEvent.VK_F6:        key = 0xffc3; break;
      case KeyEvent.VK_F7:        key = 0xffc4; break;
      case KeyEvent.VK_F8:        key = 0xffc5; break;
      case KeyEvent.VK_F9:        key = 0xffc6; break;
      case KeyEvent.VK_F10:       key = 0xffc7; break;
      case KeyEvent.VK_F11:       key = 0xffc8; break;
      case KeyEvent.VK_F12:       key = 0xffc9; break;
      default:
        return;
      }

    } else {

      //
      // A "normal" key press.  Ordinary ASCII characters go straight through.
      // For CTRL-<letter>, CTRL is sent separately so just send <letter>.
      // Backspace, tab, return, escape and delete have special keysyms.
      // Anything else we ignore.
      //

      key = keyChar;

      if (key < 0x20) {
        if (evt.isControlDown()) {
          key += 0x60;
        } else {
          switch(key) {
          case KeyEvent.VK_BACK_SPACE: key = 0xff08; break;
          case KeyEvent.VK_TAB:        key = 0xff09; break;
          case KeyEvent.VK_ENTER:      key = 0xff0d; break;
          case KeyEvent.VK_ESCAPE:     key = 0xff1b; break;
          }
        }
      } else if (key == 0x7f) {
	// Delete
	key = 0xffff;
      } else if (key > 0xff) {
	// JDK1.1 on X incorrectly passes some keysyms straight through,
	// so we do too.  JDK1.1.4 seems to have fixed this.
	// The keysyms passed are 0xff00 .. XK_BackSpace .. XK_Delete
	if ((key < 0xff00) || (key > 0xffff))
	  return;
      }
    }

    // Fake keyPresses for keys that only generates keyRelease events
    if ((key == 0xe5) || (key == 0xc5) || // XK_aring / XK_Aring
	(key == 0xe4) || (key == 0xc4) || // XK_adiaeresis / XK_Adiaeresis
	(key == 0xf6) || (key == 0xd6) || // XK_odiaeresis / XK_Odiaeresis
	(key == 0xa7) || (key == 0xbd) || // XK_section / XK_onehalf
	(key == 0xa3)) {                  // XK_sterling
      // Make sure we do not send keypress events twice on platforms
      // with correct JVMs (those that actually report KeyPress for all
      // keys)	
      if (down)
	brokenKeyPressed = true;

      if (!down && !brokenKeyPressed) {
	// We've got a release event for this key, but haven't received
        // a press. Fake it. 
	eventBufLen = 0;
	writeModifierKeyEvents(evt.getModifiers());
	writeKeyEvent(key, true);
	os.write(eventBuf, 0, eventBufLen);
      }

      if (!down)
	brokenKeyPressed = false;  
    }

    eventBufLen = 0;
    writeModifierKeyEvents(evt.getModifiers());
    writeKeyEvent(key, down);

    // Always release all modifiers after an "up" event
    if (!down)
      writeModifierKeyEvents(0);

    os.write(eventBuf, 0, eventBufLen);
  }


  //
  // Add a raw key event with the given X keysym to eventBuf.
  //

  void writeKeyEvent(int keysym, boolean down) {
    eventBuf[eventBufLen++] = (byte) KeyboardEvent;
    eventBuf[eventBufLen++] = (byte) (down ? 1 : 0);
    eventBuf[eventBufLen++] = (byte) 0;
    eventBuf[eventBufLen++] = (byte) 0;
    eventBuf[eventBufLen++] = (byte) ((keysym >> 24) & 0xff);
    eventBuf[eventBufLen++] = (byte) ((keysym >> 16) & 0xff);
    eventBuf[eventBufLen++] = (byte) ((keysym >> 8) & 0xff);
    eventBuf[eventBufLen++] = (byte) (keysym & 0xff);
  }


  //
  // Write key events to set the correct modifier state.
  //

  int oldModifiers = 0;

  void writeModifierKeyEvents(int newModifiers) {
    if ((newModifiers & CTRL_MASK) != (oldModifiers & CTRL_MASK))
      writeKeyEvent(0xffe3, (newModifiers & CTRL_MASK) != 0);

    if ((newModifiers & SHIFT_MASK) != (oldModifiers & SHIFT_MASK))
      writeKeyEvent(0xffe1, (newModifiers & SHIFT_MASK) != 0);

    if ((newModifiers & META_MASK) != (oldModifiers & META_MASK))
      writeKeyEvent(0xffe7, (newModifiers & META_MASK) != 0);

    if ((newModifiers & ALT_MASK) != (oldModifiers & ALT_MASK))
      writeKeyEvent(0xffe9, (newModifiers & ALT_MASK) != 0);

    oldModifiers = newModifiers;
  }


  //
  // Compress and write the data into the recorded session file. This
  // method assumes the recording is on (rec != null).
  //

  void recordCompressedData(byte[] data, int off, int len) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(data, off, len);
    int bufSize = len + len / 100 + 12;
    byte[] buf = new byte[bufSize];
    deflater.finish();
    int compressedSize = deflater.deflate(buf);
    recordCompactLen(compressedSize);
    rec.write(buf, 0, compressedSize);
  }

  void recordCompressedData(byte[] data) throws IOException {
    recordCompressedData(data, 0, data.length);
  }

  //
  // Write an integer in compact representation (1..3 bytes) into the
  // recorded session file. This method assumes the recording is on
  // (rec != null).
  //

  void recordCompactLen(int len) throws IOException {
    byte[] buf = new byte[3];
    int bytes = 0;
    buf[bytes++] = (byte)(len & 0x7F);
    if (len > 0x7F) {
      buf[bytes-1] |= 0x80;
      buf[bytes++] = (byte)(len >> 7 & 0x7F);
      if (len > 0x3FFF) {
	buf[bytes-1] |= 0x80;
	buf[bytes++] = (byte)(len >> 14 & 0xFF);
      }
    }
    rec.write(buf, 0, bytes);
  }
}

