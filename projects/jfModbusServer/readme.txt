jfModbusServer
=============

Modbus Server for Raspberry PI 2/3

Controls GPIO I/O pins remotely using Modbus TCP protocol.

Modbus runs on port 502 so you should use sudo to run as root.

modbus.cfg
----------
port=#
GPIO#=I/O

Example config (4 output, 4 input)
--------------
GPIO1=O
GPIO2=O
GPIO3=O
GPIO4=O
GPIO5=I
GPIO6=I
GPIO7=I
GPIO8=I

Author : Peter Quiring (pquiring@gmail.com)

Website : jfmodbusserver.sourceforge.net

Source : github.com/pquiring/javaforce (projects/jfmodbusserver)

Version : 0.1

Released : ? ? 2018
