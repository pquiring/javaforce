#include "NIDAQmx.h"

// API Reference http://zone.ni.com/reference/en-XX/help/370471AF-01/

JF_LIB_HANDLE nidll = NULL;

#ifdef _WIN32
  #define JF_LIB_NAME "nicaiu.dll"
#else
  #define JF_LIB_NAME "nicaiu.so"
#endif

int32 (*__CFUNC _DAQmxCreateTask)(const char taskName[], TaskHandle *taskHandle);
int32 (*__CFUNC _DAQmxCreateAIVoltageChan)(TaskHandle taskHandle, const char physicalChannel[], const char nameToAssignToChannel[], int32 terminalConfig, float64 minVal, float64 maxVal, int32 units, const char customScaleName[]);
int32 (*__CFUNC _DAQmxCreateDIChan)(TaskHandle taskHandle, const char lines[], const char nameToAssignToLines[], int32 lineGrouping);
int32 (*__CFUNC _DAQmxCreateCIFreqChan)(TaskHandle taskHandle, const char counter[], const char nameToAssignToChannel[], float64 minVal, float64 maxVal, int32 units, int32 edge, int32 measMethod, float64 measTime, uInt32 divisor, const char customScaleName[]);
int32 (*__CFUNC _DAQmxCfgSampClkTiming)(TaskHandle taskHandle, const char source[], float64 rate, int32 activeEdge, int32 sampleMode, uInt64 sampsPerChan);
int32 (*__CFUNC _DAQmxCfgImplicitTiming)(TaskHandle taskHandle, int32 sampleMode, uInt64 sampsPerChan);
int32 (*__CFUNC _DAQmxStartTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxStopTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxClearTask)(TaskHandle taskHandle);
int32 (*__CFUNC _DAQmxReadAnalogF64)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, bool32 fillMode, float64 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxReadBinaryU32)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, bool32 fillMode, uInt32 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxReadDigitalU32)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, bool32 fillMode, uInt32 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxReadCounterF64)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, float64 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxReadCounterU32)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, uInt32 readArray[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxReadCtrFreq)(TaskHandle taskHandle, int32 numSampsPerChan, float64 timeout, bool32 interleaved, float64 readArrayFrequency[], float64 readArrayDutyCycle[], uInt32 arraySizeInSamps, int32 *sampsPerChanRead, bool32 *reserved);
int32 (*__CFUNC _DAQmxGetExtendedErrorInfo)(char errorString[], uInt32 bufferSize);
int32 (*__CFUNC _DAQmxSetCIFreqTerm)(TaskHandle taskHandle, const char channel[], const char *data);
int32 (*__CFUNC _DAQmxSetCICtrTimebaseRate)(TaskHandle taskHandle, const char channel[], float64 data);

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_daqInit
  (JNIEnv *e, jclass cls)
{
  nidll = JF_LIB_OPEN(JF_LIB_NAME JF_LIB_OPTS);
  if (nidll == NULL) {
    printf("Could not find:%s\n", JF_LIB_NAME);
    return JNI_FALSE;
  }
  getFunction(nidll, (void**)&_DAQmxCreateTask, "DAQmxCreateTask");
  getFunction(nidll, (void**)&_DAQmxCreateAIVoltageChan, "DAQmxCreateAIVoltageChan");
  getFunction(nidll, (void**)&_DAQmxCreateDIChan, "DAQmxCreateDIChan");
  getFunction(nidll, (void**)&_DAQmxCreateCIFreqChan, "DAQmxCreateCIFreqChan");
  getFunction(nidll, (void**)&_DAQmxCfgSampClkTiming, "DAQmxCfgSampClkTiming");
  getFunction(nidll, (void**)&_DAQmxCfgImplicitTiming, "DAQmxCfgImplicitTiming");
  getFunction(nidll, (void**)&_DAQmxStartTask, "DAQmxStartTask");
  getFunction(nidll, (void**)&_DAQmxStopTask, "DAQmxStopTask");
  getFunction(nidll, (void**)&_DAQmxClearTask, "DAQmxClearTask");
  getFunction(nidll, (void**)&_DAQmxReadAnalogF64, "DAQmxReadAnalogF64");
  getFunction(nidll, (void**)&_DAQmxReadBinaryU32, "DAQmxReadBinaryU32");
  getFunction(nidll, (void**)&_DAQmxReadDigitalU32, "DAQmxReadDigitalU32");
  getFunction(nidll, (void**)&_DAQmxReadCounterF64, "DAQmxReadCounterF64");
  getFunction(nidll, (void**)&_DAQmxReadCounterU32, "DAQmxReadCounterU32");
  getFunction(nidll, (void**)&_DAQmxReadCtrFreq, "DAQmxReadCtrFreq");
  getFunction(nidll, (void**)&_DAQmxGetExtendedErrorInfo, "DAQmxGetExtendedErrorInfo");
  getFunction(nidll, (void**)&_DAQmxSetCIFreqTerm, "DAQmxSetCIFreqTerm");
  getFunction(nidll, (void**)&_DAQmxSetCICtrTimebaseRate, "DAQmxSetCICtrTimebaseRate");

  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_controls_ni_DAQmx_createTask
  (JNIEnv *e, jclass cls)
{
  TaskHandle taskHandle=0;
  (*_DAQmxCreateTask)("", &taskHandle);
  return (jlong)taskHandle;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_createChannelAnalog
  (JNIEnv *e, jclass cls, jlong task, jstring dev, jdouble rate, jlong samples, jdouble min, jdouble max)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 status = 0;
  const char *_dev = e->GetStringUTFChars(dev, NULL);
  status = (*_DAQmxCreateAIVoltageChan)(taskHandle, _dev, "", DAQmx_Val_Cfg_Default, min, max, DAQmx_Val_Volts, NULL);
  e->ReleaseStringUTFChars(dev, _dev);
  if (status == 0) {
    status = (*_DAQmxCfgSampClkTiming)(taskHandle, "", rate, DAQmx_Val_Rising, DAQmx_Val_ContSamps, samples);
  }
  return status == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_createChannelDigital
  (JNIEnv *e, jclass cls, jlong task, jstring dev, jdouble rate, jlong samples)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 status = 0;
  const char *_dev = e->GetStringUTFChars(dev, NULL);
  status = (*_DAQmxCreateDIChan)(taskHandle, _dev, "", DAQmx_Val_ChanPerLine);
  e->ReleaseStringUTFChars(dev, _dev);
  if (status == 0) {
    status = (*_DAQmxCfgSampClkTiming)(taskHandle, "", rate, DAQmx_Val_Rising, DAQmx_Val_ContSamps, samples);
  }
  return status == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_createChannelCounter
  (JNIEnv *e, jclass cls, jlong task, jstring dev, jdouble rate, jlong samples, jdouble min, jdouble max, jstring term, jdouble measureTime, jint divisor)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 status = 0;
  const char *_dev = e->GetStringUTFChars(dev, NULL);
  const char *_term = e->GetStringUTFChars(term, NULL);
  status = (*_DAQmxCreateCIFreqChan)(taskHandle, _dev, "", min, max, DAQmx_Val_Hz, DAQmx_Val_Rising, DAQmx_Val_LowFreq1Ctr, measureTime, divisor, NULL);
  if (status == 0) {
    status = (*_DAQmxSetCIFreqTerm)(taskHandle, _dev, _term);
  }
  if (status == 0) {
//    status = (*_DAQmxCfgImplicitTiming)(taskHandle, DAQmx_Val_ContSamps, samples);
  }
  if (status == 0) {
//    status = (*_DAQmxSetCICtrTimebaseRate)(taskHandle, _dev, rate);
  }
  e->ReleaseStringUTFChars(dev, _dev);
  e->ReleaseStringUTFChars(term, _term);
  return status == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_controls_ni_DAQmx_startTask
  (JNIEnv *e, jclass cls, jlong task)
{
  TaskHandle taskHandle=(TaskHandle)task;
  return (*_DAQmxStartTask)(taskHandle) == 0;
}

JNIEXPORT jint JNICALL Java_javaforce_controls_ni_DAQmx_readTaskAnalog
  (JNIEnv *e, jclass cls, jlong task, jint numchs, jdoubleArray data)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 read = 0;
  void *_data = e->GetDoubleArrayElements(data, NULL);
  int size = e->GetArrayLength(data);
  (*_DAQmxReadAnalogF64)(taskHandle, size / numchs, 10.0, DAQmx_Val_GroupByChannel, (float64*)_data, size, &read, NULL);
  e->ReleaseDoubleArrayElements(data, (jdouble*)_data, JNI_COMMIT);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_controls_ni_DAQmx_readTaskBinary
  (JNIEnv *e, jclass cls, jlong task, jint numchs, jintArray data)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 read = 0;
  void *_data = e->GetIntArrayElements(data, NULL);
  int size = e->GetArrayLength(data);
  (*_DAQmxReadBinaryU32)(taskHandle, size / numchs, 10.0, DAQmx_Val_GroupByChannel, (uInt32*)_data, size, &read, NULL);
  e->ReleaseIntArrayElements(data, (jint*)_data, JNI_COMMIT);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_controls_ni_DAQmx_readTaskDigital
  (JNIEnv *e, jclass cls, jlong task, jint numchs, jintArray data)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 read = 0;
  void *_data = e->GetIntArrayElements(data, NULL);
  int size = e->GetArrayLength(data);
  (*_DAQmxReadDigitalU32)(taskHandle, size / numchs, 10.0, DAQmx_Val_GroupByChannel, (uInt32*)_data, size, &read, NULL);
  e->ReleaseIntArrayElements(data, (jint*)_data, JNI_COMMIT);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_controls_ni_DAQmx_readTaskCounter
  (JNIEnv *e, jclass cls, jlong task, jint numchs, jdoubleArray data)
{
  TaskHandle taskHandle=(TaskHandle)task;
  int32 read = 0;
  void *_data = e->GetDoubleArrayElements(data, NULL);
  int size = e->GetArrayLength(data);
  (*_DAQmxReadCounterF64)(taskHandle, size / numchs, 10.0, (float64*)_data, size, &read, NULL);
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
