#include <emmintrin.h>

union MD128 {
  __m128i md;
  int i32[4];
};

static int simd_diff(jint *pc1, jint *pc2, int size_4) {
  MD128 p1, p2, add, mask;
  int diff = 0;
  p1.md = _mm_setzero_si128();
  p2.md = _mm_setzero_si128();
  add.md = _mm_set_epi32(0x00080808,0x00080808,0x00080808,0x00080808);
  mask.md = _mm_set_epi32(0x00f0f0f0,0x00f0f0f0,0x00f0f0f0,0x00f0f0f0);
  for(int i=0;i<size_4;i++) {
    p1.i32[0] = *(pc1++);
    p1.i32[1] = *(pc1++);
    p1.i32[2] = *(pc1++);
    p1.i32[3] = *(pc1++);
    p1.md = _mm_adds_epu8(p1.md, add.md);
    p1.md = _mm_and_si128(p1.md, mask.md);
    p2.i32[0] = *(pc2++);
    p2.i32[1] = *(pc2++);
    p2.i32[2] = *(pc2++);
    p2.i32[3] = *(pc2++);
    p2.md = _mm_adds_epu8(p2.md, add.md);
    p2.md = _mm_and_si128(p2.md, mask.md);
    if ( p1.i32[0] != p2.i32[0] ) diff++;
    if ( p1.i32[1] != p2.i32[1] ) diff++;
    if ( p1.i32[2] != p2.i32[2] ) diff++;
    if ( p1.i32[3] != p2.i32[3] ) diff++;
  }
  return diff;
}
