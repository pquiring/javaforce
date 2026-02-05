static int strlen8(char* str) {
  int len = 0;
  while (*str != 0) {
    len++;
    str++;
  }
  return len;
}

static int strlen16(jchar* str) {
  int len = 0;
  while (*str != 0) {
    len++;
    str++;
  }
  return len;
}

static void strcpy16(jchar* dest, jchar* src) {
  while (*src != 0) {
    *(dest++) = *(src++);
  }
}

static void strcpy8(char* dest, char* src) {
  while (*src != 0) {
    *(dest++) = *(src++);
  }
}

static void strcpy16_8(jchar* dest, char* src) {
  while (*src != 0) {
    *(dest++) = *(src++);
  }
}

static void strcpy16_8_len(jchar* dest, char* src, int len) {
  while (len > 0) {
    *(dest++) = *(src++);
    len--;
  }
  *dest = 0;
}

static void strcpy8_16(char* dest, jchar* src) {
  while (*src != 0) {
    *(dest++) = *(src++);
  }
}

static void strcpy8_16_len(char* dest, jchar* src, int len) {
  while (len > 0) {
    *(dest++) = *(src++);
    len--;
  }
  *dest = 0;
}

//swap endianness of UTF-16 strings
static void strswap16(jchar* str) {
  int len = strlen16(str);
  char* str8 = (char*)str;
  char tmp;
  for(int i=0;i<len;i++) {
    tmp = str8[i * 2];
    str8[i * 2] = str8[i * 2 + 1];
    str8[i * 2 + 1] = tmp;
  }
}
