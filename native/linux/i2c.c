//Raspberry PI I2C

#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

#include "javaforce_pi_I2C.h"

#include "..\common\register.h"

static int file_i2c;

JNIEXPORT jboolean JNICALL Java_javaforce_pi_I2C_init
  (JNIEnv *env, jclass obj)
{
  if ((file_i2c = open("/dev/i2c-1", O_RDWR)) < 0)
  {
    printf("Failed to open the i2c bus.\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_I2C_setSlave
  (JNIEnv *env, jclass obj, jint addr)
{
  if (ioctl(file_i2c, I2C_SLAVE, addr) < 0)
  {
    printf("Failed to acquire bus access and/or talk to slave.\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_javaforce_pi_I2C_read
  (JNIEnv *e, jclass c, jbyteArray ba)
{
  jbyte *buf = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int readsiz = read(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, 0);
  return readsiz;
}

JNIEXPORT jboolean JNICALL Java_javaforce_pi_I2C_write
  (JNIEnv *e, jclass c, jbyteArray ba)
{
  jbyte *buf = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int written = write(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, JNI_ABORT);
  return written == bufsiz;
}

static JNINativeMethod javaforce_pi_I2C[] = {
  {"init", "()Z", (void *)&Java_javaforce_pi_I2C_init},
  {"setSlave", "(I)Z", (void *)&Java_javaforce_pi_I2C_setSlave},
  {"write", "([B)Z", (void *)&Java_javaforce_pi_I2C_write},
  {"read", "([B)I", (void *)&Java_javaforce_pi_I2C_read},
};

extern void i2c_register(JNIEnv *env);

void i2c_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/pi/I2C");
  registerNatives(env, cls, javaforce_pi_I2C, sizeof(javaforce_pi_I2C)/sizeof(JNINativeMethod));
}
