/******************************************************************************
 *                                                                            *
 * Copyright (c) 1999-2003 Wimba S.A., All Rights Reserved.                   *
 *                                                                            *
 * COPYRIGHT:                                                                 *
 *      This software is the property of Wimba S.A.                           *
 *      This software is redistributed under the Xiph.org variant of          *
 *      the BSD license.                                                      *
 *      Redistribution and use in source and binary forms, with or without    *
 *      modification, are permitted provided that the following conditions    *
 *      are met:                                                              *
 *      - Redistributions of source code must retain the above copyright      *
 *      notice, this list of conditions and the following disclaimer.         *
 *      - Redistributions in binary form must reproduce the above copyright   *
 *      notice, this list of conditions and the following disclaimer in the   *
 *      documentation and/or other materials provided with the distribution.  *
 *      - Neither the name of Wimba, the Xiph.org Foundation nor the names of *
 *      its contributors may be used to endorse or promote products derived   *
 *      from this software without specific prior written permission.         *
 *                                                                            *
 * WARRANTIES:                                                                *
 *      This software is made available by the authors in the hope            *
 *      that it will be useful, but without any warranty.                     *
 *      Wimba S.A. is not liable for any consequence related to the           *
 *      use of the provided software.                                         *
 *                                                                            *
 * Class: FilteredAudioInputStream.java                                       *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 *                                                                            *
 * Date: 12th July 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id$ */

package javaforce.codec.speex.spi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A <code>FilteredAudioInputStream</code> is an AudioInputStream with buffers
 * to facilitate transcoding the audio.
 * A first byte array can buffer the data from the underlying inputstream until
 * sufficient data for transcoding is present.
 * A second byte array can hold the transcoded audio data, ready to be read out
 * by the stream user.
 *
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision$
 */
public abstract class FilteredAudioInputStream
        extends AudioInputStream {
    /**
     * The default size of the buffer.
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    /**
     * The underlying inputStream.
     */
    protected InputStream in;

    /**
     * The internal buffer array where the data is stored. When necessary,
     * it may be replaced by another array of a different size.
     */
    protected byte[] buf;

    /**
     * The index one greater than the index of the last valid byte in the buffer.
     * This value is always in the range 0 through <code>buf.length</code>;
     * elements <code>buf[0]</code> through <code>buf[count-1]</code> contain
     * buffered input data obtained and filtered from the underlying inputstream.
     */
    protected int count;

    /**
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     * <p/>
     * This value is always in the range 0 through <code>count</code>.
     * If it is less than <code>count</code>, then <code>buf[pos]</code> is the
     * next byte to be supplied as input.
     * If it is equal to <code>count</code>, then the next <code>read</code> or
     * <code>skip</code> operation will require more bytes to be read and
     * filtered from the underlying inputstream.
     *
     * @see #buf
     */
    protected int pos;

    /**
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     * <p/>
     * This value is always in the range -1 through <code>pos</code>.
     * If there is no marked position in the inputstream, this field is -1.
     * If there is a marked position in the inputstream, then
     * <code>buf[markpos]</code> is the first byte to be supplied as input after
     * a <code>reset</code> operation.
     * If <code>markpos</code> is not -1, then all bytes from positions
     * <code>buf[markpos]</code> through <code>buf[pos-1]</code> must remain in
     * the buffer array (though they may be moved to another place in the buffer
     * array, with suitable adjustments to the values of <code>count</code>,
     * <code>pos</code>, and <code>markpos</code>); they may not be discarded
     * unless and until the difference between <code>pos</code> and
     * <code>markpos</code> exceeds <code>marklimit</code>.
     *
     * @see #mark(int)
     * @see #pos
     */
    protected int markpos;

    /**
     * The maximum read ahead allowed after a call to the <code>mark</code>
     * method before subsequent calls to the <code>reset</code> method fail.
     * Whenever the difference between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>, then the  mark may be dropped by setting
     * <code>markpos</code> to -1.
     *
     * @see #mark(int)
     * @see #reset()
     */
    protected int marklimit;

    /**
     * Array of size 1, used by the read method to read just 1 byte.
     */
    private final byte[] single = new byte[1];

    /**
     * The internal buffer array where the unfiltered data is temporarily stored.
     */
    protected byte[] prebuf;

    /**
     * The index one greater than the index of the last valid byte in the
     * unfiltered data buffer.
     * This value is always in the range 0 through <code>prebuf.length</code>;
     * elements <code>prebuf[0]</code> through <code>prebuf[count-1]</code>
     * contain buffered input data obtained from the underlying input stream.
     */
    protected int precount;

    /**
     * The current position in the unfiltered data buffer. This is the index of
     * the next character to be read from the <code>prebuf</code> array.
     * <p/>
     * This value is always in the range 0 through <code>precount</code>.
     * If it is less than <code>precount</code>, then <code>prebuf[pos]</code> is
     * the next byte to be supplied as input.
     * If it is equal to <code>precount</code>, then the next <code>read</code>
     * or <code>skip</code> operation will require more bytes to be read from the
     * contained inputstream.
     *
     * @see #prebuf
     */
    protected int prepos;

    /**
     * Check to make sure that this stream has not been closed
     *
     * @throws IOException
     */
    protected void checkIfStillOpen()
            throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
    }

    /**
     * Creates a <code>FilteredAudioInputStream</code> and saves its argument,
     * the input stream <code>in</code>, for later use.
     * An internal buffer array is created and  stored in <code>buf</code>.
     *
     * @param in     the underlying input stream.
     * @param format the format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     * @throws IllegalArgumentException if size <= 0 or presize <= 0.
     */
    public FilteredAudioInputStream(final InputStream in,
                                    final AudioFormat format,
                                    final long length) {
        this(in, format, length, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a <code>FilteredAudioInputStream</code> with the specified buffer
     * size, and saves its argument, the inputstream <code>in</code> for later use.
     * An internal buffer array of length <code>size</code> is created and stored
     * in <code>buf</code>.
     *
     * @param in     the underlying input stream.
     * @param format the format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     * @param size   the buffer sizes.
     * @throws IllegalArgumentException if size <= 0.
     */
    public FilteredAudioInputStream(final InputStream in,
                                    final AudioFormat format,
                                    final long length,
                                    final int size) {
        this(in, format, length, size, size);
    }

    /**
     * Creates a <code>FilteredAudioInputStream</code> with the specified buffer
     * size, and saves its argument, the inputstream <code>in</code> for later use.
     * An internal buffer array of length <code>size</code> is created and stored
     * in <code>buf</code>.
     *
     * @param in      the underlying input stream.
     * @param format  the format of this stream's audio data.
     * @param length  the length in sample frames of the data in this stream.
     * @param size    the buffer size.
     * @param presize the prebuffer size.
     * @throws IllegalArgumentException if size <= 0 or presize <= 0.
     */
    public FilteredAudioInputStream(final InputStream in,
                                    final AudioFormat format,
                                    final long length,
                                    final int size,
                                    final int presize) {
        super(in, format, length);
        this.in = in;
        if ((size <= 0) || (presize <= 0)) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
        count = 0;
        prebuf = new byte[presize];
        precount = 0;
        marklimit = size;
        markpos = -1;
    }

    /**
     * Fills the buffer with more data, taking into account shuffling and other
     * tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     *
     * @throws IOException
     */
    protected void fill()
            throws IOException {
        makeSpace();
        while (true) {
            int read = in.read(prebuf, precount, prebuf.length - precount);
            if (read < 0) { // inputstream has ended
                // do last stuff here
                break;
            } else if (read > 0) {
                // do stuff here
                precount += read;
                break;
            } else { // read == 0
                // read 0 bytes from underlying stream yet it is not finished.
            }
        }
    }

    /**
     * Free up some space in the buffers.
     */
    protected void makeSpace() {
        if (markpos < 0)
            pos = 0; /* no mark: throw away the buffer */
        else if (pos >= buf.length) /* no room left in buffer */
            if (markpos > 0) { /* can throw away early part of the buffer */
                int sz = pos - markpos;
                System.arraycopy(buf, markpos, buf, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buf.length >= marklimit) {
                markpos = -1; /* buffer got too big, invalidate mark */
                pos = 0; /* drop buffer contents */
            } else { /* grow buffer */
                int nsz = pos * 2;
                if (nsz > marklimit)
                    nsz = marklimit;
                byte[] nbuf = new byte[nsz];
                System.arraycopy(buf, 0, nbuf, 0, pos);
                buf = nbuf;
            }
        count = pos;
    }

    /**
     * See the general contract of the <code>read</code> method of
     * <code>InputStream</code>.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     * @see #in
     */
    public synchronized int read()
            throws IOException {
        if (read(single, 0, 1) == -1) {
            return (-1);
        } else {
            return (single[0] & 0xFF);
        }
    }

    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     * <p/>
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     * <p/>
     * <li> The specified number of bytes have been read,
     * <p/>
     * <li> The <code>read</code> method of the underlying stream returns
     * <code>-1</code>, indicating end-of-file, or
     * <p/>
     * <li> The <code>available</code> method of the underlying stream
     * returns zero, indicating that further input requests would block.
     * <p/>
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     * <p/>
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     *
     * @param b   destination buffer.
     * @param off offset at which to start storing bytes.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or <code>-1</code> if the end of
     * the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized int read(final byte[] b,
                                 final int off,
                                 final int len)
            throws IOException {
        checkIfStillOpen();
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        int avail = count - pos;
        if (avail <= 0) {
            fill();
            avail = count - pos;
            if (avail <= 0) return -1;
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(buf, pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    /**
     * See the general contract of the <code>skip</code> method of
     * <code>InputStream</code>.
     *
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized long skip(final long n)
            throws IOException {
        checkIfStillOpen();
        // Sanity check
        if (n <= 0) {
            return 0;
        }
        // Skip buffered data if there is any
        if (pos < count) {
            int avail = count - pos;
            if (avail > n) {
                pos += n;
                return n;
            } else {
                pos = count;
                return avail;
            }
        }
        // Read into buffers and skip
        else {
            fill(); // This is potentially blocking (i.e. the read on the underlying inputStream could be blocking)
            int avail = count - pos;
            if (avail <= 0)
                return 0;
            long skipped = (avail < n) ? avail : n;
            pos += skipped;
            return skipped;
        }
    }

    /**
     * Returns the number of bytes that can be read from this inputstream without
     * blocking.
     * <p/>
     * The <code>available</code> method of <code>FilteredAudioInputStream</code>
     * returns the sum of the the number of bytes remaining to be read in the
     * buffer (<code>count - pos</code>).
     * The result of calling the <code>available</code> method of the underlying
     * inputstream is not used, as this data will have to be filtered, and thus
     * may not be the same size after processing (although subclasses that do the
     * filtering should override this method and use the amount of data available
     * in the underlying inputstream).
     *
     * @return the number of bytes that can be read from this inputstream without
     * blocking.
     * @throws IOException if an I/O error occurs.
     * @see #in
     */
    public synchronized int available()
            throws IOException {
        checkIfStillOpen();
        return (count - pos);
    }

    /**
     * See the general contract of the <code>mark</code> method of
     * <code>InputStream</code>.
     *
     * @param readlimit the maximum limit of bytes that can be read before
     *                  the mark position becomes invalid.
     * @see #reset()
     */
    public synchronized void mark(final int readlimit) {
        if (readlimit > buf.length - pos) { // not enough room
            byte[] newbuf;
            if (readlimit <= buf.length) {
                newbuf = buf; // just shift buffer
            } else {
                newbuf = new byte[readlimit]; // need a new buffer
            }
            System.arraycopy(buf, pos, newbuf, 0, count - pos);
            buf = newbuf;
            count -= pos;
            pos = markpos = 0;
        } else {
            markpos = pos;
        }
        marklimit = readlimit;
    }

    /**
     * See the general contract of the <code>reset</code> method of
     * <code>InputStream</code>.
     * <p/>
     * If <code>markpos</code> is -1 (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code> is thrown.
     * Otherwise, <code>pos</code> is set equal to <code>markpos</code>.
     *
     * @throws IOException if this stream has not been marked or
     *                     if the mark has been invalidated.
     * @see #mark(int)
     */
    public synchronized void reset()
            throws IOException {
        checkIfStillOpen();
        if (markpos < 0)
            throw new IOException("Attempt to reset when no mark is valid");
        pos = markpos;
    }

    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. The <code>markSupported</code> method of
     * <code>FilteredAudioInputStream</code> returns <code>true</code>.
     *
     * @return a <code>boolean</code> indicating if this stream type supports
     * the <code>mark</code> and <code>reset</code> methods.
     * @see #mark(int)
     * @see #reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Closes this input stream and releases any system resources associated with
     * the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    public synchronized void close()
            throws IOException {
        if (in == null)
            return;
        in.close();
        in = null;
        buf = null;
        prebuf = null;
    }
}
