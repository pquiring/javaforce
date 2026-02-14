//Raspberry PI GPIO

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#include "javaforce_jni_GPIOJNI.h"

#include "../common/register.h"

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
jboolean gpioSetup(jint addr)
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GPIOJNI_gpioSetup
  (JNIEnv *env, jobject obj, jint addr)
{
  return gpioSetup(addr);
}

jboolean gpioConfigOutput(jint bit) {
  INP_GPIO(bit);  //Always use INP_GPIO before OUT_GPIO -- why???
  OUT_GPIO(bit);
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GPIOJNI_gpioConfigOutput
  (JNIEnv *env, jobject obj, jint bit)
{
  return gpioConfigOutput(bit);
}

jboolean gpioConfigInput(jint bit) {
  INP_GPIO(bit);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GPIOJNI_gpioConfigInput
  (JNIEnv *env, jobject obj, jint bit)
{
  return gpioConfigOutput(bit);
}

jboolean gpioWrite(jint bit, jboolean value)
{
  if (value == JNI_TRUE) {
    GPIO_SET = 1 << bit;
  } else {
    GPIO_CLR = 1 << bit;
  }
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GPIOJNI_gpioWrite
  (JNIEnv *env, jobject obj, jint bit, jboolean value)
{
  return gpioWrite(bit, value);
}

jboolean gpioRead(jint bit)
{
  return GET_GPIO(bit);
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GPIOJNI_gpioRead
  (JNIEnv *env, jobject obj, jint bit)
{
  return gpioRead(bit);
}

static JNINativeMethod javaforce_pi_GPIO[] = {
  {"gpioSetup", "(I)Z", (void *)&Java_javaforce_jni_GPIOJNI_gpioSetup},
  {"gpioConfigOutput", "(I)Z", (void *)&Java_javaforce_jni_GPIOJNI_gpioConfigOutput},
  {"gpioConfigInput", "(I)Z", (void *)&Java_javaforce_jni_GPIOJNI_gpioConfigInput},
  {"gpioWrite", "(IZ)Z", (void *)&Java_javaforce_jni_GPIOJNI_gpioWrite},
  {"gpioRead", "(I)Z", (void *)&Java_javaforce_jni_GPIOJNI_gpioRead},
};

extern void gpio_register(JNIEnv *env);

void gpio_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/GPIOJNI");
  registerNatives(env, cls, javaforce_pi_GPIO, sizeof(javaforce_pi_GPIO)/sizeof(JNINativeMethod));
}

export "C" {
  JNIEXPORT jboolean (*_gpioSetup)(jint) = gpioSetup;
  JNIEXPORT jboolean (*_gpioConfigOutput)(jint) = gpioConfigOutput;
  JNIEXPORT jboolean (*_gpioConfigInput)(jint) = gpioConfigInput;
  JNIEXPORT jboolean (*_gpioWrite)(jint,jboolean) = gpioWrite;
  JNIEXPORT jboolean (*_gpioRead)(jint) = gpioRead;

  JNIEXPORT jboolean gpioinit() {return JNI_TRUE;}
}
