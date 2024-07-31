//OpenCL

#define CL_TARGET_OPENCL_VERSION 300

#include "../opencl/CL/cl.h"
#include "../opencl/CL/cl_function_types.h"

JF_LIB_HANDLE opencl = NULL;

static jboolean opencl_loaded = JNI_FALSE;

//functions
clBuildProgram_fn _clBuildProgram;
clCloneKernel_fn _clCloneKernel;
clCompileProgram_fn _clCompileProgram;
clCreateBuffer_fn _clCreateBuffer;
clCreateBufferWithProperties_fn _clCreateBufferWithProperties;
//clCreateCommandQueue_fn _clCreateCommandQueue;  //deprecated
clCreateCommandQueueWithProperties_fn _clCreateCommandQueueWithProperties;
clCreateContext_fn _clCreateContext;
clCreateContextFromType_fn _clCreateContextFromType;
//clCreateFromGLBuffer_fn _clCreateFromGLBuffer;
//clCreateFromGLRenderbuffer_fn _clCreateFromGLRenderbuffer;
//clCreateFromGLTexture_fn _clCreateFromGLTexture;
//clCreateFromGLTexture2D_fn _clCreateFromGLTexture2D;
//clCreateFromGLTexture3D_fn _clCreateFromGLTexture3D;
clCreateImage_fn _clCreateImage;
//clCreateImage2D_fn _clCreateImage2D;  //deprecated
//clCreateImage3D_fn _clCreateImage3D;  //deprecated
clCreateImageWithProperties_fn _clCreateImageWithProperties;
clCreateKernel_fn _clCreateKernel;
clCreateKernelsInProgram_fn _clCreateKernelsInProgram;
clCreatePipe_fn _clCreatePipe;
clCreateProgramWithBinary_fn _clCreateProgramWithBinary;
clCreateProgramWithBuiltInKernels_fn _clCreateProgramWithBuiltInKernels;
clCreateProgramWithIL_fn _clCreateProgramWithIL;
clCreateProgramWithSource_fn _clCreateProgramWithSource;
//clCreateSampler_fn _clCreateSampler;  //deprecated
clCreateSamplerWithProperties_fn _clCreateSamplerWithProperties;
clCreateSubBuffer_fn _clCreateSubBuffer;
clCreateSubDevices_fn _clCreateSubDevices;
clCreateUserEvent_fn _clCreateUserEvent;
//clEnqueueAcquireGLObjects_fn _clEnqueueAcquireGLObjects;
//clEnqueueBarrier_fn _clEnqueueBarrier;  //deprecated
clEnqueueBarrierWithWaitList_fn _clEnqueueBarrierWithWaitList;
clEnqueueCopyBuffer_fn _clEnqueueCopyBuffer;
clEnqueueCopyBufferRect_fn _clEnqueueCopyBufferRect;
clEnqueueCopyBufferToImage_fn _clEnqueueCopyBufferToImage;
clEnqueueCopyImage_fn _clEnqueueCopyImage;
clEnqueueCopyImageToBuffer_fn _clEnqueueCopyImageToBuffer;
clEnqueueFillBuffer_fn _clEnqueueFillBuffer;
clEnqueueFillImage_fn _clEnqueueFillImage;
clEnqueueMapBuffer_fn _clEnqueueMapBuffer;
clEnqueueMapImage_fn _clEnqueueMapImage;
//clEnqueueMarker_fn _clEnqueueMarker;  //deprecated
clEnqueueMarkerWithWaitList_fn _clEnqueueMarkerWithWaitList;
clEnqueueMigrateMemObjects_fn _clEnqueueMigrateMemObjects;
clEnqueueNativeKernel_fn _clEnqueueNativeKernel;
clEnqueueNDRangeKernel_fn _clEnqueueNDRangeKernel;
clEnqueueReadBuffer_fn _clEnqueueReadBuffer;
clEnqueueReadBufferRect_fn _clEnqueueReadBufferRect;
clEnqueueReadImage_fn _clEnqueueReadImage;
//clEnqueueReleaseGLObjects_fn _clEnqueueReleaseGLObjects;
clEnqueueSVMFree_fn _clEnqueueSVMFree;
clEnqueueSVMMap_fn _clEnqueueSVMMap;
clEnqueueSVMMemcpy_fn _clEnqueueSVMMemcpy;
clEnqueueSVMMemFill_fn _clEnqueueSVMMemFill;
clEnqueueSVMMigrateMem_fn _clEnqueueSVMMigrateMem;
clEnqueueSVMUnmap_fn _clEnqueueSVMUnmap;
//clEnqueueTask_fn _clEnqueueTask;  //deprecated
clEnqueueUnmapMemObject_fn _clEnqueueUnmapMemObject;
//clEnqueueWaitForEvents_fn _clEnqueueWaitForEvents;  //deprecated
clEnqueueWriteBuffer_fn _clEnqueueWriteBuffer;
clEnqueueWriteBufferRect_fn _clEnqueueWriteBufferRect;
clEnqueueWriteImage_fn _clEnqueueWriteImage;
clFinish_fn _clFinish;
clFlush_fn _clFlush;
clGetCommandQueueInfo_fn _clGetCommandQueueInfo;
clGetContextInfo_fn _clGetContextInfo;
clGetDeviceAndHostTimer_fn _clGetDeviceAndHostTimer;
clGetDeviceIDs_fn _clGetDeviceIDs;
clGetDeviceInfo_fn _clGetDeviceInfo;
clGetEventInfo_fn _clGetEventInfo;
clGetEventProfilingInfo_fn _clGetEventProfilingInfo;
//clGetExtensionFunctionAddress_fn _clGetExtensionFunctionAddress;  //deprecated
clGetExtensionFunctionAddressForPlatform_fn _clGetExtensionFunctionAddressForPlatform;
//clGetGLObjectInfo_fn _clGetGLObjectInfo;
//clGetGLTextureInfo_fn _clGetGLTextureInfo;
clGetHostTimer_fn _clGetHostTimer;
clGetImageInfo_fn _clGetImageInfo;
clGetKernelArgInfo_fn _clGetKernelArgInfo;
clGetKernelInfo_fn _clGetKernelInfo;
clGetKernelSubGroupInfo_fn _clGetKernelSubGroupInfo;
clGetKernelWorkGroupInfo_fn _clGetKernelWorkGroupInfo;
clGetMemObjectInfo_fn _clGetMemObjectInfo;
clGetPipeInfo_fn _clGetPipeInfo;
clGetPlatformIDs_fn _clGetPlatformIDs;
clGetPlatformInfo_fn _clGetPlatformInfo;
clGetProgramBuildInfo_fn _clGetProgramBuildInfo;
clGetProgramInfo_fn _clGetProgramInfo;
clGetSamplerInfo_fn _clGetSamplerInfo;
clGetSupportedImageFormats_fn _clGetSupportedImageFormats;
clLinkProgram_fn _clLinkProgram;
clReleaseCommandQueue_fn _clReleaseCommandQueue;
clReleaseContext_fn _clReleaseContext;
clReleaseDevice_fn _clReleaseDevice;
clReleaseEvent_fn _clReleaseEvent;
clReleaseKernel_fn _clReleaseKernel;
clReleaseMemObject_fn _clReleaseMemObject;
clReleaseProgram_fn _clReleaseProgram;
clReleaseSampler_fn _clReleaseSampler;
clRetainCommandQueue_fn _clRetainCommandQueue;
clRetainContext_fn _clRetainContext;
clRetainDevice_fn _clRetainDevice;
clRetainEvent_fn _clRetainEvent;
clRetainKernel_fn _clRetainKernel;
clRetainMemObject_fn _clRetainMemObject;
clRetainProgram_fn _clRetainProgram;
clRetainSampler_fn _clRetainSampler;
//clSetCommandQueueProperty_fn _clSetCommandQueueProperty;  //deprecated
clSetContextDestructorCallback_fn _clSetContextDestructorCallback;
clSetDefaultDeviceCommandQueue_fn _clSetDefaultDeviceCommandQueue;
clSetEventCallback_fn _clSetEventCallback;
clSetKernelArg_fn _clSetKernelArg;
clSetKernelArgSVMPointer_fn _clSetKernelArgSVMPointer;
clSetKernelExecInfo_fn _clSetKernelExecInfo;
clSetMemObjectDestructorCallback_fn _clSetMemObjectDestructorCallback;
//clSetProgramReleaseCallback_fn _clSetProgramReleaseCallback;  //deprecated
clSetProgramSpecializationConstant_fn _clSetProgramSpecializationConstant;
clSetUserEventStatus_fn _clSetUserEventStatus;
clSVMAlloc_fn _clSVMAlloc;
clSVMFree_fn _clSVMFree;
//clUnloadCompiler_fn _clUnloadCompiler;  //deprecated
clUnloadPlatformCompiler_fn _clUnloadPlatformCompiler;
clWaitForEvents_fn _clWaitForEvents;

static jboolean opencl_init(const char* openclFile)
{
  printf("opencl init...");

  opencl = loadLibrary(openclFile);
  if (opencl == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), openclFile);
    return JNI_FALSE;
  }

  //get functions
  getFunction(opencl, (void**)&_clBuildProgram, "clBuildProgram");
  getFunction(opencl, (void**)&_clCloneKernel, "clCloneKernel");
  getFunction(opencl, (void**)&_clCompileProgram, "clCompileProgram");
  getFunction(opencl, (void**)&_clCreateBuffer, "clCreateBuffer");
  getFunction(opencl, (void**)&_clCreateBufferWithProperties, "clCreateBufferWithProperties");
//  getFunction(opencl, (void**)&_clCreateCommandQueue, "clCreateCommandQueue");
  getFunction(opencl, (void**)&_clCreateCommandQueueWithProperties, "clCreateCommandQueueWithProperties");
  getFunction(opencl, (void**)&_clCreateContext, "clCreateContext");
  getFunction(opencl, (void**)&_clCreateContextFromType, "clCreateContextFromType");
//  getFunction(opencl, (void**)&_clCreateFromGLBuffer, "clCreateFromGLBuffer");
//  getFunction(opencl, (void**)&_clCreateFromGLRenderbuffer, "clCreateFromGLRenderbuffer");
//  getFunction(opencl, (void**)&_clCreateFromGLTexture, "clCreateFromGLTexture");
//  getFunction(opencl, (void**)&_clCreateFromGLTexture2D, "clCreateFromGLTexture2D");
//  getFunction(opencl, (void**)&_clCreateFromGLTexture3D, "clCreateFromGLTexture3D");
  getFunction(opencl, (void**)&_clCreateImage, "clCreateImage");
//  getFunction(opencl, (void**)&_clCreateImage2D, "clCreateImage2D");
//  getFunction(opencl, (void**)&_clCreateImage3D, "clCreateImage3D");
  getFunction(opencl, (void**)&_clCreateImageWithProperties, "clCreateImageWithProperties");
  getFunction(opencl, (void**)&_clCreateKernel, "clCreateKernel");
  getFunction(opencl, (void**)&_clCreateKernelsInProgram, "clCreateKernelsInProgram");
  getFunction(opencl, (void**)&_clCreatePipe, "clCreatePipe");
  getFunction(opencl, (void**)&_clCreateProgramWithBinary, "clCreateProgramWithBinary");
  getFunction(opencl, (void**)&_clCreateProgramWithBuiltInKernels, "clCreateProgramWithBuiltInKernels");
  getFunction(opencl, (void**)&_clCreateProgramWithIL, "clCreateProgramWithIL");
  getFunction(opencl, (void**)&_clCreateProgramWithSource, "clCreateProgramWithSource");
//  getFunction(opencl, (void**)&_clCreateSampler, "clCreateSampler");
  getFunction(opencl, (void**)&_clCreateSamplerWithProperties, "clCreateSamplerWithProperties");
  getFunction(opencl, (void**)&_clCreateSubBuffer, "clCreateSubBuffer");
  getFunction(opencl, (void**)&_clCreateSubDevices, "clCreateSubDevices");
  getFunction(opencl, (void**)&_clCreateUserEvent, "clCreateUserEvent");
//  getFunction(opencl, (void**)&_clEnqueueAcquireGLObjects, "clEnqueueAcquireGLObjects");
//  getFunction(opencl, (void**)&_clEnqueueBarrier, "clEnqueueBarrier");
  getFunction(opencl, (void**)&_clEnqueueBarrierWithWaitList, "clEnqueueBarrierWithWaitList");
  getFunction(opencl, (void**)&_clEnqueueCopyBuffer, "clEnqueueCopyBuffer");
  getFunction(opencl, (void**)&_clEnqueueCopyBufferRect, "clEnqueueCopyBufferRect");
  getFunction(opencl, (void**)&_clEnqueueCopyBufferToImage, "clEnqueueCopyBufferToImage");
  getFunction(opencl, (void**)&_clEnqueueCopyImage, "clEnqueueCopyImage");
  getFunction(opencl, (void**)&_clEnqueueCopyImageToBuffer, "clEnqueueCopyImageToBuffer");
  getFunction(opencl, (void**)&_clEnqueueFillBuffer, "clEnqueueFillBuffer");
  getFunction(opencl, (void**)&_clEnqueueFillImage, "clEnqueueFillImage");
  getFunction(opencl, (void**)&_clEnqueueMapBuffer, "clEnqueueMapBuffer");
  getFunction(opencl, (void**)&_clEnqueueMapImage, "clEnqueueMapImage");
//  getFunction(opencl, (void**)&_clEnqueueMarker, "clEnqueueMarker");
  getFunction(opencl, (void**)&_clEnqueueMarkerWithWaitList, "clEnqueueMarkerWithWaitList");
  getFunction(opencl, (void**)&_clEnqueueMigrateMemObjects, "clEnqueueMigrateMemObjects");
  getFunction(opencl, (void**)&_clEnqueueNativeKernel, "clEnqueueNativeKernel");
  getFunction(opencl, (void**)&_clEnqueueNDRangeKernel, "clEnqueueNDRangeKernel");
  getFunction(opencl, (void**)&_clEnqueueReadBuffer, "clEnqueueReadBuffer");
  getFunction(opencl, (void**)&_clEnqueueReadBufferRect, "clEnqueueReadBufferRect");
  getFunction(opencl, (void**)&_clEnqueueReadImage, "clEnqueueReadImage");
//  getFunction(opencl, (void**)&_clEnqueueReleaseGLObjects, "clEnqueueReleaseGLObjects");
  getFunction(opencl, (void**)&_clEnqueueSVMFree, "clEnqueueSVMFree");
  getFunction(opencl, (void**)&_clEnqueueSVMMap, "clEnqueueSVMMap");
  getFunction(opencl, (void**)&_clEnqueueSVMMemcpy, "clEnqueueSVMMemcpy");
  getFunction(opencl, (void**)&_clEnqueueSVMMemFill, "clEnqueueSVMMemFill");
  getFunction(opencl, (void**)&_clEnqueueSVMMigrateMem, "clEnqueueSVMMigrateMem");
  getFunction(opencl, (void**)&_clEnqueueSVMUnmap, "clEnqueueSVMUnmap");
//  getFunction(opencl, (void**)&_clEnqueueTask, "clEnqueueTask");
  getFunction(opencl, (void**)&_clEnqueueUnmapMemObject, "clEnqueueUnmapMemObject");
//  getFunction(opencl, (void**)&_clEnqueueWaitForEvents, "clEnqueueWaitForEvents");
  getFunction(opencl, (void**)&_clEnqueueWriteBuffer, "clEnqueueWriteBuffer");
  getFunction(opencl, (void**)&_clEnqueueWriteBufferRect, "clEnqueueWriteBufferRect");
  getFunction(opencl, (void**)&_clEnqueueWriteImage, "clEnqueueWriteImage");
  getFunction(opencl, (void**)&_clFinish, "clFinish");
  getFunction(opencl, (void**)&_clFlush, "clFlush");
  getFunction(opencl, (void**)&_clGetCommandQueueInfo, "clGetCommandQueueInfo");
  getFunction(opencl, (void**)&_clGetContextInfo, "clGetContextInfo");
  getFunction(opencl, (void**)&_clGetDeviceAndHostTimer, "clGetDeviceAndHostTimer");
  getFunction(opencl, (void**)&_clGetDeviceIDs, "clGetDeviceIDs");
  getFunction(opencl, (void**)&_clGetDeviceInfo, "clGetDeviceInfo");
  getFunction(opencl, (void**)&_clGetEventInfo, "clGetEventInfo");
  getFunction(opencl, (void**)&_clGetEventProfilingInfo, "clGetEventProfilingInfo");
//  getFunction(opencl, (void**)&_clGetExtensionFunctionAddress, "clGetExtensionFunctionAddress");
  getFunction(opencl, (void**)&_clGetExtensionFunctionAddressForPlatform, "clGetExtensionFunctionAddressForPlatform");
//  getFunction(opencl, (void**)&_clGetGLObjectInfo, "clGetGLObjectInfo");
//  getFunction(opencl, (void**)&_clGetGLTextureInfo, "clGetGLTextureInfo");
  getFunction(opencl, (void**)&_clGetHostTimer, "clGetHostTimer");
  getFunction(opencl, (void**)&_clGetImageInfo, "clGetImageInfo");
  getFunction(opencl, (void**)&_clGetKernelArgInfo, "clGetKernelArgInfo");
  getFunction(opencl, (void**)&_clGetKernelInfo, "clGetKernelInfo");
  getFunction(opencl, (void**)&_clGetKernelSubGroupInfo, "clGetKernelSubGroupInfo");
  getFunction(opencl, (void**)&_clGetKernelWorkGroupInfo, "clGetKernelWorkGroupInfo");
  getFunction(opencl, (void**)&_clGetMemObjectInfo, "clGetMemObjectInfo");
  getFunction(opencl, (void**)&_clGetPipeInfo, "clGetPipeInfo");
  getFunction(opencl, (void**)&_clGetPlatformIDs, "clGetPlatformIDs");
  getFunction(opencl, (void**)&_clGetPlatformInfo, "clGetPlatformInfo");
  getFunction(opencl, (void**)&_clGetProgramBuildInfo, "clGetProgramBuildInfo");
  getFunction(opencl, (void**)&_clGetProgramInfo, "clGetProgramInfo");
  getFunction(opencl, (void**)&_clGetSamplerInfo, "clGetSamplerInfo");
  getFunction(opencl, (void**)&_clGetSupportedImageFormats, "clGetSupportedImageFormats");
  getFunction(opencl, (void**)&_clLinkProgram, "clLinkProgram");
  getFunction(opencl, (void**)&_clReleaseCommandQueue, "clReleaseCommandQueue");
  getFunction(opencl, (void**)&_clReleaseContext, "clReleaseContext");
  getFunction(opencl, (void**)&_clReleaseDevice, "clReleaseDevice");
  getFunction(opencl, (void**)&_clReleaseEvent, "clReleaseEvent");
  getFunction(opencl, (void**)&_clReleaseKernel, "clReleaseKernel");
  getFunction(opencl, (void**)&_clReleaseMemObject, "clReleaseMemObject");
  getFunction(opencl, (void**)&_clReleaseProgram, "clReleaseProgram");
  getFunction(opencl, (void**)&_clReleaseSampler, "clReleaseSampler");
  getFunction(opencl, (void**)&_clRetainCommandQueue, "clRetainCommandQueue");
  getFunction(opencl, (void**)&_clRetainContext, "clRetainContext");
  getFunction(opencl, (void**)&_clRetainDevice, "clRetainDevice");
  getFunction(opencl, (void**)&_clRetainEvent, "clRetainEvent");
  getFunction(opencl, (void**)&_clRetainKernel, "clRetainKernel");
  getFunction(opencl, (void**)&_clRetainMemObject, "clRetainMemObject");
  getFunction(opencl, (void**)&_clRetainProgram, "clRetainProgram");
  getFunction(opencl, (void**)&_clRetainSampler, "clRetainSampler");
//  getFunction(opencl, (void**)&_clSetCommandQueueProperty, "clSetCommandQueueProperty");
  getFunction(opencl, (void**)&_clSetContextDestructorCallback, "clSetContextDestructorCallback");
  getFunction(opencl, (void**)&_clSetDefaultDeviceCommandQueue, "clSetDefaultDeviceCommandQueue");
  getFunction(opencl, (void**)&_clSetEventCallback, "clSetEventCallback");
  getFunction(opencl, (void**)&_clSetKernelArg, "clSetKernelArg");
  getFunction(opencl, (void**)&_clSetKernelArgSVMPointer, "clSetKernelArgSVMPointer");
  getFunction(opencl, (void**)&_clSetKernelExecInfo, "clSetKernelExecInfo");
  getFunction(opencl, (void**)&_clSetMemObjectDestructorCallback, "clSetMemObjectDestructorCallback");
//  getFunction(opencl, (void**)&_clSetProgramReleaseCallback, "clSetProgramReleaseCallback");
  getFunction(opencl, (void**)&_clSetProgramSpecializationConstant, "clSetProgramSpecializationConstant");
  getFunction(opencl, (void**)&_clSetUserEventStatus, "clSetUserEventStatus");
  getFunction(opencl, (void**)&_clSVMAlloc, "clSVMAlloc");
  getFunction(opencl, (void**)&_clSVMFree, "clSVMFree");
//  getFunction(opencl, (void**)&_clUnloadCompiler, "clUnloadCompiler");
  getFunction(opencl, (void**)&_clUnloadPlatformCompiler, "clUnloadPlatformCompiler");
  getFunction(opencl, (void**)&_clWaitForEvents, "clWaitForEvents");

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

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_ninit
  (JNIEnv* e, jclass c, jstring jopencl)
{
  if (opencl_loaded) return opencl_loaded;

  const char* openclFile = e->GetStringUTFChars(jopencl, NULL);

  jboolean ret = opencl_init(openclFile);

  e->ReleaseStringUTFChars(jopencl, openclFile);

  if (!ret) return JNI_FALSE;

  //get JNI IDs

  opencl_loaded = JNI_TRUE;

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
  res = (*_clGetPlatformIDs)(1, &platform_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get platform id!\n");
    return JNI_FALSE;
  }

  //get device_id
  res = (*_clGetDeviceIDs)(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get device id!\n");
    return JNI_FALSE;
  }

  // Create a compute context
  context = (*_clCreateContext)(0, 1, &device_id, NULL, NULL, &err);
  if (!context)
  {
    printf("Error: Failed to create a compute context!\n");
    return JNI_FALSE;
  }

  // Create a command commands
  commands = (*_clCreateCommandQueueWithProperties)(context, device_id, NULL, &err);
  if (!commands)
  {
    printf("Error: Failed to create a command commands!\n");
    return EXIT_FAILURE;
  }

  // Create the compute program from the source buffer
  program = (*_clCreateProgramWithSource)(context, 1, (const char**) & KernelSource, NULL, &err);
  if (!program)
  {
    printf("Error: Failed to create compute program!\n");
    return EXIT_FAILURE;
  }

  // Build the program executable
  err = (*_clBuildProgram)(program, 0, NULL, NULL, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    size_t len;
    char buffer[2048];

    printf("Error: Failed to build program executable!\n");
    (*_clGetProgramBuildInfo)(program, device_id, CL_PROGRAM_BUILD_LOG, sizeof(buffer), buffer, &len);
    printf("%s\n", buffer);
    return JNI_FALSE;
  }

  // Create the compute kernel in the program we wish to run
  kernel = (*_clCreateKernel)(program, "square", &err);
  if (!kernel || err != CL_SUCCESS)
  {
    printf("Error: Failed to create compute kernel!\n");
    return JNI_FALSE;
  }

  // Create the input and output arrays in device memory for our calculation
  //
  input = (*_clCreateBuffer)(context,  CL_MEM_READ_ONLY,  sizeof(float) * count, NULL, NULL);
  output = (*_clCreateBuffer)(context, CL_MEM_WRITE_ONLY, sizeof(float) * count, NULL, NULL);
  if (!input || !output)
  {
    printf("Error: Failed to allocate device memory!\n");
    return JNI_FALSE;
  }

  // Write our data set into the input array in device memory
  err = (*_clEnqueueWriteBuffer)(commands, input, CL_TRUE, 0, sizeof(float) * count, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write to source array!\n");
    return JNI_FALSE;
  }

  // Set the arguments to our compute kernel
  err  = (*_clSetKernelArg)(kernel, 0, sizeof(cl_mem), &input);
  err |= (*_clSetKernelArg)(kernel, 1, sizeof(cl_mem), &output);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to set kernel arguments! %d\n", err);
    return JNI_FALSE;
  }

  // Get the maximum work group size for executing the kernel on the device
  //
  err = (*_clGetKernelWorkGroupInfo)(kernel, device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  // Execute the kernel over the entire range of our 1d input data set
  // using the maximum number of work group items for this device
  //
  global = count;
  err = (*_clEnqueueNDRangeKernel)(commands, kernel, 1, NULL, &global, &local, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute kernel!\n");
    return JNI_FALSE;
  }

  // Wait for the command commands to get serviced before reading back results
  //
  (*_clFinish)(commands);

  // Read back the results from the device to verify the output
  err = (*_clEnqueueReadBuffer)(commands, output, CL_TRUE, 0, sizeof(float) * count, results, 0, NULL, NULL);
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
  (*_clReleaseMemObject)(input);
  (*_clReleaseMemObject)(output);
  (*_clReleaseProgram)(program);
  (*_clReleaseKernel)(kernel);
  (*_clReleaseCommandQueue)(commands);
  (*_clReleaseContext)(context);

  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_cl_CL_ncreate
  (JNIEnv *e, jclass o, jstring src, jint type)
{
  int res, err;
  CLContext *ctx = (CLContext*)malloc(sizeof(CLContext));
  memset(ctx, 0, sizeof(CLContext));

  //get platform_id
  res = (*_clGetPlatformIDs)(1, &ctx->platform_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get platform id!\n");
    free(ctx);
    return 0;
  }

  //get device_id
  res = (*_clGetDeviceIDs)(ctx->platform_id, type, 1, &ctx->device_id, NULL);

  if (res != CL_SUCCESS) {
    printf("Error: Failed to get device id!\n");
    free(ctx);
    return 0;
  }

  // Create a compute context
  ctx->context = (*_clCreateContext)(0, 1, &ctx->device_id, NULL, NULL, &err);
  if (!ctx->context)
  {
    printf("Error: Failed to create a compute context!\n");
    free(ctx);
    return 0;
  }

  // Create a command commands
  ctx->commands = (*_clCreateCommandQueueWithProperties)(ctx->context, ctx->device_id, NULL, &err);
  if (!ctx->commands)
  {
    printf("Error: Failed to create a command commands!\n");
    free(ctx);
    return 0;
  }

  const char *c_src = e->GetStringUTFChars(src, NULL);

  // Create the compute program from the source buffer
  ctx->program = (*_clCreateProgramWithSource)(ctx->context, 1, (const char**)&c_src, NULL, &err);

  e->ReleaseStringUTFChars(src, c_src);

  if (!ctx->program)
  {
    printf("Error: Failed to create compute program!\n");
    free(ctx);
    return 0;
  }

  // Build the program executable
  err = (*_clBuildProgram)(ctx->program, 0, NULL, NULL, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    size_t len;
    char buffer[2048];

    printf("Error: Failed to build program executable!\n");
    (*_clGetProgramBuildInfo)(ctx->program, ctx->device_id, CL_PROGRAM_BUILD_LOG, sizeof(buffer), buffer, &len);
    printf("%s\n", buffer);
    free(ctx);
    return 0;
  }

  return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_cl_CL_nkernel
  (JNIEnv *e, jclass o, jlong ctx_ptr, jstring kernel)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;
  int err;

  const char *c_kernel = e->GetStringUTFChars(kernel, NULL);

  // Create the compute kernel in the program we wish to run
  jlong kernel_ptr = (jlong)(*_clCreateKernel)(ctx->program, c_kernel, &err);

  e->ReleaseStringUTFChars(kernel, c_kernel);

  if (!kernel_ptr || err != CL_SUCCESS)
  {
    printf("Error: Failed to create compute kernel!\n");
    return 0;
  }

  return kernel_ptr;
}

JNIEXPORT jlong JNICALL Java_javaforce_cl_CL_ncreateBuffer
  (JNIEnv *e, jobject o, jlong ctx_ptr, jint size, jint type)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;

  cl_mem buffer = (*_clCreateBuffer)(ctx->context, type, size, NULL, NULL);

  return (jlong)buffer;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nsetArg
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong kernel, jint idx, jbyteArray data)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  int size = e->GetArrayLength(data);

  int err = (*_clSetKernelArg)((cl_kernel)kernel, idx, size, dataptr);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to set kernel arguments! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nwriteBufferi8
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong buffer, jbyteArray data)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  int size = e->GetArrayLength(data);

  int err = (*_clEnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nwriteBufferf32
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong buffer, jfloatArray data)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  int size = e->GetArrayLength(data);

  int err = (*_clEnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nexecute
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong kernel, jint count)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  size_t global = count;
  size_t local;

  int err = (*_clGetKernelWorkGroupInfo)((cl_kernel)kernel, ctx->device_id, CL_KERNEL_WORK_GROUP_SIZE, sizeof(local), &local, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to retrieve kernel work group info! %d\n", err);
    return JNI_FALSE;
  }

  err = (*_clEnqueueNDRangeKernel)(ctx->commands, (cl_kernel)kernel, 1, NULL, &global, &local, 0, NULL, NULL);
  if (err)
  {
    printf("Error: Failed to execute kernel!\n");
    return JNI_FALSE;
  }

  (*_clFinish)(ctx->commands);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nreadBufferi8
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong buffer, jbyteArray data)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  int size = e->GetArrayLength(data);

  int err = (*_clEnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nreadBufferf32
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong buffer, jfloatArray data)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  int size = e->GetArrayLength(data);

  int err = (*_clEnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, dataptr, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  e->ReleasePrimitiveArrayCritical(data, dataptr, JNI_ABORT);

  return err == CL_SUCCESS;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nfreeKernel
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong kernel)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_clReleaseKernel)((cl_kernel)kernel);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nfreeBuffer
  (JNIEnv *e, jclass o, jlong ctx_ptr, jlong buffer)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_clReleaseMemObject)((cl_mem)buffer);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_cl_CL_nclose
  (JNIEnv *e, jclass o, jlong ctx_ptr)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_clReleaseProgram)(ctx->program);
  (*_clReleaseCommandQueue)(ctx->commands);
  (*_clReleaseContext)(ctx->context);
  free(ctx);

  return JNI_TRUE;
}
