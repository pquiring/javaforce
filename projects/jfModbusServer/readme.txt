jfModbusServer
=============

Modbus Server for Raspberry PI 2/3

Controls GPIO I/O pins remotely using Modbus TCP protocol.

Includes pre-built native library for Raspberry PI Debian build.

Modbus runs on port 502 so you should use sudo to run as root.

modbus.cfg
----------
port=#
  configure modbus port (decimal) (default = 502)
GPIO:I/O:addr=#;pin=#
  setup I/O pin as input or output
    addr = modbus coil or discrete number
invert=true
  invert GPIO pin logic
I2C:I/O:addr=#;slaveaddr=#;type={type};read=...;write=...;avg=#,#
  setup I2C device for read/write where:
    addr = starting modbus register (each is 16bit so could span multiple registers)
    type = int8, int16, int24, int32, float32, float64
    slaveaddr = address of I2C slave device (hex)
    read bytes can include I# to indicate bytes to return
    write bytes can include O# to indicate bytes to write
    avg = samples , interval (optional)
      modbus read requests will return average of last # samples
      samples = # of samples to keep track of
      interval = ms between samples
      a timer is used to continously read samples

Example config
--------------
GPIO:=addr=1;bit=O;dir=I
I2C:addr=2;slaveaddr=2a;type=int32;write=00,55,11;read=00,66,I0,I1,I2,I3

Notes :
  - you must enable the I2C interface in raspi-config and reboot
  - the config has changed from version 0.1
  - I2C input device can write (optional) and then read bytes
  - I2C output device can only write bytes

Author : Peter Quiring (pquiring@gmail.com)

Website : jfmodbusserver.sourceforge.net

Source : github.com/pquiring/javaforce (projects/jfmodbusserver)

Version : 0.2

Released : Mar ?, 2019

