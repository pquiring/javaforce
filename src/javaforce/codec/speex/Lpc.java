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
 * Class: Lpc.java                                                            *
 *                                                                            *
 * Author: Marc GIMPEL                                                        *
 * Based on code by: Jean-Marc VALIN                                          *
 *                                                                            *
 * Date: 9th April 2003                                                       *
 *                                                                            *
 ******************************************************************************/

/* $Id$ */

/*
  Copyright 1992, 1993, 1994 by Jutta Degener and Carsten Bormann,
  Technische Universitaet Berlin

  Any use of this software is permitted provided that this notice is not
  removed and that neither the authors nor the Technische Universitaet Berlin
  are deemed to have made any representations as to the suitability of this
  software for any purpose nor are held responsible for any defects of
  this software.  THERE IS ABSOLUTELY NO WARRANTY FOR THIS SOFTWARE.

  As a matter of courtesy, the authors request to be informed about uses
  this software has found, about bugs in this software, and about any
  improvements that may be of general interest.

  Berlin, 28.11.1994
  Jutta Degener
  Carsten Bormann


   Code slightly modified by Jean-Marc Valin

   Speex License:

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of the Xiph.org Foundation nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR
   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package javaforce.codec.speex;

/**
 * LPC - and Reflection Coefficients.
 * <p/>
 * <p>The next two functions calculate linear prediction coefficients
 * and/or the related reflection coefficients from the first P_MAX+1
 * values of the autocorrelation function.
 * <p/>
 * <p>Invented by N. Levinson in 1947, modified by J. Durbin in 1959.
 *
 * @author Marc Gimpel, Wimba S.A. (mgimpel@horizonwimba.com)
 * @version $Revision$
 */
public class Lpc {
    /**
     * Returns minimum mean square error.
     *
     * @param lpc - float[0...p-1] LPC coefficients
     * @param ac  -  in: float[0...p] autocorrelation values
     * @param ref - out: float[0...p-1] reflection coef's
     * @param p
     * @return minimum mean square error.
     */
    public static float wld(final float[] lpc,
                            final float[] ac,
                            final float[] ref,
                            final int p) {
        int i, j;
        float r, error = ac[0];
        if (ac[0] == 0) {
            for (i = 0; i < p; i++)
                ref[i] = 0;
            return 0;
        }
        for (i = 0; i < p; i++) {
      /* Sum up this iteration's reflection coefficient. */
            r = -ac[i + 1];
            for (j = 0; j < i; j++) r -= lpc[j] * ac[i - j];
            ref[i] = r /= error;
      /*  Update LPC coefficients and total error. */
            lpc[i] = r;
            for (j = 0; j < i / 2; j++) {
                float tmp = lpc[j];
                lpc[j] += r * lpc[i - 1 - j];
                lpc[i - 1 - j] += r * tmp;
            }
            if ((i % 2) != 0)
                lpc[j] += lpc[j] * r;
            error *= 1.0 - r * r;
        }
        return error;
    }

    /**
     * Compute the autocorrelation
     * ,--,
     * ac(i) = >  x(n) * x(n-i)  for all n
     * `--'
     * for lags between 0 and lag-1, and x == 0 outside 0...n-1
     *
     * @param x   - in: float[0...n-1] samples x
     * @param ac  - out: float[0...lag-1] ac values
     * @param lag
     * @param n
     */
    public static void autocorr(final float[] x,
                                final float[] ac,
                                int lag,
                                final int n) {
        float d;
        int i;
        while (lag-- > 0) {
            for (i = lag, d = 0; i < n; i++)
                d += x[i] * x[i - lag];
            ac[lag] = d;
        }
    }
}
