/**
 * package javaforce.jbus;
 *
 * JBus is an inter-process communications protocol.
 *
 * JBusServer connects to a message bus with a specified name.
 * JBusClient connects to a message bus using a client supplied name.
 *
 * This is implemented using javaforce.ipc.DBus.
 * On Linux systems the specified bus name should start with "javaforce."
 *   or supply your own DBus conf in /etc/dbus-1/system.d
 *
 * This is a legacy system and was originally based on a TCP server/client transport.
 * Newer applications should use DBus instead.
 *
 */
package javaforce.bus;
