//OpenCL

#define CL_TARGET_OPENCL_VERSION 300

#include "../opencl/CL/cl.h"
#include "../opencl/CL/cl_function_types.h"

JF_LIB_HANDLE opencl = NULL;

static jboolean opencl_loaded = JNI_FALSE;

//functions
clGetPlatformIDs_fn _clGetPlatformIDs;
clGetDeviceIDs_fn _clGetDeviceIDs;
clCreateContext_fn _clCreateContext;
clCreateCommandQueueWithProperties_fn _clCreateCommandQueueWithProperties;
clCreateProgramWithSource_fn _clCreateProgramWithSource;
clBuildProgram_fn _clBuildProgram;
clCreateKernel_fn _clCreateKernel;
clCreateBuffer_fn _clCreateBuffer;
clEnqueueWriteBuffer_fn _clEnqueueWriteBuffer;
clFinish_fn _clFinish;
clSetKernelArg_fn _clSetKernelArg;
clGetKernelWorkGroupInfo_fn _clGetKernelWorkGroupInfo;
clGetProgramBuildInfo_fn _clGetProgramBuildInfo;
clEnqueueNDRangeKernel_fn _clEnqueueNDRangeKernel;
clEnqueueReadBuffer_fn _clEnqueueReadBuffer;
clReleaseMemObject_fn _clReleaseMemObject;
clReleaseProgram_fn _clReleaseProgram;
clReleaseKernel_fn _clReleaseKernel;
clReleaseCommandQueue_fn _clReleaseCommandQueue;
clReleaseContext_fn _clReleaseContext;

static jboolean opencl_init(const char* openclFile)
{
  printf("opencl init...");

  opencl = loadLibrary(openclFile);
  if (opencl == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), openclFile);
    return JNI_FALSE;
  }

  //get functions
  getFunction(opencl, (void**)&_clGetPlatformIDs, "clGetPlatformIDs");
  getFunction(opencl, (void**)&_clGetDeviceIDs, "clGetDeviceIDs");
  getFunction(opencl, (void**)&_clCreateContext, "clCreateContext");
  getFunction(opencl, (void**)&_clCreateCommandQueueWithProperties, "clCreateCommandQueueWithProperties");
  getFunction(opencl, (void**)&_clCreateProgramWithSource, "clCreateProgramWithSource");
  getFunction(opencl, (void**)&_clBuildProgram, "clBuildProgram");
  getFunction(opencl, (void**)&_clCreateKernel, "clCreateKernel");
  getFunction(opencl, (void**)&_clCreateBuffer, "clCreateBuffer");
  getFunction(opencl, (void**)&_clFinish, "clFinish");
  getFunction(opencl, (void**)&_clEnqueueWriteBuffer, "clEnqueueWriteBuffer");
  getFunction(opencl, (void**)&_clSetKernelArg, "clSetKernelArg");
  getFunction(opencl, (void**)&_clGetKernelWorkGroupInfo, "clGetKernelWorkGroupInfo");
  getFunction(opencl, (void**)&_clGetProgramBuildInfo, "clGetProgramBuildInfo");
  getFunction(opencl, (void**)&_clEnqueueNDRangeKernel, "clEnqueueNDRangeKernel");
  getFunction(opencl, (void**)&_clEnqueueReadBuffer, "clEnqueueReadBuffer");
  getFunction(opencl, (void**)&_clReleaseMemObject, "clReleaseMemObject");
  getFunction(opencl, (void**)&_clReleaseProgram, "clReleaseProgram");
  getFunction(opencl, (void**)&_clReleaseKernel, "clReleaseKernel");
  getFunction(opencl, (void**)&_clReleaseCommandQueue, "clReleaseCommandQueue");
  getFunction(opencl, (void**)&_clReleaseContext, "clReleaseContext");


  printf("ok\n");

  return JNI_TRUE;
}

#define DATA_SIZE (1024)

// Simple compute kernel which computes the square of an input array
//
static const char* KernelSource = "\n" \
"__kernel void square(__global float* input, __global float* output, const unsigned int count)\n" \
"{\n" \
"   int i = get_global_id(0);\n" \
"   if(i < count)\n" \
"       output[i] = input[i] * input[i];\n" \
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

  if (res != CL_SUCCESS) return JNI_FALSE;

  //get device_id
  res = (*_clGetDeviceIDs)(platform_id, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL);

  if (res != CL_SUCCESS) return JNI_FALSE;

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
  err = 0;
  err  = (*_clSetKernelArg)(kernel, 0, sizeof(cl_mem), &input);
  err |= (*_clSetKernelArg)(kernel, 1, sizeof(cl_mem), &output);
  err |= (*_clSetKernelArg)(kernel, 2, sizeof(unsigned int), &count);
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
  err = (*_clEnqueueReadBuffer)( commands, output, CL_TRUE, 0, sizeof(float) * count, results, 0, NULL, NULL );
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
  printf("Computed '%d/%d' correct values!\n", correct, count);

  // Shutdown and cleanup
  (*_clReleaseMemObject)(input);
  (*_clReleaseMemObject)(output);
  (*_clReleaseProgram)(program);
  (*_clReleaseKernel)(kernel);
  (*_clReleaseCommandQueue)(commands);
  (*_clReleaseContext)(context);

  return JNI_TRUE;
}
