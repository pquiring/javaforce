jfModbusServer
==============

Modbus Server for Raspberry PI 2/3

Read/Write GPIO I/O pins and I2C devices remotely using Modbus TCP protocol.

Includes pre-built native library for Raspberry PI Debian build.

Modbus runs on port 502 which requires root access.
Access to /dev/mem is also required which requires root access.
  sudo ./run.sh

modbus.cfg
----------
PORT=#
  configure modbus port (decimal) (default = 502)
GPIO:I/O:addr=#:pin=#
  setup I/O pin as input or output
    addr = modbus coil or discrete number
    pin = GPIO pin # (1-40)
INVERT=true|false
  invert GPIO pin logic (default = false)
I2C:I/O:addr=#:slaveaddr=#:type={type}:read=...:write=...:avg=#,#
  setup I2C device for read/write where:
    addr = starting modbus register (each is 16bit so could span multiple registers)
    type = int8, int16, int24, int32, float32, float64
    slaveaddr = address of I2C slave device (hex)
    read bytes can include I# to indicate bytes to return (hex)
    write bytes can include O# to indicate bytes to write (hex)
    write bytes can include 'cs8' to add 8bit checksum byte of all preceding bytes
    avg = samples , interval (optional)
      modbus read requests will return average of last # samples
      samples = # of samples to keep track of (min = 10)
      interval = ms between samples (min = 100)
      a timer is used to continously read samples

Example config
--------------
#this will setup pin#25 as an output coil
GPIO:O:addr=1:pin=25
#this will read power meters from ncd.io
I2C:I:addr=1:slaveaddr=2a:type=int32:write=92,6a,01,01,01,00,00,cs8:read=I1,I2,I3,00

Notes :
  - you must enable the I2C interface in raspi-config and reboot
  - the config has changed from version 0.1
  - I2C input device can write (optional) and then read bytes
  - I2C output device can only write bytes
  - after installing .deb package you need to run 'systemctl enable jfmodbusserver' and reboot to enable the service

Author : Peter Quiring (pquiring@gmail.com)

Website : jfmodbusserver.sourceforge.net

Source : github.com/pquiring/javaforce (projects/jfmodbusserver)

Version : 0.3

Released : Feb 27, 2019

