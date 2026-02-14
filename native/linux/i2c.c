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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_I2CJNI_i2cSetup
  (JNIEnv *env, jobject obj)
{
  return i2cSetup();
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_I2CJNI_i2cSetSlave
  (JNIEnv *env, jobject obj, jint addr)
{
  return i2cSetSlave(addr);
}

jint i2cRead(jbyte* buf, jint bufsiz)
{
  return read(file_i2c, buf, bufsiz);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_I2CJNI_i2cRead
  (JNIEnv *e, jobject c, jbyteArray ba)
{
  jbyte *buf = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int readsiz = read(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, 0);
  return readsiz;
}

jboolean i2cWrite(jbyte* buf, jint bufsiz)
{
  int written = write(file_i2c, buf, bufsiz);
  return written == bufsiz;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_I2CJNI_i2cWrite
  (JNIEnv *e, jobject c, jbyteArray ba)
{
  jbyte *buf = e->GetByteArrayElements(ba, NULL);
  int bufsiz = e->GetArrayLength(ba);
  int written = write(file_i2c, buf, bufsiz);
  e->ReleaseByteArrayElements(ba, buf, JNI_ABORT);
  return written == bufsiz;
}

static JNINativeMethod javaforce_pi_I2C[] = {
  {"i2cSetup", "()Z", (void *)&Java_javaforce_jni_I2CJNI_i2cSetup},
  {"i2cSetSlave", "(I)Z", (void *)&Java_javaforce_jni_I2CJNI_i2cSetSlave},
  {"i2cWrite", "([BI)Z", (void *)&Java_javaforce_jni_I2CJNI_i2cWrite},
  {"i2cRead", "([BI)I", (void *)&Java_javaforce_jni_I2CJNI_i2cRead},
};

extern void i2c_register(JNIEnv *env);

void i2c_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/I2CJNI");
  registerNatives(env, cls, javaforce_pi_I2C, sizeof(javaforce_pi_I2C)/sizeof(JNINativeMethod));
}

extern "C" {
  JNIEXPORT jboolean (*_i2cSetup)() = &i2cSetup;
  JNIEXPORT jboolean (*_i2cSetSlave)(jint) = &i2cSetSlave;
  JNIEXPORT jboolean (*_i2cWrite)(jbyte*, jint) = &i2cWrite;
  JNIEXPORT jint (*_i2cRead)(jbyte*, jint) = &i2cRead;

  JNIEXPORT jboolean i2cinit() {return JNI_TRUE;}
}
