//Raspberry PI GPIO

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#include "javaforce_pi_GPIO.h"

#include "..\common\register.h"

#define PAGE_SIZE (4*1024)
#define BLOCK_SIZE (4*1024)

// I/O access
volatile unsigned int *gpio;  //32bits (each bit is a port)

// GPIO setup macros. Always use INP_GPIO(x) before using OUT_GPIO(x) or SET_GPIO_ALT(x,y)
#define INP_GPIO(g) *(gpio+((g)/10)) &= ~(7<<(((g)%10)*3))
#define OUT_GPIO(g) *(gpio+((g)/10)) |=  (1<<(((g)%10)*3))
#define SET_GPIO_ALT(g,a) *(gpio+(((g)/10))) |= (((a)<=3?(a)+4:(a)==4?3:2)<<(((g)%10)*3))

#define GPIO_SET *(gpio+7)  // sets  bits which are 1 ignores bits which are 0
#define GPIO_CLR *(gpio+10) // clears bits which are 1 ignores bits which are 0

#define GET_GPIO(g) (*(gpio+13)&(1<<g)) // 0 if LOW, (1<<g) if HIGH

#define GPIO_PULL *(gpio+37) // Pull up/pull down
#define GPIO_PULLCLK0 *(gpio+38) // Pull up/pull down clock

//
// Set up a memory regions to access GPIO
//
JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_ninit
  (JNIEnv *env, jclass obj, jint addr)
{
  int  mem_fd;
  void *gpio_map;

  /* open /dev/mem */
  if ((mem_fd = open("/dev/mem", O_RDWR|O_SYNC) ) < 0) {
    printf("can't open /dev/mem \n");
    return JNI_FALSE;
  }

  gpio_map = mmap(
    NULL,           //Any adddress in our space will do
    BLOCK_SIZE,     //Map length
    PROT_READ|PROT_WRITE,// Enable reading & writting to mapped memory
    MAP_SHARED,     //Shared with other processes
    mem_fd,         //File to map
    addr            //Offset to GPIO peripheral
  );

  close(mem_fd); //No need to keep mem_fd open after mmap

  if (gpio_map == MAP_FAILED) {
    printf("mmap error %p\n", gpio_map);
    return JNI_FALSE;
  }

  // Always use volatile pointer!
  gpio = (volatile unsigned *)gpio_map;

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_configOutput
  (JNIEnv *env, jclass obj, jint bit)
{
  INP_GPIO(bit);  //Always use INP_GPIO before OUT_GPIO -- why???
  OUT_GPIO(bit);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_configInput
  (JNIEnv *env, jclass obj, jint bit)
{
  INP_GPIO(bit);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_write
  (JNIEnv *env, jclass obj, jint bit, jboolean value)
{
  if (value == JNI_TRUE) {
    GPIO_SET = 1 << bit;
  } else {
    GPIO_CLR = 1 << bit;
  }
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_read
  (JNIEnv *env, jclass obj, jint bit)
{
  return GET_GPIO(bit);
}

static JNINativeMethod javaforce_pi_GPIO[] = {
  {"ninit", "(I)Z", (void *)&Java_javaforce_pi_GPIO_ninit},
  {"configOutput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configOutput},
  {"configInput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configInput},
  {"write", "(IZ)Z", (void *)&Java_javaforce_pi_GPIO_write},
  {"read", "(I)Z", (void *)&Java_javaforce_pi_GPIO_read},
};

extern void gpio_register(JNIEnv *env);

void gpio_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/pi/GPIO");
  registerNatives(env, cls, javaforce_pi_GPIO, sizeof(javaforce_pi_GPIO)/sizeof(JNINativeMethod));
}
