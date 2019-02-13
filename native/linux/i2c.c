//Raspberry PI I2C

#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

#include "javaforce_pi_I2C.h"

static int file_i2c;

JNIEXPORT jboolean JNICALL Java_javaforce_pi_I2C_init
  (JNIEnv *env, jclass obj, jint addr)
{
  if ((file_i2c = open("/dev/i2c-1", O_RDWR)) < 0)
  {
    printf("Failed to open the i2c bus");
    return JNI_FALSE;
  }

  if (ioctl(file_i2c, I2C_SLAVE, addr) < 0)
  {
    printf("Failed to acquire bus access and/or talk to slave.\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;	
}

JNIEXPORT jint JNICALL Java_javaforce_pi_I2C_read
  (JNIEnv *env, jclass obj, jByteArray ba)
{
  jbyte *bufr = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int read = read(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, 0);
  return read;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_I2C_write
  (JNIEnv *env, jclass obj, jByteArray ba)
{
  jbyte *bufr = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int written = write(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, 0);
  return written == bufsiz;
}
