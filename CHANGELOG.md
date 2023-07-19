# Release Notes

## metratec-connection-library 1.23.1

* MpsTunnelConnection: increased link default timeout to 61s
* MpsTunnelConnection: maximum BINXT frame size is now configurable

## metratec-connection-library 1.23.0

* upnp support removed
* log4j auto import removed

## metratec-connection-library 1.22.4

* upd broadcast optimized
* Added methods to get metraTec devices via UDP or UPNP
* use nrjavaserial version 5.1.1

## metratec-connection-library 1.22.3

* added FileConnection as an alternative to UsbConnection/Rs232Connection
* use nrjavaserial version 3.15.0

## metratec-connection-library 1.22.2

* added FileConnection type for working on raw device nodes
  (for embedded systems)

## metratec-connection-library 1.22.1

* communication logging with slf4j added
* TCP - the receive timeout can now also be set when the connection 
    is currently closed  

## metratec-connection-library 1.22

* declare licenses correctly (transitively), so a project
  depending on this lib can generate its LICENSE.txt automatically
* added MpsLegacyTunnelConnection, based on MPS DAT-commands
* fixed bogus read timeout exceptions in MpsTunnelConnection.recv()
  when -1 is expected instead
* added MpsTunnelConnection.setLinkTimeout()/getLinkTimeout() to
  configure a separate link timeout
* minimum Java version is 1.7 again (as in connection-library v1.8)

## metratec-connection-library 1.21

* added scanning support for TUC 2 UPnP devices
* made CommConnectionException.getErrorDescription() more detailed
* MpsTunnelConnection:
  * Fixed read() method for bytes that have the 8th bit set
  * Fixed sending more than 96 bytes per send() call
  * Fixed enforcement of certain large receive and connection
    timeouts (numeric overflow)
  * Fixed linking to a slave device during connect()

## metratec-connection-library 1.20.1

* SSL connection timeout is now set correctly

## metratec-connection-library 1.20

* added MPS tunneling support (MpsTunnelConnection)

## metratec-connection-library 1.19.3

* fix usb reconnect crash

## metratec-connection-library 1.19.2

*  add license information
* add slf4j logger

## metratec-connection-library 1.19.1

* add toString() method to all connections

## metratec-connection-library 1.19

* refactor
* rename to metratec-connection-library

## connectionLibrary 1.18

* add ssl tcp connection
* add server mode for tcp connections  

## connectionLibrary 1.17.2

* fixed removing duplicates from UDPConnection.getMetratecEthernetDevices() and friends
  list of lantronix and metraTec devices are also automatically sorted now.

## connectionLibrary 1.17.1

* allow custom VID/PID pairs to be registered using USBConnection.addVIDPID()
  necessary in order to use HAMEG power supplies (0x0403/0xED72)
* USBConnection: Support 1.5 stop bits (QA-131)

## ConnectionLibrary 1.17

* USBConnection.setSerialNumber() no longer reconnect the connection.
  This has been broken in v1.16.
* Replaced RS232Connection backend `rxtx-rebundled` with `nrjavaserial`.
  This fixes serial connections on OS X.
* Fixed finding and opening USBConnections for devices with metraTec
  VID/PIDs on OS X.

## ConnectionLibrary 1.16

* used newer version of jd2xx - 2.0.8.17-8 which now supports mac os x (64 bit)
* RS232Connection and USBConnnection now implement the interface SerialConnection
* UDP broadcast on MAC OS X is supported provided only one interface has been
  enabled.

## ConnectionLibrary 1.15

* added ICommConnection implementation for (raw) printers: PrinterConnection
  see JavaDoc for more details.
* test/test/TestPrinter.java is a small example of printing a label via ZPL.

## ConnectionLibrary 1.14

* UDPConnection.getAllInterfaceInetAddresses() now only returns real
  interface addresses, and only IPv4 addresses. This is what you
  usually want when using the list of addresses for broadcasting.
  This significantly improves broadcasting performance.

## ConnectionLibrary 1.13

* Export UDPConnection.sendBroadcastRecvOnUnicast() which can be used
  on all platforms whenever the device responds with an unicast packet
  to the broadcast request.
* Fixed Lantronix device discovery in UDPConnection.getLantronixEthernetDevices()
  on Linux.
* Also extract MAC address from Lantronix responses. This fixes potential duplicate
  Lantronix devices being returned by UDPConnection.getLantronixEthernetDevices()

## ConnectionLibrary 1.12

* fixed UDPConnection.getLantronixEthernetDevices() and UDPConnection.getMetratecEthernetDevices()
  on systems with multiple network interfaces
* UDPConnection.sendRecvBroadcast() is a new method for building UDP broadcast
  clients/servers in a platform-independant manner.

## ConnectionLibrary 1.11

* allow configuration of data/stop bits, parity and flow control on USBConnection
* removed old commented code from UDPConnection

## ConnectionLibrary 1.10.1

* use new jd2xx-2.0.8.17-7: Writing the EEPROM of FT-X chipsets is now
* possible using the JD2XX API.

## Connection Library 1.10

* register metraTec USB VID/PID pairs using new JD2XX.setVIDPID() this should make devices with non-standard VID/PID
  pairs available on Linux
* internal cleanup
* fixed UDP broadcasting in UDPConnection.getLantronixEthernetDevices() and UDPConnection.getMetratecEthernetDevices().
  This fixed these methods, at least under Linux and probably other operating systems that don't allow broadcasts from
	the localhost address
* added ICommConnection.recv(byte[], int, int) and ICommConnection.recv(byte[]) for reading into byte buffers

## Connection Library 1.9

* fixed USBConnections on Windows i686 (JD2XX)
* extensive internal cleanup:
  * this resulted in backwards-compatible API changes of ICommConnection.recv()
  * which allows an arbitrary number of termination signs now
* buffer all connections: this theoretically improves performance significantly when reading on a connection

## connection Library 1.8

* added CommConnectionException.getErrorDescription()
* fixed send timeouts on USBConnections (JD2XX)

## Connection Library 1.7

* internal cleanup (Mavenization)
* use rebundled versions of RXTX and JD2XX:
* it is no longer necessary to ship the JNIs with the LibraryConnection jar

## Connection Library 1.6

* some internal cleanup

## Connection Library 1.5.2

* add sendTimeout to the USBConnection
* add USBConnection serial-number getter & setter
* add Apache Maven POM file

## Connection Library 1.5.1

* add isConnected() (replace deprecated getState())
* add disconnect()  (replace deprecated close()) 
* add getSocket() to the TCP, getSerialPort to the RS232 and getJD2XX to the USB connection class

## Connection Library V1.5

* add 64Bit support

## Connection Library V1.4.7

* UDP Connection use 'InetAddress.getLocalHost()' for default bind address, or set a one

## Connection Library V1.4.6

* 64-Bit handling
* USBConnection, remove Errorcodes from static methods
* RS232Connection, add 'NoClassDefFoundError' handling

## Connection Library V1.4.5

* add 'StringBuilder receive(int terminator)'
* update doc

## Connection Library V1.4.4

* TCPIP, update handling

## Connection Library V1.4.3

* RS232 bug fix, IOException message null pointer if reader removed

## Connection Library V1.4.2

* add getOutputStream to the ICommConnection interface

## Connection Library V1.4.1

* update documentation

## Connection Library V1.4

* USB - change constructors:
  * USBConnection(DeviceInfo usbDevice, int baudrate)
  * USBConnection(DeviceInfo usbDevice)
  * USBConnection(String usbDeviceSerialNumber, int baudrate)
  * USBConnection(String usbDeviceSerialNumber)
* change static method String[] getUSBDevices() into ArrayList<DeviceInfo> getUSBDevices()

## Connection Library V1.3

* RS232
  * add detailed error message
  * set default receiving timeout to 500 ms;
* USB
  * set default receiving timeout to 500 ms

## Connection Library V1.2

* remove not critical bugs (FindBug)

## Connection Library V1.1

* USBConnection V1.1
  * add throw NoClassDefFoundError Exception
* UDPConnection V1.1
  * add public static ArrayList<EthernetDevice> getMetratecEthernetDevices(int timeout) throws IOException
  * add public static ArrayList<EthernetDevice> getLantronixEthernetDevices(int timeout) throws IOException
  * add public static boolean configMetratecEthernetDeviceDHCP(String macAddress) throws CommConnectionException, IOException
  * add public static boolean configMetratecEthernetDeviceStatic(String macAddress,String ipAddress,String subnetmask,String gateway) throws CommConnectionException, IOException
  * add EthernetDevice Class 1.0
* ICommConnection V1.1
  * add public void send(byte[] senddata) throws CommConnectionException

## Connection Library V1.0

* CommConnectionException 1.0
* ICommConnection 1.0
* RS232Connection 1.0
* TCPIPConnection 1.0
* UDPConnection 1.0
* USBConnection 1.0
