//Raspberry PI GPIO

#define PI_1_PERI_BASE         0x20000000
#define GPIO_1_BASE            (PI_1_PERI_BASE + 0x200000) /* GPIO controller */

#define PI_2_3_PERI_BASE       0x3f000000
#define GPIO_2_3_BASE          (PI_2_3_PERI_BASE + 0x200000) /* GPIO controller */

#define PI_4_PERI_BASE         0x7E000000
#define GPIO_4_BASE            (PI_4_PERI_BASE + 0x200000) /* GPIO controller */

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#include "javaforce_pi_GPIO.h"

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
JNIEXPORT jboolean JNICALL Java_javaforce_pi_GPIO_init
  (JNIEnv *env, jclass obj)
{
  int  mem_fd;
  void *gpio_map;

  /* open /dev/mem */
  if ((mem_fd = open("/dev/mem", O_RDWR|O_SYNC) ) < 0) {
    printf("can't open /dev/mem \n");
    return JNI_FALSE;
  }

  /* mmap GPIO RPi4 */
  gpio_map = mmap(
    NULL,           //Any adddress in our space will do
    BLOCK_SIZE,     //Map length
    PROT_READ|PROT_WRITE,// Enable reading & writting to mapped memory
    MAP_SHARED,     //Shared with other processes
    mem_fd,         //File to map
    GPIO_4_BASE     //Offset to GPIO peripheral
  );

  if (gpio_map != MAP_FAILED) {
    close(mem_fd); //No need to keep mem_fd open after mmap
    // Always use volatile pointer!
    gpio = (volatile unsigned *)gpio_map;
    printf("GPIO:Detected Raspberry PI 4\n");
    return JNI_TRUE;
  }

  /* mmap GPIO RPi2/3 */
  gpio_map = mmap(
    NULL,           //Any adddress in our space will do
    BLOCK_SIZE,     //Map length
    PROT_READ|PROT_WRITE,// Enable reading & writting to mapped memory
    MAP_SHARED,     //Shared with other processes
    mem_fd,         //File to map
    GPIO_2_3_BASE   //Offset to GPIO peripheral
  );

  if (gpio_map != MAP_FAILED) {
    close(mem_fd); //No need to keep mem_fd open after mmap
    // Always use volatile pointer!
    gpio = (volatile unsigned *)gpio_map;
    printf("GPIO:Detected Raspberry PI 2/3\n");
    return JNI_TRUE;
  }

  /* mmap GPIO RPi1 */
  gpio_map = mmap(
    NULL,           //Any adddress in our space will do
    BLOCK_SIZE,     //Map length
    PROT_READ|PROT_WRITE,// Enable reading & writting to mapped memory
    MAP_SHARED,     //Shared with other processes
    mem_fd,         //File to map
    GPIO_1_BASE     //Offset to GPIO peripheral
  );

  close(mem_fd); //No need to keep mem_fd open after mmap

  if (gpio_map == MAP_FAILED) {
    printf("mmap error %p\n", gpio_map);
    return JNI_FALSE;
  }

  // Always use volatile pointer!
  gpio = (volatile unsigned *)gpio_map;
  printf("GPIO:Detected Raspberry PI 1\n");

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
