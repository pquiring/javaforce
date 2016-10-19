#include "NIDAQmx.h"

LIB_HANDLE nidll = NULL;

#ifdef __WIN32__
  #define LIB_NAME "nicaiu.dll"
#else
  #define LIB_NAME "nicaiu.so"
#endif

int32 (*__CFUNC _DAQmxCreateTask)(const char taskName[], TaskHandle *taskHandle);
int32 (*__CFUNC _DAQmxCreateAIVoltageChan)(TaskHandle taskHandle, const char physicalChannel[], const char nameToAssignToChannel[], int32 terminalConfig, float64 minVal, float64 maxVal, int32 units, const char customScaleName[]);
int32 (*__CFUNC _DAQmxCfgSampClkTiming)(TaskHandle taskHandle, const char source[], float64 rate, int32 activeEdge, int32 sampleMode, uInt64 sampsPerChan);
int32 (*__CFUNC _DAQmxStartTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxStopTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxClearTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxReadAnalogF64)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, bool32 fillMode, float64 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxGetExtendedErrorInfo)(char errorString[], uInt32 bufferSize);

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_daqInit
  (JNIEnv *e, jclass cls)
{
  nidll = LIB_OPEN(LIB_NAME LIB_OPTS);
  if (nidll == NULL) {
    printf("Could not find:%s\n", LIB_NAME);
    return JNI_FALSE;
  }
  getFunction(nidll, (void**)&_DAQmxCreateTask, "DAQmxCreateTask");
  getFunction(nidll, (void**)&_DAQmxCreateAIVoltageChan, "DAQmxCreateAIVoltageChan");
  getFunction(nidll, (void**)&_DAQmxCfgSampClkTiming, "DAQmxCfgSampClkTiming");
  getFunction(nidll, (void**)&_DAQmxStartTask, "DAQmxStartTask");
  getFunction(nidll, (void**)&_DAQmxStopTask, "DAQmxStopTask");
  getFunction(nidll, (void**)&_DAQmxClearTask, "DAQmxClearTask");
  getFunction(nidll, (void**)&_DAQmxReadAnalogF64, "DAQmxReadAnalogF64");
  getFunction(nidll, (void**)&_DAQmxGetExtendedErrorInfo, "DAQmxGetExtendedErrorInfo");

  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_controls_ni_DAQmx_createTask
  (JNIEnv *e, jclass cls)
{
  TaskHandle taskHandle=0;
  (*_DAQmxCreateTask)("", &taskHandle);
  return (jlong)taskHandle;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_createChannel
  (JNIEnv *e, jclass cls, jlong task, jint type, jstring dev)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 status = 0;
  const char *_dev = e->GetStringUTFChars(dev, NULL);
  switch (type) {
    case javaforce_controls_ni_DAQmx_AI_Voltage: status = (*_DAQmxCreateAIVoltageChan)(taskHandle, _dev, "", DAQmx_Val_Cfg_Default, -10.0, 10.0, DAQmx_Val_Volts, NULL); break;
  }
  e->ReleaseStringUTFChars(dev, _dev);
  return status == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_configTiming
  (JNIEnv *e, jclass cls, jlong task, jdouble rate, jlong samples)
{
  TaskHandle taskHandle=(TaskHandle)task;
  return (*_DAQmxCfgSampClkTiming)(taskHandle, "", rate, DAQmx_Val_Rising, DAQmx_Val_FiniteSamps, samples) == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_startTask
  (JNIEnv *e, jclass cls, jlong task)
{
  TaskHandle taskHandle=(TaskHandle)task;
  return (*_DAQmxStartTask)(taskHandle) == 0;
}

JNIEXPORT jint JNICALL Java_javaforce_controls_ni_DAQmx_readTask
  (JNIEnv *e, jclass cls, jlong task, jdoubleArray data)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 read = 0;
  void *_data = e->GetDoubleArrayElements(data, NULL);
  (*_DAQmxReadAnalogF64)(taskHandle, 1000, 10.0, DAQmx_Val_GroupByChannel, (float64*)_data, 1000, &read, NULL);
  e->ReleaseDoubleArrayElements(data, (jdouble*)_data, JNI_COMMIT);
  return read;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_stopTask
  (JNIEnv *e, jclass cls, jlong task)
{
  TaskHandle taskHandle=(TaskHandle)task;
  return (*_DAQmxStopTask)(taskHandle) == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_clearTask
  (JNIEnv *e, jclass cls, jlong task)
{
  TaskHandle taskHandle=(TaskHandle)task;
  return (*_DAQmxClearTask)(taskHandle) == 0;
}

JNIEXPORT void JNICALL Java_javaforce_controls_ni_DAQmx_printError
  (JNIEnv *e, jclass cls)
{
  char errmsg[2048];
  (*_DAQmxGetExtendedErrorInfo)(errmsg, 2048);
  printf("NI.Error=%s\n", errmsg);
}
