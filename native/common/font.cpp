#define STB_TRUETYPE_IMPLEMENTATION
#include "../stb/stb_truetype.h"

//#define FONT_DEBUG

JNIEXPORT jint JNICALL Java_javaforce_ui_Font_loadFont
  (JNIEnv *e, jclass c, jbyteArray fontdata, jint ptSize, jintArray fontinfo, jintArray coords, jintArray glyph, jintArray cps, jbyteArray pixels, jint px, jint py)
{
  stbtt_fontinfo font;
  float scale;
  int ascent, descent, linegap;
  int baseline, scale_descent, scale_linegap;
  jboolean isCopy;

  uint8 *fontdata_ptr = (uint8*)e->GetPrimitiveArrayCritical(fontdata, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int *fontinfo_ptr = (int*)e->GetPrimitiveArrayCritical(fontinfo, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int *coords_ptr = (int*)e->GetPrimitiveArrayCritical(coords, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int *glyph_ptr = (int*)e->GetPrimitiveArrayCritical(glyph, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int *cps_ptr = (int*)e->GetPrimitiveArrayCritical(cps, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  uint8 *pixels_ptr = (uint8*)e->GetPrimitiveArrayCritical(pixels, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  int cps_len = e->GetArrayLength(cps);

  if (!stbtt_InitFont(&font, fontdata_ptr, 0)) {
    printf("InitFont failed!");
    return JNI_FALSE;
  }
  scale = stbtt_ScaleForPixelHeight(&font, ptSize);
#ifdef FONT_DEBUG
  printf("Font scale=%f\n", scale);
#endif
  stbtt_GetFontVMetrics(&font, &ascent, &descent, &linegap);
  baseline = (int) (ascent * scale);  //scale_ascent
  scale_descent = (int) (descent * scale);
  scale_linegap = (int) (linegap * scale);
  fontinfo_ptr[0] = baseline;  //scale_ascent
  fontinfo_ptr[1] = scale_descent;
  fontinfo_ptr[2] = scale_linegap;
#ifdef FONT_DEBUG
  printf("Font ascent=%d descent=%d linegap=%d\n", baseline, scale_descent, scale_linegap);
#endif

  float xpos = 2.0f;
  float ypos = scale_linegap;
  int max_height = 0;
  int cpos = 0;
  int gpos = 0;
  int drawn = 0;
  int max_ascent = 0;
  int max_descent = 0;

  for(int cps_pos=0;cps_pos<cps_len;cps_pos++) {
    int cp = cps_ptr[cps_pos];
#ifdef FONT_DEBUG
    printf("Gylph cp=%d", cp);
#endif
    int advance, xoffset, x1, y1, x2, y2;
    float x_shift = xpos - (float) floor(xpos);
    float y_shift = 0;
    stbtt_GetCodepointHMetrics(&font, cp, &advance, &xoffset);
    stbtt_GetCodepointBitmapBoxSubpixel(&font, cp, scale, scale, x_shift, y_shift, &x1, &y1, &x2, &y2);
    //NOTE : x1,y1 = top-left x2,y2 = bottom-right
    int width = abs(x2-x1)+1;
    int height = abs(y2-y1)+1;
#ifdef FONT_DEBUG
    printf(" size=%dx%d p1=%d,%d p2=%d,%d", width, height, x1, y1, x2, y2);
#endif
    if (xpos + x1 + width + 2.0f > px) {
      //wrap to left side
      xpos = 2.0f;
      ypos += max_height;
      ypos += scale_linegap;
      max_height = 0;
    }
    if (height > max_height) {
      max_height = height;
    }
    if ((xpos + x1 + width + 2.0f > px) || (ypos + -y2 + height + 2.0f > py)) {
      //font too large for buffer
#ifdef FONT_DEBUG
      printf("Font end at cp=%d\n", cp);
#endif
      break;
    }
    if (y1 < max_ascent) {
      max_ascent = y1;
    }
    if (y2 > max_descent) {
      max_descent = y2;
    }
    glyph_ptr[gpos++] = y1;  //baseline
    glyph_ptr[gpos++] = (advance * scale);  //advance
    int ixpos = xpos;
    int iypos = ypos;
    int offset = ((iypos + baseline + y1) * px) + (ixpos + x1);
#ifdef FONT_DEBUG
    printf(" offset=%d", offset);
#endif
    stbtt_MakeCodepointBitmapSubpixel(&font, pixels_ptr + offset, width, height, px, scale, scale, x_shift, y_shift, cp);
    coords_ptr[cpos++] = xpos + x1;
    coords_ptr[cpos++] = ypos + baseline + y1;
    coords_ptr[cpos++] = xpos + x2;
    coords_ptr[cpos++] = ypos + baseline + y2;
    xpos += (advance * scale);
    xpos += 2.0f;  //seeing some overlapping
#ifdef FONT_DEBUG
    printf("\n");
#endif
    drawn++;
  }

  fontinfo_ptr[3] = max_ascent;
  fontinfo_ptr[4] = max_descent;
#ifdef FONT_DEBUG
  printf("Font:max_ascent=%d max_descent=%d\n", max_ascent, max_descent);
#endif

  e->ReleasePrimitiveArrayCritical(fontdata, fontdata_ptr, JNI_COMMIT);
  e->ReleasePrimitiveArrayCritical(fontinfo, fontinfo_ptr, JNI_COMMIT);
  e->ReleasePrimitiveArrayCritical(coords, coords_ptr, JNI_COMMIT);
  e->ReleasePrimitiveArrayCritical(glyph, glyph_ptr, JNI_COMMIT);
  e->ReleasePrimitiveArrayCritical(cps, cps_ptr, JNI_COMMIT);
  e->ReleasePrimitiveArrayCritical(pixels, pixels_ptr, JNI_COMMIT);

  return drawn;
}

/*/

Font Metrics:

         ?...............
         .              .
         . x---------|  .
         . |x1,y1    |  .
         . |         |  .
         . |         | ascent
         . |         |  .
         . |         |  .
baseline o-----------|--o
         . |         |  .
         . |         |  .
         . |         | descent
         . |         |  .
         . |    x2,y2|  .
         . |---------x  .
         .              .
         ....advance.....

  o = 0,0 (origin)
  ? = xpos, ypos

Notes:
  y1 is usually negative (which makes ascent negative)

/*/
