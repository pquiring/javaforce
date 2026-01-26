//OpenCL

#define CL_TARGET_OPENCL_VERSION 300

#include "../opencl/CL/cl.h"
#include "../opencl/CL/cl_function_types.h"

#include "register.h"

JF_LIB_HANDLE opencl = NULL;

static jboolean opencl_loaded = JNI_FALSE;
static jboolean opencl_debug = JNI_FALSE;
static jboolean opencl_test = JNI_FALSE;

//functions
clBuildProgram_fn _cl_BuildProgram;
clCloneKernel_fn _cl_CloneKernel;
clCompileProgram_fn _cl_CompileProgram;
clCreateBuffer_fn _cl_CreateBuffer;
clCreateBufferWithProperties_fn _cl_CreateBufferWithProperties;
//clCreateCommandQueue_fn _cl_CreateCommandQueue;  //deprecated
clCreateCommandQueueWithProperties_fn _cl_CreateCommandQueueWithProperties;
clCreateContext_fn _cl_CreateContext;
clCreateContextFromType_fn _cl_CreateContextFromType;
//clCreateFromGLBuffer_fn _cl_CreateFromGLBuffer;
//clCreateFromGLRenderbuffer_fn _cl_CreateFromGLRenderbuffer;
//clCreateFromGLTexture_fn _cl_CreateFromGLTexture;
//clCreateFromGLTexture2D_fn _cl_CreateFromGLTexture2D;
//clCreateFromGLTexture3D_fn _cl_CreateFromGLTexture3D;
clCreateImage_fn _cl_CreateImage;
//clCreateImage2D_fn _cl_CreateImage2D;  //deprecated
//clCreateImage3D_fn _cl_CreateImage3D;  //deprecated
clCreateImageWithProperties_fn _cl_CreateImageWithProperties;
clCreateKernel_fn _cl_CreateKernel;
clCreateKernelsInProgram_fn _cl_CreateKernelsInProgram;
clCreatePipe_fn _cl_CreatePipe;
clCreateProgramWithBinary_fn _cl_CreateProgramWithBinary;
clCreateProgramWithBuiltInKernels_fn _cl_CreateProgramWithBuiltInKernels;
clCreateProgramWithIL_fn _cl_CreateProgramWithIL;
clCreateProgramWithSource_fn _cl_CreateProgramWithSource;
//clCreateSampler_fn _cl_CreateSampler;  //deprecated
clCreateSamplerWithProperties_fn _cl_CreateSamplerWithProperties;
clCreateSubBuffer_fn _cl_CreateSubBuffer;
clCreateSubDevices_fn _cl_CreateSubDevices;
clCreateUserEvent_fn _cl_CreateUserEvent;
//clEnqueueAcquireGLObjects_fn _cl_EnqueueAcquireGLObjects;
//clEnqueueBarrier_fn _cl_EnqueueBarrier;  //deprecated
clEnqueueBarrierWithWaitList_fn _cl_EnqueueBarrierWithWaitList;
clEnqueueCopyBuffer_fn _cl_EnqueueCopyBuffer;
clEnqueueCopyBufferRect_fn _cl_EnqueueCopyBufferRect;
clEnqueueCopyBufferToImage_fn _cl_EnqueueCopyBufferToImage;
clEnqueueCopyImage_fn _cl_EnqueueCopyImage;
clEnqueueCopyImageToBuffer_fn _cl_EnqueueCopyImageToBuffer;
clEnqueueFillBuffer_fn _cl_EnqueueFillBuffer;
clEnqueueFillImage_fn _cl_EnqueueFillImage;
clEnqueueMapBuffer_fn _cl_EnqueueMapBuffer;
clEnqueueMapImage_fn _cl_EnqueueMapImage;
//clEnqueueMarker_fn _cl_EnqueueMarker;  //deprecated
clEnqueueMarkerWithWaitList_fn _cl_EnqueueMarkerWithWaitList;
clEnqueueMigrateMemObjects_fn _cl_EnqueueMigrateMemObjects;
clEnqueueNativeKernel_fn _cl_EnqueueNativeKernel;
clEnqueueNDRangeKernel_fn _cl_EnqueueNDRangeKernel;
clEnqueueReadBuffer_fn _cl_EnqueueReadBuffer;
clEnqueueReadBufferRect_fn _cl_EnqueueReadBufferRect;
clEnqueueReadImage_fn _cl_EnqueueReadImage;
//clEnqueueReleaseGLObjects_fn _cl_EnqueueReleaseGLObjects;
clEnqueueSVMFree_fn _cl_EnqueueSVMFree;
clEnqueueSVMMap_fn _cl_EnqueueSVMMap;
clEnqueueSVMMemcpy_fn _cl_EnqueueSVMMemcpy;
clEnqueueSVMMemFill_fn _cl_EnqueueSVMMemFill;
clEnqueueSVMMigrateMem_fn _cl_EnqueueSVMMigrateMem;
clEnqueueSVMUnmap_fn _cl_EnqueueSVMUnmap;
//clEnqueueTask_fn _cl_EnqueueTask;  //deprecated
clEnqueueUnmapMemObject_fn _cl_EnqueueUnmapMemObject;
//clEnqueueWaitForEvents_fn _cl_EnqueueWaitForEvents;  //deprecated
clEnqueueWriteBuffer_fn _cl_EnqueueWriteBuffer;
clEnqueueWriteBufferRect_fn _cl_EnqueueWriteBufferRect;
clEnqueueWriteImage_fn _cl_EnqueueWriteImage;
clFinish_fn _cl_Finish;
clFlush_fn _cl_Flush;
clGetCommandQueueInfo_fn _cl_GetCommandQueueInfo;
clGetContextInfo_fn _cl_GetContextInfo;
clGetDeviceAndHostTimer_fn _cl_GetDeviceAndHostTimer;
clGetDeviceIDs_fn _cl_GetDeviceIDs;
clGetDeviceInfo_fn _cl_GetDeviceInfo;
clGetEventInfo_fn _cl_GetEventInfo;
clGetEventProfilingInfo_fn _cl_GetEventProfilingInfo;
//clGetExtensionFunctionAddress_fn _cl_GetExtensionFunctionAddress;  //deprecated
clGetExtensionFunctionAddressForPlatform_fn _cl_GetExtensionFunctionAddressForPlatform;
//clGetGLObjectInfo_fn _cl_GetGLObjectInfo;
//clGetGLTextureInfo_fn _cl_GetGLTextureInfo;
clGetHostTimer_fn _cl_GetHostTimer;
clGetImageInfo_fn _cl_GetImageInfo;
clGetKernelArgInfo_fn _cl_GetKernelArgInfo;
clGetKernelInfo_fn _cl_GetKernelInfo;
clGetKernelSubGroupInfo_fn _cl_GetKernelSubGroupInfo;
clGetKernelWorkGroupInfo_fn _cl_GetKernelWorkGroupInfo;
clGetMemObjectInfo_fn _cl_GetMemObjectInfo;
clGetPipeInfo_fn _cl_GetPipeInfo;
clGetPlatformIDs_fn _cl_GetPlatformIDs;
clGetPlatformInfo_fn _cl_GetPlatformInfo;
clGetProgramBuildInfo_fn _cl_GetProgramBuildInfo;
clGetProgramInfo_fn _cl_GetProgramInfo;
clGetSamplerInfo_fn _cl_GetSamplerInfo;
clGetSupportedImageFormats_fn _cl_GetSupportedImageFormats;
clLinkProgram_fn _cl_LinkProgram;
clReleaseCommandQueue_fn _cl_ReleaseCommandQueue;
clReleaseContext_fn _cl_ReleaseContext;
clReleaseDevice_fn _cl_ReleaseDevice;
clReleaseEvent_fn _cl_ReleaseEvent;
clReleaseKernel_fn _cl_ReleaseKernel;
clReleaseMemObject_fn _cl_ReleaseMemObject;
clReleaseProgram_fn _cl_ReleaseProgram;
clReleaseSampler_fn _cl_ReleaseSampler;
clRetainCommandQueue_fn _cl_RetainCommandQueue;
clRetainContext_fn _cl_RetainContext;
clRetainDevice_fn _cl_RetainDevice;
clRetainEvent_fn _cl_RetainEvent;
clRetainKernel_fn _cl_RetainKernel;
clRetainMemObject_fn _cl_RetainMemObject;
clRetainProgram_fn _cl_RetainProgram;
clRetainSampler_fn _cl_RetainSampler;
//clSetCommandQueueProperty_fn _cl_SetCommandQueueProperty;  //deprecated
clSetContextDestructorCallback_fn _cl_SetContextDestructorCallback;
clSetDefaultDeviceCommandQueue_fn _cl_SetDefaultDeviceCommandQueue;
clSetEventCallback_fn _cl_SetEventCallback;
clSetKernelArg_fn _cl_SetKernelArg;
clSetKernelArgSVMPointer_fn _cl_SetKernelArgSVMPointer;
clSetKernelExecInfo_fn _cl_SetKernelExecInfo;
clSetMemObjectDestructorCallback_fn _cl_SetMemObjectDestructorCallback;
//clSetProgramReleaseCallback_fn _cl_SetProgramReleaseCallback;  //deprecated
clSetProgramSpecializationConstant_fn _cl_SetProgramSpecializationConstant;
clSetUserEventStatus_fn _cl_SetUserEventStatus;
clSVMAlloc_fn _cl_SVMAlloc;
clSVMFree_fn _cl_SVMFree;
//clUnloadCompiler_fn _cl_UnloadCompiler;  //deprecated
clUnloadPlatformCompiler_fn _cl_UnloadPlatformCompiler;
clWaitForEvents_fn _cl_WaitForEvents;

static jboolean opencl_init(const char* openclFile)
{
  printf("opencl init...");

  opencl = loadLibrary(openclFile);
  if (opencl == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), openclFile);
    return JNI_FALSE;
  }

  //get functions
  getFunction(opencl, (void**)&_cl_BuildProgram, "clBuildProgram");
  getFunction(opencl, (void**)&_cl_CloneKernel, "clCloneKernel");
  getFunction(opencl, (void**)&_cl_CompileProgram, "clCompileProgram");
  getFunction(opencl, (void**)&_cl_CreateBuffer, "clCreateBuffer");
  getFunction(opencl, (void**)&_cl_CreateBufferWithProperties, "clCreateBufferWithProperties");
//  getFunction(opencl, (void**)&_cl_CreateCommandQueue, "clCreateCommandQueue");
  getFunction(opencl, (void**)&_cl_CreateCommandQueueWithProperties, "clCreateCommandQueueWithProperties");
  getFunction(opencl, (void**)&_cl_CreateContext, "clCreateContext");
  getFunction(opencl, (void**)&_cl_CreateContextFromType, "clCreateContextFromType");
//  getFunction(opencl, (void**)&_cl_CreateFromGLBuffer, "clCreateFromGLBuffer");
//  getFunction(opencl, (void**)&_cl_CreateFromGLRenderbuffer, "clCreateFromGLRenderbuffer");
//  getFunction(opencl, (void**)&_cl_CreateFromGLTexture, "clCreateFromGLTexture");
//  getFunction(opencl, (void**)&_cl_CreateFromGLTexture2D, "clCreateFromGLTexture2D");
//  getFunction(opencl, (void**)&_cl_CreateFromGLTexture3D, "clCreateFromGLTexture3D");
  getFunction(opencl, (void**)&_cl_CreateImage, "clCreateImage");
//  getFunction(opencl, (void**)&_cl_CreateImage2D, "clCreateImage2D");
//  getFunction(opencl, (void**)&_cl_CreateImage3D, "clCreateImage3D");
  getFunction(opencl, (void**)&_cl_CreateImageWithProperties, "clCreateImageWithProperties");
  getFunction(opencl, (void**)&_cl_CreateKernel, "clCreateKernel");
  getFunction(opencl, (void**)&_cl_CreateKernelsInProgram, "clCreateKernelsInProgram");
  getFunction(opencl, (void**)&_cl_CreatePipe, "clCreatePipe");
  getFunction(opencl, (void**)&_cl_CreateProgramWithBinary, "clCreateProgramWithBinary");
  getFunction(opencl, (void**)&_cl_CreateProgramWithBuiltInKernels, "clCreateProgramWithBuiltInKernels");
  getFunction(opencl, (void**)&_cl_CreateProgramWithIL, "clCreateProgramWithIL");
  getFunction(opencl, (void**)&_cl_CreateProgramWithSource, "clCreateProgramWithSource");
//  getFunction(opencl, (void**)&_cl_CreateSampler, "clCreateSampler");
  getFunction(opencl, (void**)&_cl_CreateSamplerWithProperties, "clCreateSamplerWithProperties");
  getFunction(opencl, (void**)&_cl_CreateSubBuffer, "clCreateSubBuffer");
  getFunction(opencl, (void**)&_cl_CreateSubDevices, "clCreateSubDevices");
  getFunction(opencl, (void**)&_cl_CreateUserEvent, "clCreateUserEvent");
//  getFunction(opencl, (void**)&_cl_EnqueueAcquireGLObjects, "clEnqueueAcquireGLObjects");
//  getFunction(opencl, (void**)&_cl_EnqueueBarrier, "clEnqueueBarrier");
  getFunction(opencl, (void**)&_cl_EnqueueBarrierWithWaitList, "clEnqueueBarrierWithWaitList");
  getFunction(opencl, (void**)&_cl_EnqueueCopyBuffer, "clEnqueueCopyBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueCopyBufferRect, "clEnqueueCopyBufferRect");
  getFunction(opencl, (void**)&_cl_EnqueueCopyBufferToImage, "clEnqueueCopyBufferToImage");
  getFunction(opencl, (void**)&_cl_EnqueueCopyImage, "clEnqueueCopyImage");
  getFunction(opencl, (void**)&_cl_EnqueueCopyImageToBuffer, "clEnqueueCopyImageToBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueFillBuffer, "clEnqueueFillBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueFillImage, "clEnqueueFillImage");
  getFunction(opencl, (void**)&_cl_EnqueueMapBuffer, "clEnqueueMapBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueMapImage, "clEnqueueMapImage");
//  getFunction(opencl, (void**)&_cl_EnqueueMarker, "clEnqueueMarker");
  getFunction(opencl, (void**)&_cl_EnqueueMarkerWithWaitList, "clEnqueueMarkerWithWaitList");
  getFunction(opencl, (void**)&_cl_EnqueueMigrateMemObjects, "clEnqueueMigrateMemObjects");
  getFunction(opencl, (void**)&_cl_EnqueueNativeKernel, "clEnqueueNativeKernel");
  getFunction(opencl, (void**)&_cl_EnqueueNDRangeKernel, "clEnqueueNDRangeKernel");
  getFunction(opencl, (void**)&_cl_EnqueueReadBuffer, "clEnqueueReadBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueReadBufferRect, "clEnqueueReadBufferRect");
  getFunction(opencl, (void**)&_cl_EnqueueReadImage, "clEnqueueReadImage");
//  getFunction(opencl, (void**)&_cl_EnqueueReleaseGLObjects, "clEnqueueReleaseGLObjects");
  getFunction(opencl, (void**)&_cl_EnqueueSVMFree, "clEnqueueSVMFree");
  getFunction(opencl, (void**)&_cl_EnqueueSVMMap, "clEnqueueSVMMap");
  getFunction(opencl, (void**)&_cl_EnqueueSVMMemcpy, "clEnqueueSVMMemcpy");
  getFunction(opencl, (void**)&_cl_EnqueueSVMMemFill, "clEnqueueSVMMemFill");
  getFunction(opencl, (void**)&_cl_EnqueueSVMMigrateMem, "clEnqueueSVMMigrateMem");
  getFunction(opencl, (void**)&_cl_EnqueueSVMUnmap, "clEnqueueSVMUnmap");
//  getFunction(opencl, (void**)&_cl_EnqueueTask, "clEnqueueTask");
  getFunction(opencl, (void**)&_cl_EnqueueUnmapMemObject, "clEnqueueUnmapMemObject");
//  getFunction(opencl, (void**)&_cl_EnqueueWaitForEvents, "clEnqueueWaitForEvents");
  getFunction(opencl, (void**)&_cl_EnqueueWriteBuffer, "clEnqueueWriteBuffer");
  getFunction(opencl, (void**)&_cl_EnqueueWriteBufferRect, "clEnqueueWriteBufferRect");
  getFunction(opencl, (void**)&_cl_EnqueueWriteImage, "clEnqueueWriteImage");
  getFunction(opencl, (void**)&_cl_Finish, "clFinish");
  getFunction(opencl, (void**)&_cl_Flush, "clFlush");
  getFunction(opencl, (void**)&_cl_GetCommandQueueInfo, "clGetCommandQueueInfo");
  getFunction(opencl, (void**)&_cl_GetContextInfo, "clGetContextInfo");
  getFunction(opencl, (void**)&_cl_GetDeviceAndHostTimer, "clGetDeviceAndHostTimer");
  getFunction(opencl, (void**)&_cl_GetDeviceIDs, "clGetDeviceIDs");
  getFunction(opencl, (void**)&_cl_GetDeviceInfo, "clGetDeviceInfo");
  getFunction(opencl, (void**)&_cl_GetEventInfo, "clGetEventInfo");
  getFunction(opencl, (void**)&_cl_GetEventProfilingInfo, "clGetEventProfilingInfo");
//  getFunction(opencl, (void**)&_cl_GetExtensionFunctionAddress, "clGetExtensionFunctionAddress");
  getFunction(opencl, (void**)&_cl_GetExtensionFunctionAddressForPlatform, "clGetExtensionFunctionAddressForPlatform");
//  getFunction(opencl, (void**)&_cl_GetGLObjectInfo, "clGetGLObjectInfo");
//  getFunction(opencl, (void**)&_cl_GetGLTextureInfo, "clGetGLTextureInfo");
  getFunction(opencl, (void**)&_cl_GetHostTimer, "clGetHostTimer");
  getFunction(opencl, (void**)&_cl_GetImageInfo, "clGetImageInfo");
  getFunction(opencl, (void**)&_cl_GetKernelArgInfo, "clGetKernelArgInfo");
  getFunction(opencl, (void**)&_cl_GetKernelInfo, "clGetKernelInfo");
  getFunction(opencl, (void**)&_cl_GetKernelSubGroupInfo, "clGetKernelSubGroupInfo");
  getFunction(opencl, (void**)&_cl_GetKernelWorkGroupInfo, "clGetKernelWorkGroupInfo");
  getFunction(opencl, (void**)&_cl_GetMemObjectInfo, "clGetMemObjectInfo");
  getFunction(opencl, (void**)&_cl_GetPipeInfo, "clGetPipeInfo");
  getFunction(opencl, (void**)&_cl_GetPlatformIDs, "clGetPlatformIDs");
  getFunction(opencl, (void**)&_cl_GetPlatformInfo, "clGetPlatformInfo");
  getFunction(opencl, (void**)&_cl_GetProgramBuildInfo, "clGetProgramBuildInfo");
  getFunction(opencl, (void**)&_cl_GetProgramInfo, "clGetProgramInfo");
  getFunction(opencl, (void**)&_cl_GetSamplerInfo, "clGetSamplerInfo");
  getFunction(opencl, (void**)&_cl_GetSupportedImageFormats, "clGetSupportedImageFormats");
  getFunction(opencl, (void**)&_cl_LinkProgram, "clLinkProgram");
  getFunction(opencl, (void**)&_cl_ReleaseCommandQueue, "clReleaseCommandQueue");
  getFunction(opencl, (void**)&_cl_ReleaseContext, "clReleaseContext");
  getFunction(opencl, (void**)&_cl_ReleaseDevice, "clReleaseDevice");
  getFunction(opencl, (void**)&_cl_ReleaseEvent, "clReleaseEvent");
  getFunction(opencl, (void**)&_cl_ReleaseKernel, "clReleaseKernel");
  getFunction(opencl, (void**)&_cl_ReleaseMemObject, "clReleaseMemObject");
  getFunction(opencl, (void**)&_cl_ReleaseProgram, "clReleaseProgram");
  getFunction(opencl, (void**)&_cl_ReleaseSampler, "clReleaseSampler");
  getFunction(opencl, (void**)&_cl_RetainCommandQueue, "clRetainCommandQueue");
  getFunction(opencl, (void**)&_cl_RetainContext, "clRetainContext");
  getFunction(opencl, (void**)&_cl_RetainDevice, "clRetainDevice");
  getFunction(opencl, (void**)&_cl_RetainEvent, "clRetainEvent");
  getFunction(opencl, (void**)&_cl_RetainKernel, "clRetainKernel");
  getFunction(opencl, (void**)&_cl_RetainMemObject, "clRetainMemObject");
  getFunction(opencl, (void**)&_cl_RetainProgram, "clRetainProgram");
  getFunction(opencl, (void**)&_cl_RetainSampler, "clRetainSampler");
//  getFunction(opencl, (void**)&_cl_SetCommandQueueProperty, "clSetCommandQueueProperty");
  getFunction(opencl, (void**)&_cl_SetContextDestructorCallback, "clSetContextDestructorCallback");
  getFunction(opencl, (void**)&_cl_SetDefaultDeviceCommandQueue, "clSetDefaultDeviceCommandQueue");
  getFunction(opencl, (void**)&_cl_SetEventCallback, "clSetEventCallback");
  getFunction(opencl, (void**)&_cl_SetKernelArg, "clSetKernelArg");
  getFunction(opencl, (void**)&_cl_SetKernelArgSVMPointer, "clSetKernelArgSVMPointer");
  getFunction(opencl, (void**)&_cl_SetKernelExecInfo, "clSetKernelExecInfo");
  getFunction(opencl, (void**)&_cl_SetMemObjectDestructorCallback, "clSetMemObjectDestructorCallback");
//  getFunction(opencl, (void**)&_cl_SetProgramReleaseCallback, "clSetProgramReleaseCallback");
  getFunction(opencl, (void**)&_cl_SetProgramSpecializationConstant, "clSetProgramSpecializationConstant");
  getFunction(opencl, (void**)&_cl_SetUserEventStatus, "clSetUserEventStatus");
  getFunction(opencl, (void**)&_cl_SVMAlloc, "clSVMAlloc");
  getFunction(opencl, (void**)&_cl_SVMFree, "clSVMFree");
//  getFunction(opencl, (void**)&_cl_UnloadCompiler, "clUnloadCompiler");
  getFunction(opencl, (void**)&_cl_UnloadPlatformCompiler, "clUnloadPlatformCompiler");
  getFunction(opencl, (void**)&_cl_WaitForEvents, "clWaitForEvents");

  printf("ok\n");

  return JNI_TRUE;
}

struct CLContext {
  cl_platform_id platform_id;         // compute platform id
  cl_device_id device_id;             // compute device id
  cl_context context;                 // compute context
  cl_command_queue commands;          // compute command queue
  cl_program program;                 // compute program
};

#define DATA_SIZE (64 * 1024)

// Simple compute kernel which computes the square of an input array
//
static const char* KernelSource = "\n" \
"__kernel void square(__global float* input, __global float* output)\n" \
"{\n" \
"   int i = get_global_id(0);\n" \
"   output[i] = input[i] * input[i];\n" \
"}\n" \
"\n";

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clLoadLibrary
  (JNIEnv* e, jobject o, jstring jopencl)
{
  if (opencl_loaded) return opencl_loaded;

  const char* openclFile = e->GetStringUTFChars(jopencl, NULL);

  jboolean ret = opencl_init(openclFile);

  e->ReleaseStringUTFChars(jopencl, openclFile);

  if (!ret) return JNI_FALSE;

  opencl_loaded = JNI_TRUE;

  if (!opencl_test) return JNI_TRUE;

  //do a simple test

  int res;
  int err;

  float data[DATA_SIZE];              // original data set given to device
  float results[DATA_SIZE];           // results returned from device
  unsigned int correct;               // number of correct results returned

  size_t global;                      // global domain size for our calculation
  size_t local;                       // local domain size for our calculation

  cl_platform_id platform_id;         // compute platform id
  cl_device_id device_id;             // compute device id
  cl_context context;                 // compute context
  cl_command_queue commands;          // compute command queue
  cl_program program;                 // compute program
  cl_kernel kernel;                   // compute kernel

  cl_mem input;                       // device memory used for the input array
  cl_mem output;                      // device memory used for the output array

  // Fill our data set with random float values
  int i = 0;
  unsigned int count = DATA_SIZE;
  for(i = 0; i < count; i++) {
    data[i] = rand() / (float)RAND_MAX;
  }

  //get platform_id
  res = (*_cl_GetPlatformIDs)(1, &platform_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get platform id!%d\n", res);
    return JNI_FALSE;
  }

  //get device_id
  res = (*_cl_GetDeviceIDs)(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get device id!:%d\n", res);
    return JNI_FALSE;
  }

  // Create a compute context
  context = (*_cl_CreateContext)(0, 1, &device_id, NULL, NULL, &err);
  if (!context)
  {
    printf("Error: Failed to create a compute context!\n");
    return JNI_FALSE;
  }

  // Create a command commands
  commands = (*_cl_CreateCommandQueueWithProperties)(context, device_id, NULL, &err);
  if (!commands)
  {
    printf("Error: Failed to create a command commands!\n");
    return EXIT_FAILURE;
  }

  // Create the compute program from the source buffer
  program = (*_cl_CreateProgramWithSource)(context, 1, (const char**) & KernelSource, NULL, &err);
  if (!program)
  {
    printf("Error: Failed to create compute program!\n");
    return EXIT_FAILURE;
  }

  // Build the program executable
  err = (*_cl_BuildProgram)(program, 0, NULL, NULL, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    size_t len;
    char buffer[2048];

    printf("Error: Failed to build program executable!\n");
    (*_cl_GetProgramBuildInfo)(program, device_id, CL_PROGRAM_BUILD_LOG, sizeof(buffer), buffer, &len);
    printf("%s\n", buffer);
    return JNI_FALSE;
  }

  // Create the compute kernel in the program we wish to run
  kernel = (*_cl_CreateKernel)(program, "square", &err);
  if (!kernel || err != CL_SUCCESS)
  {
    printf("Error: Failed to create compute kernel!\n");
    return JNI_FALSE;
  }

  // Create the input and output arrays in device memory for our calculation
  //
  input = (*_cl_CreateBuffer)(context,  CL_MEM_READ_ONLY,  sizeof(float) * count, NULL, NULL);
  output = (*_cl_CreateBuffer)(context, CL_MEM_WRITE_ONLY, sizeof(float) * count, NULL, NULL);
  if (!input || !output)
  {
    printf("Error: Failed to allocate device memory!\n");
    return JNI_FALSE;
  }

  // Write our data set into the input array in device memory
  err = (*_cl_EnqueueWriteBuffer)(commands, input, CL_TRUE, 0, sizeof(float) * count, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write to source array!\n");
    return JNI_FALSE;
  }

  // Set the arguments to our compute kernel
  err  = (*_cl_SetKernelArg)(kernel, 0, sizeof(cl_mem), &input);
  err |= (*_cl_SetKernelArg)(kernel, 1, sizeof(cl_mem), &output);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to set kernel arguments! %d\n", err);
    return JNI_FALSE;
  }

  // Get the maximum work group size for executing the kernel on the device
  //
  err = (*_cl_GetKernelWorkGroupInfo)(kernel, device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  // Execute the kernel over the entire range of our 1d input data set
  // using the maximum number of work group items for this device
  //
  global = count;
  err = (*_cl_EnqueueNDRangeKernel)(commands, kernel, 1, NULL, &global, &local, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute kernel!\n");
    return JNI_FALSE;
  }

  // Wait for the command commands to get serviced before reading back results
  //
  (*_cl_Finish)(commands);

  // Read back the results from the device to verify the output
  err = (*_cl_EnqueueReadBuffer)(commands, output, CL_TRUE, 0, sizeof(float) * count, results, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to read output array! %d\n", err);
    return JNI_FALSE;
  }

  // Validate our results
  //
  correct = 0;
  for(i = 0; i < count; i++)
  {
    if (results[i] == data[i] * data[i]) {
      correct++;
    }
  }

  // Print a brief summary detailing the results
  //
  printf("OpenCL Test : Computed '%d/%d' correct values!\n", correct, count);

  // Shutdown and cleanup
  (*_cl_ReleaseMemObject)(input);
  (*_cl_ReleaseMemObject)(output);
  (*_cl_ReleaseProgram)(program);
  (*_cl_ReleaseKernel)(kernel);
  (*_cl_ReleaseCommandQueue)(commands);
  (*_cl_ReleaseContext)(context);

  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_CLJNI_clCreate
  (JNIEnv *e, jobject o, jstring src, jint type)
{
  int res, err;
  CLContext *ctx = (CLContext*)malloc(sizeof(CLContext));
  memset(ctx, 0, sizeof(CLContext));

  if (opencl_debug) {
    printf("ctx=%p\n", ctx);
  }

  //get platform_id
  res = (*_cl_GetPlatformIDs)(1, &ctx->platform_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get platform id!\n");
    free(ctx);
    return 0;
  }

  if (opencl_debug) {
    printf("platform_id=%p\n", ctx->platform_id);
  }

  //get device_id
  res = (*_cl_GetDeviceIDs)(ctx->platform_id, type, 1, &ctx->device_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get device id!\n");
    free(ctx);
    return 0;
  }

  if (opencl_debug) {
    printf("device_id=%p\n", ctx->device_id);
  }

  // Create a compute context
  ctx->context = (*_cl_CreateContext)(0, 1, &ctx->device_id, NULL, NULL, &err);
  if (!ctx->context)
  {
    printf("Error: Failed to create a compute context!\n");
    free(ctx);
    return 0;
  }

  if (opencl_debug) {
    printf("context=%p\n", ctx->context);
  }

  // Create a command commands
  ctx->commands = (*_cl_CreateCommandQueueWithProperties)(ctx->context, ctx->device_id, NULL, &err);
  if (!ctx->commands)
  {
    printf("Error: Failed to create a command commands!\n");
    free(ctx);
    return 0;
  }

  if (opencl_debug) {
    printf("commands=%p\n", ctx->commands);
  }

  const char *c_src = e->GetStringUTFChars(src, NULL);

  // Create the compute program from the source buffer
  ctx->program = (*_cl_CreateProgramWithSource)(ctx->context, 1, (const char**)&c_src, NULL, &err);

  e->ReleaseStringUTFChars(src, c_src);

  if (!ctx->program)
  {
    printf("Error: Failed to create compute program!\n");
    free(ctx);
    return 0;
  }

  // Build the program executable
  err = (*_cl_BuildProgram)(ctx->program, 0, NULL, NULL, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    size_t len;
    char buffer[1024 * 16];

    printf("Error: Failed to build program executable!\n");
    err = (*_cl_GetProgramBuildInfo)(ctx->program, ctx->device_id, CL_PROGRAM_BUILD_LOG, sizeof(buffer), buffer, &len);
    if (err == CL_SUCCESS) {
      printf("%s\n", buffer);
    } else {
      printf("clGetProgramBuildInfo failed:%d\n", err);
    }
    free(ctx);
    return 0;
  }

  return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_CLJNI_clKernel
  (JNIEnv *e, jobject o, jlong ctx_ptr, jstring kernel)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;
  int err;

  const char *c_kernel = e->GetStringUTFChars(kernel, NULL);

  // Create the compute kernel in the program we wish to run
  jlong kernel_ptr = (jlong)(*_cl_CreateKernel)(ctx->program, c_kernel, &err);

  e->ReleaseStringUTFChars(kernel, c_kernel);

  if (!kernel_ptr || err != CL_SUCCESS)
  {
    printf("Error: Failed to create compute kernel!\n");
    return 0;
  }

  return kernel_ptr;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_CLJNI_clCreateBuffer
  (JNIEnv *e, jobject o, jlong ctx_ptr, jint size, jint type)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;

  cl_mem buffer = (*_cl_CreateBuffer)(ctx->context, type, size, NULL, NULL);

  return (jlong)buffer;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clSetArg
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel, jint idx, jbyteArray data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);

  int err = (*_cl_SetKernelArg)((cl_kernel)kernel, idx, size, dataptr);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to set kernel arguments! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clWriteBufferi8
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong buffer, jbyteArray data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);

  int err = (*_cl_EnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clWriteBufferf32
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong buffer, jfloatArray data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);

  int err = (*_cl_EnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clExecute
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel, jint count)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  size_t global = count;
  size_t local;

  int err = (*_cl_GetKernelWorkGroupInfo)((cl_kernel)kernel, ctx->device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  if (opencl_debug) {
    printf("max_local = %zu\n", local);
  }

	if (local > count) {
    local = count;
  }

  err = (*_cl_EnqueueNDRangeKernel)(ctx->commands, (cl_kernel)kernel, 1, NULL, &global, &local, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute kernel!%d\n", err);
    return JNI_FALSE;
  }

  (*_cl_Finish)(ctx->commands);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clExecute2
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel, jint count1, jint count2)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  size_t globals[2];
  globals[0] = count1;
  globals[1] = count2;
  size_t local;
  size_t locals[2];

  //see CL_DEVICE_MAX_WORK_ITEM_SIZES

  int err = (*_cl_GetKernelWorkGroupInfo)((cl_kernel)kernel, ctx->device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  if (opencl_debug) {
    printf("max_local = %zu\n", local);
  }

  locals[0] = count1 > local ? local : count1;
  locals[1] = count2 > local ? local : count2;

  err = (*_cl_EnqueueNDRangeKernel)(ctx->commands, (cl_kernel)kernel, 2, NULL, globals, locals, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute2 kernel!%d\n", err);
    return JNI_FALSE;
  }

  (*_cl_Finish)(ctx->commands);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clExecute3
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel, jint count1, jint count2, jint count3)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  size_t globals[3];
  globals[0] = count1;
  globals[1] = count2;
  globals[2] = count3;
  size_t local;
  size_t locals[3];

  //see CL_DEVICE_MAX_WORK_ITEM_SIZES

  int err = (*_cl_GetKernelWorkGroupInfo)((cl_kernel)kernel, ctx->device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  if (opencl_debug) {
    printf("max_local = %zu\n", local);
  }

  locals[0] = count1 > local ? local : count1;
  locals[1] = count2 > local ? local : count2;
  locals[2] = count3 > local ? local : count3;

  err = (*_cl_EnqueueNDRangeKernel)(ctx->commands, (cl_kernel)kernel, 3, NULL, globals, locals, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute3 kernel!%d\n", err);
    return JNI_FALSE;
  }

  (*_cl_Finish)(ctx->commands);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clExecute4
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel, jint count1, jint count2, jint count3, jint count4)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  size_t globals[4];
  globals[0] = count1;
  globals[1] = count2;
  globals[2] = count3;
  globals[3] = count4;
  size_t local;
  size_t locals[4];

  //see CL_DEVICE_MAX_WORK_ITEM_SIZES

  int err = (*_cl_GetKernelWorkGroupInfo)((cl_kernel)kernel, ctx->device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  if (opencl_debug) {
    printf("max_local = %zu\n", local);
  }

  locals[0] = count1 > local ? local : count1;
  locals[1] = count2 > local ? local : count2;
  locals[2] = count3 > local ? local : count3;
  locals[3] = count4 > local ? local : count4;

  err = (*_cl_EnqueueNDRangeKernel)(ctx->commands, (cl_kernel)kernel, 4, NULL, globals, locals, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute4 kernel!%d\n", err);
    return JNI_FALSE;
  }

  (*_cl_Finish)(ctx->commands);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clReadBufferi8
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong buffer, jbyteArray data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);

  int err = (*_cl_EnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clReadBufferf32
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong buffer, jfloatArray data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);

  int err = (*_cl_EnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clFreeKernel
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong kernel)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseKernel)((cl_kernel)kernel);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clFreeBuffer
  (JNIEnv *e, jobject o, jlong ctx_ptr, jlong buffer)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseMemObject)((cl_mem)buffer);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CLJNI_clClose
  (JNIEnv *e, jobject o, jlong ctx_ptr)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseProgram)(ctx->program);
  (*_cl_ReleaseCommandQueue)(ctx->commands);
  (*_cl_ReleaseContext)(ctx->context);
  free(ctx);

  return JNI_TRUE;
}

static JNINativeMethod javaforce_cl_CL[] = {
  {"clLoadLibrary", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_CLJNI_clLoadLibrary},
  {"clCreate", "(Ljava/lang/String;I)J", (void *)&Java_javaforce_jni_CLJNI_clCreate},
  {"clKernel", "(JLjava/lang/String;)J", (void *)&Java_javaforce_jni_CLJNI_clKernel},
  {"clCreateBuffer", "(JII)J", (void *)&Java_javaforce_jni_CLJNI_clCreateBuffer},
  {"clSetArg", "(JJI[BI)Z", (void *)&Java_javaforce_jni_CLJNI_clSetArg},
  {"clWriteBufferi8", "(JJ[BI)Z", (void *)&Java_javaforce_jni_CLJNI_clWriteBufferi8},
  {"clWriteBufferf32", "(JJ[FI)Z", (void *)&Java_javaforce_jni_CLJNI_clWriteBufferf32},
  {"clExecute", "(JJI)Z", (void *)&Java_javaforce_jni_CLJNI_clExecute},
  {"clExecute2", "(JJII)Z", (void *)&Java_javaforce_jni_CLJNI_clExecute2},
  {"clExecute3", "(JJIII)Z", (void *)&Java_javaforce_jni_CLJNI_clExecute3},
  {"clExecute4", "(JJIIII)Z", (void *)&Java_javaforce_jni_CLJNI_clExecute4},
  {"clReadBufferi8", "(JJ[BI)Z", (void *)&Java_javaforce_jni_CLJNI_clReadBufferi8},
  {"clReadBufferf32", "(JJ[FI)Z", (void *)&Java_javaforce_jni_CLJNI_clReadBufferf32},
  {"clFreeKernel", "(JJ)Z", (void *)&Java_javaforce_jni_CLJNI_clFreeKernel},
  {"clFreeBuffer", "(JJ)Z", (void *)&Java_javaforce_jni_CLJNI_clFreeBuffer},
  {"clClose", "(J)Z", (void *)&Java_javaforce_jni_CLJNI_clClose},
};

extern "C" void cl_register(JNIEnv *env);

void cl_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/CLJNI");
  registerNatives(env, cls, javaforce_cl_CL, sizeof(javaforce_cl_CL)/sizeof(JNINativeMethod));
}
