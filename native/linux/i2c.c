//Raspberry PI I2C

#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

#include "javaforce_jni_I2CJNI.h"

#include "../common/register.h"

static int file_i2c;

jboolean i2cSetup()
{
  if ((file_i2c = open("/dev/i2c-1", O_RDWR)) < 0)
  {
    printf("Failed to open the i2c bus.\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

jboolean i2cSetSlave(jint addr)
{
  if (ioctl(file_i2c, I2C_SLAVE, addr) < 0)
  {
    printf("Failed to acquire bus access and/or talk to slave.\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

jint i2cRead(jbyte* buf, jint bufsiz)
{
  return read(file_i2c, buf, bufsiz);
}

jboolean i2cWrite(jbyte* buf, jint bufsiz)
{
  int written = write(file_i2c, buf, bufsiz);
  return written == bufsiz;
}

extern "C" {
  JNIEXPORT jboolean (*_i2cSetup)() = &i2cSetup;
  JNIEXPORT jboolean (*_i2cSetSlave)(jint) = &i2cSetSlave;
  JNIEXPORT jboolean (*_i2cWrite)(jbyte*, jint) = &i2cWrite;
  JNIEXPORT jint (*_i2cRead)(jbyte*, jint) = &i2cRead;

  JNIEXPORT jboolean I2CAPIinit() {return JNI_TRUE;}
}
