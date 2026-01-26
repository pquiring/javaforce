//OpenCL FFM

jboolean clLoadLibrary(char* openclFile)
{
  if (opencl_loaded) return opencl_loaded;

  jboolean ret = opencl_init(openclFile);

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

jlong clCreate(char* src, jint type)
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

  // Create the compute program from the source buffer
  ctx->program = (*_cl_CreateProgramWithSource)(ctx->context, 1, (const char**)&src, NULL, &err);

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

jlong clKernel(jlong ctx_ptr, char* kernel)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;
  int err;

  // Create the compute kernel in the program we wish to run
  jlong kernel_ptr = (jlong)(*_cl_CreateKernel)(ctx->program, kernel, &err);

  if (!kernel_ptr || err != CL_SUCCESS)
  {
    printf("Error: Failed to create compute kernel!\n");
    return 0;
  }

  return kernel_ptr;
}

jlong clCreateBuffer(jlong ctx_ptr, jint size, jint type)
{
  if (ctx_ptr == 0) return 0;
  CLContext *ctx = (CLContext*)ctx_ptr;

  cl_mem buffer = (*_cl_CreateBuffer)(ctx->context, type, size, NULL, NULL);

  return (jlong)buffer;
}

jboolean clSetArg(jlong ctx_ptr, jlong kernel, jint idx, jbyte* data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  int err = (*_cl_SetKernelArg)((cl_kernel)kernel, idx, size, data);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to set kernel arguments! %d\n", err);
  }

  return err == CL_SUCCESS;
}

jboolean clWriteBufferi8(jlong ctx_ptr, jlong buffer, jbyte* data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  int err = (*_cl_EnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  return err == CL_SUCCESS;
}

jboolean clWriteBufferf32(jlong ctx_ptr, jlong buffer, jfloat* data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  int err = (*_cl_EnqueueWriteBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  return err == CL_SUCCESS;
}

jboolean clExecute(jlong ctx_ptr, jlong kernel, jint count)
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

jboolean clExecute2(jlong ctx_ptr, jlong kernel, jint count1, jint count2)
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

jboolean clExecute3(jlong ctx_ptr, jlong kernel, jint count1, jint count2, jint count3)
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

jboolean clExecute4(jlong ctx_ptr, jlong kernel, jint count1, jint count2, jint count3, jint count4)
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

jboolean clReadBufferi8(jlong ctx_ptr, jlong buffer, jbyte* data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  int err = (*_cl_EnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  return err == CL_SUCCESS;
}

jboolean clReadBufferf32(jlong ctx_ptr, jlong buffer, jfloat* data, jint size)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  int err = (*_cl_EnqueueReadBuffer)(ctx->commands, (cl_mem)buffer, CL_TRUE, 0, size * 4, data, 0, NULL, NULL);
  if (err != CL_SUCCESS)
  {
    printf("Error: Failed to write buffer! %d\n", err);
  }

  return err == CL_SUCCESS;
}

jboolean clFreeKernel(jlong ctx_ptr, jlong kernel)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseKernel)((cl_kernel)kernel);

  return JNI_TRUE;
}

jboolean clFreeBuffer(jlong ctx_ptr, jlong buffer)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseMemObject)((cl_mem)buffer);

  return JNI_TRUE;
}

jboolean clClose(jlong ctx_ptr)
{
  if (ctx_ptr == 0) return JNI_FALSE;
  CLContext *ctx = (CLContext*)ctx_ptr;

  (*_cl_ReleaseProgram)(ctx->program);
  (*_cl_ReleaseCommandQueue)(ctx->commands);
  (*_cl_ReleaseContext)(ctx->context);
  free(ctx);

  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_clLoadLibrary)(char*) = &clLoadLibrary;
  JNIEXPORT jlong (*_clCreate)(char*,jint) = &clCreate;
  JNIEXPORT jlong (*_clKernel)(jlong,char*) = &clKernel;
  JNIEXPORT jlong (*_clCreateBuffer)(jlong,jint,jint) = &clCreateBuffer;
  JNIEXPORT jboolean (*_clSetArg)(jlong,jlong,jint idx,jbyte*,jint) = &clSetArg;
  JNIEXPORT jboolean (*_clWriteBufferi8)(jlong,jlong,jbyte*,jint) = &clWriteBufferi8;
  JNIEXPORT jboolean (*_clWriteBufferf32)(jlong,jlong,jfloat*,jint) = &clWriteBufferf32;
  JNIEXPORT jboolean (*_clExecute)(jlong,jlong,jint) = &clExecute;
  JNIEXPORT jboolean (*_clExecute2)(jlong,jlong,jint,jint) = &clExecute2;
  JNIEXPORT jboolean (*_clExecute3)(jlong,jlong,jint,jint,jint) = &clExecute3;
  JNIEXPORT jboolean (*_clExecute4)(jlong,jlong,jint,jint,jint,jint) = &clExecute4;
  JNIEXPORT jboolean (*_clReadBufferi8)(jlong,jlong,jbyte*,jint) = &clReadBufferi8;
  JNIEXPORT jboolean (*_clReadBufferf32)(jlong,jlong,jfloat*,jint) = &clReadBufferf32;
  JNIEXPORT jboolean (*_clFreeKernel)(jlong,jlong) = &clFreeKernel;
  JNIEXPORT jboolean (*_clFreeBuffer)(jlong,jlong) = &clFreeBuffer;
  JNIEXPORT jboolean (*_clClose)(jlong) = &clClose;

  JNIEXPORT jboolean CLAPIinit() {return JNI_TRUE;}
}
