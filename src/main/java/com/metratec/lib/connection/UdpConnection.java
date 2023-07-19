package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a udp port
 *
 * @author man
 * @version 1.22.4
 *
 */
@SuppressWarnings("PMD.EmptyCatchBlock")
public abstract class UdpConnection {
  
  private static Logger logger = LoggerFactory.getLogger(UdpConnection.class);
  private static InetAddress BROADCAST_ADDR;

  static {
    byte[] addr = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    try {
      BROADCAST_ADDR = InetAddress.getByAddress(addr);
    } catch (Exception e) {
    }
  }

  /**
   * Get the InetAddresses of all local network interfaces.
   *
   * Includes only IPv4 addresses and only real interface addresses (no local hosts etc.). It is
   * important to keep this list as small as possible since it is also the list we send broadcasts
   * on.
   *
   * @return List of addresses
   *
   * @throws SocketException if an I/O error occurs.
   */
  public static List<InetAddress> getAllInterfaceInetAddresses() throws SocketException {
    List<InetAddress> ret = new LinkedList<>();

    for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e
        .hasMoreElements();) {
      NetworkInterface iface = e.nextElement();

      /*
       * This excludes the loopback address 127.0.0.1 but not Microsoft loopback adapters.
       */
      if (iface.isLoopback() || !iface.isUp()) {
        continue;
      }
      
      for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
        if (addr instanceof Inet4Address) {
          ret.add(addr);
        }
      }
    }

    return ret;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("win");
  }

  private static boolean isMac() {
    return System.getProperty("os.name").toLowerCase().contains("os x");
  }

  /**
   * Send broadcast packets from a list of source addresses and receive answers on the corresponding
   * unicast addresses.
   * <p>
   * This can be used whenever a device responds to a broadcast packet with an unicast answer.
   * <p>
   * It can and must also be used on Windows to receive broadcast answers. However you should not
   * use this method in portable code that needs to receive broadcast answers since different
   * operating systems need different tricks to receive broadcast responses and associate them with
   * specific local interfaces. Always use
   * {@link #sendRecvBroadcast(DatagramPacket, int, int, int, List, UdpBroadcastHandlerInterface)}
   * for that purpose.
   *
   * @param sendPacket Packet to broadcast. The destination address is set to 255.255.255.255
   *        automatically. If this is null, no packet is sent.
   * @param srcPort Source port of outbound broadcasts and used to listen for responses.
   * @param recvLength Length of response packets. This will be the length of returned packets' data
   *        buffers. If less than or equal 0, do not try to receive any packets.
   * @param timeout Time to wait for responses in milliseconds.
   * @param addresses Addresses to bind to when broadcasting. This effectively determines the
   *        interfaces that sendPacket will be broadcast on.
   * @param handler A functor object for handling received responses/packets.
   *
   * @throws IOException if an unexpected I/O error occured.
   */
  /*
   * Also the implementation of sendRecvBroadcast() for Windows:
   *
   * On Windows we can and must reuse the socket used to send the broadcast to receive broadcast
   * answers. Using a separate receive socket does not work since it would have to bind to the same
   * port as the socket used for sending.
   *
   * The behaviour on Windows 7 is that the packet is broadcast on every interface in `addresses`
   * individually. Broadcast responses are only received on the same interface, so it is easy to
   * associate with a local interface.
   *
   * On Windows XP, the IP stack behaves differently and always routes broadcasts on every interface
   * regardless of the packet's source address. Since we use the same algorithm on Windows XP as on
   * Windows 7, the packet gets broadcast on every interface multiple times. Fortunately, we still
   * only receive broadcasts that get in via the interface with the same address as our source port,
   * so we can associate responses with the correct local interface.
   *
   * See also https://social.technet.microsoft.com/Forums/windows/en-US/
   * 72e7387a-9f2c-4bf4-a004-c89ddde1c8aa/
   * how-to-fix-the-global-broadcast-address-255255255255-behavior-on-windows?forum=
   * w7itpronetworking
   *
   * However, I cannot verify the claim that on Windows XP broadcasts are always sent with the
   * source address of an arbitrary adapter. Even if that would be the case, it would not matter
   * since we assume here that all responses to `sendPacket` are sent as full-broadcasts instead of
   * being addressed to sendPacket's source address.
   */
  public static void sendBroadcastRecvOnUnicast(DatagramPacket sendPacket, int srcPort,
      int recvLength, int timeout, List<InetAddress> addresses,
      UdpBroadcastHandlerInterface handler) throws IOException {
    DatagramPacket recvPacket;
    DatagramSocket socket;

    if (sendPacket != null) {
      sendPacket.setAddress(BROADCAST_ADDR);
    }

    for (InetAddress localAddr : addresses) {
      /*
       * We must create a new socket for each address since we cannot easily rebind an existing one.
       */
      socket = new DatagramSocket(srcPort, localAddr);
      try {
        socket.setBroadcast(true);
        socket.setSoTimeout(timeout);
        if (sendPacket != null) {
          if(logger.isTraceEnabled()) {
            logger.trace("broadcast send from "+localAddr.getHostAddress()+":"+srcPort+" - "+toHexString(sendPacket.getData()));
          }
          socket.send(sendPacket);
        }
      } catch (IOException e) {
        /* we cannot broadcast on every interface */
        socket.close();
        continue;
      }

      if (recvLength <= 0) {
        socket.close();
        continue;
      }
      long waitUntil = System.currentTimeMillis() + timeout;
      try {
        while (waitUntil >= System.currentTimeMillis()) {
          try {
            recvPacket = new DatagramPacket(new byte[recvLength], recvLength);
  
            socket.receive(recvPacket);
            if(logger.isTraceEnabled()) {
              logger.trace("broadcast recv from  "+recvPacket.getAddress().getHostAddress()+":"+recvPacket.getPort()+" - "+toHexString(recvPacket.getData()));
            }
            if (!handler.handle(localAddr, recvPacket, socket)) {
              return;
            }
          } catch (SocketTimeoutException e) {
            /*
             * expected when all broadcasts have been received
             */
          }
        }
      }  finally {
        socket.close();
      }
    }
  }

  /*
   * Implementation of sendRecvBroadcast() on Linux. This method is private since it makes no sense
   * on Windows (or other platforms).
   *
   * Here we send (broadcast) packets like in sendBroadcastRecvOnUnicast() but must use a separate
   * receive socket since we can only receive broadcasts to the all-broadcast address reliably when
   * binding to the all-broadcast socket. We cannot reuse the send socket since it is bound to a
   * local interface address.
   */
  private static void sendBroadcastRecvOnBroadcast(DatagramPacket sendPacket, int srcPort,
      int recvLength, int timeout, List<InetAddress> addresses,
      UdpBroadcastHandlerInterface handler) throws IOException {
    DatagramSocket recvSocket;
    DatagramPacket recvPacket;

    DatagramSocket sendSocket;

    if (sendPacket != null) {
      sendPacket.setAddress(BROADCAST_ADDR);
    }

    /*
     * It is assumed that devices will respond to sendPacket on the same port it was sent out
     * (sendPacket's source port, i.e. the port that the send socket binds to). Since we must use a
     * separate socket for receiving, we fix sendPacket's source port and the listening port to
     * `srcPort`.
     */
    recvSocket = new DatagramSocket(srcPort, BROADCAST_ADDR);
    try {
      recvSocket.setBroadcast(true);
      recvSocket.setReuseAddress(true);
      recvSocket.setSoTimeout(25);

      for (InetAddress localAddr : addresses) {
        /*
         * We must create a new socket for each address since we cannot easily rebind an existing
         * one.
         */
        sendSocket = new DatagramSocket(srcPort, localAddr);
        try {
          sendSocket.setBroadcast(true);
          if (sendPacket != null) {
            if(logger.isTraceEnabled()) {
              logger.trace("broadcast send from "+localAddr.getHostAddress()+":"+srcPort+" - "+toHexString(sendPacket.getData()));
            }
            sendSocket.send(sendPacket);
          }
        } catch (IOException e) {
          /* we cannot broadcast on every interface */
          sendSocket.close();
          continue;
        }

        if (recvLength <= 0) {
          sendSocket.close();
          continue;
        }
        long waitUntil = System.currentTimeMillis() + timeout;
        try {
          while (waitUntil >= System.currentTimeMillis()) {
            try{
              recvPacket = new DatagramPacket(new byte[recvLength], recvLength);
              recvSocket.receive(recvPacket);
              if(logger.isTraceEnabled()) {
                logger.trace("broadcast recv from  "+recvPacket.getAddress().getHostAddress()+":"+recvPacket.getPort()+" - "+toHexString(recvPacket.getData()));
              }
              if (!handler.handle(localAddr, recvPacket, sendSocket)) {
                return;
              }
            } catch (SocketTimeoutException e) {
              /*
              * expected when all broadcasts have been received
              */
            }
          }
        }  finally {
          sendSocket.close();
        }
      }
    } finally {
      recvSocket.close();
    }
  }

  /*
   * Implementation of sendRecvBroadcast() on BSD-derived systems like MAC OS X. This method is
   * private since it makes no sense on Windows and Linux.
   *
   * The implementation is almost identical to sendBroadcastRecvOnUnicast(). The difference is that
   * the socket is not bound to any interface. No packets are received via the socket bound to an
   * interface. This also means this implementation will not work properly when multiple interfaces
   * are active on the system.
   *
   * NOTES:
   *
   * Required socket options like IP_ONESBCAST (see BSD�s ip(4)) or IP_RECVIF are either not
   * supported in OS X or not provided by the Java API.
   *
   * Raw sockets (e.g. RockSaw) can not be used to receive UDP packets on BSD-derived systems like
   * OS X.
   *
   * BSD provides the BPF API but there is no Java wrapper for it.
   *
   * One could do it in C(JNI) but it would require us to maintain another library and would be not
   * worth the effort for a small gain.
   *
   * Another alternative pcap4j does not bundle libpcap and is not fully supported in OS X.
   */
  private static void sendBroadcastRecvOnAny(DatagramPacket sendPacket, int srcPort, int recvLength,
      int timeout, List<InetAddress> addresses, UdpBroadcastHandlerInterface handler)
      throws IOException {
    DatagramPacket recvPacket;
    DatagramSocket socket;

    for (InetAddress localAddr : addresses) {
      /*
       * We must create a new socket for each address since we cannot easily rebind an existing one.
       */
      socket = new DatagramSocket(srcPort);
      try {
        socket.setBroadcast(true);
        socket.setSoTimeout(timeout);
        if (sendPacket != null) {
          sendPacket.setAddress(BROADCAST_ADDR);
          if(logger.isTraceEnabled()) {
            logger.trace("broadcast send from "+localAddr.getHostAddress()+":"+srcPort+" - "+toHexString(sendPacket.getData()));
          }
          socket.send(sendPacket);
        }
      } catch (IOException e) {
        /* we cannot broadcast on every interface */
        socket.close();
        continue;
      }

      if (recvLength <= 0) {
        socket.close();
        continue;
      }
      long waitUntil = System.currentTimeMillis() + timeout;
      try {
        while(waitUntil >= System.currentTimeMillis()) {
          try{
            recvPacket = new DatagramPacket(new byte[recvLength], recvLength);
  
            socket.receive(recvPacket);
            if(logger.isTraceEnabled()) {
              logger.trace("broadcast recv from  "+recvPacket.getAddress().getHostAddress()+":"+recvPacket.getPort()+" - "+toHexString(recvPacket.getData()));
            }
            if (!handler.handle(localAddr, recvPacket, socket)) {
              return;
            }
          } catch (SocketTimeoutException e) {
            /*
             * expected when all broadcasts have been received
             */
          }
        }
      }  finally {
        socket.close();
      }
    }
  }

  /**
   * Send broadcast packets from all local interface addresses and receive broadcast answers in a
   * platform-independant manner.
   *
   * @param sendPacket Packet to broadcast. The destination address is set to 255.255.255.255
   *        automatically. If this is null, no packet is sent.
   * @param srcPort Source port of outbound broadcasts and used for listening for broadcasts.
   * @param recvLength Length of broadcast responses. This will be the length of returned packets'
   *        data buffers. If less than or equal 0, do not try to receive any packets.
   * @param timeout Time to wait for responses in milliseconds.
   * @param handler A functor object for handling received broadcast responses/packets.
   *
   * @throws IOException if an unexpected I/O error occured.
   */
  public static void sendRecvBroadcast(DatagramPacket sendPacket, int srcPort, int recvLength,
      int timeout, UdpBroadcastHandlerInterface handler) throws IOException {
    List<InetAddress> addresses = getAllInterfaceInetAddresses();

    sendRecvBroadcast(sendPacket, srcPort, recvLength, timeout, addresses, handler);
  }

  /**
   * Send broadcast packets from a single source source address and receive broadcast answers in a
   * platform-independant manner.
   *
   * @param sendPacket Packet to broadcast. The destination address is set to 255.255.255.255
   *        automatically. If this is null, no packet is sent.
   * @param srcPort Source port of outbound broadcasts and used for listening for broadcasts.
   * @param recvLength Length of broadcast responses. This will be the length of returned packets'
   *        data buffers. If less than or equal 0, do not try to receive any packets.
   * @param timeout Time to wait for responses in milliseconds.
   * @param addr Address to bind to when broadcasting. This effectively determines the interface
   *        that sendPacket will be broadcast on.
   * @param handler A functor object for handling received broadcast responses/packets.
   *
   * @throws IOException if an unexpected I/O error occured.
   */
  public static void sendRecvBroadcast(DatagramPacket sendPacket, int srcPort, int recvLength,
      int timeout, InetAddress addr, UdpBroadcastHandlerInterface handler) throws IOException {
    List<InetAddress> addresses = new ArrayList<>(1);

    addresses.add(addr);

    sendRecvBroadcast(sendPacket, srcPort, recvLength, timeout, addresses, handler);
  }

  /**
   * Send broadcast packets from a list of source addresses and receive broadcast answers in a
   * platform-independant manner.
   *
   * @param sendPacket Packet to broadcast. The destination address is set to 255.255.255.255
   *        automatically. If this is null, no packet is sent.
   * @param srcPort Source port of outbound broadcasts and used for listening for broadcasts.
   * @param recvLength Length of broadcast responses. This will be the length of returned packets'
   *        data buffers. If less than or equal 0, do not try to receive any packets.
   * @param timeout Time to wait for responses in milliseconds.
   * @param addresses Addresses to bind to when broadcasting. This effectively determines the
   *        interfaces that sendPacket will be broadcast on.
   * @param handler A functor object for handling received broadcast responses/packets.
   *
   * @throws IOException if an unexpected I/O error occured.
   */
  public static void sendRecvBroadcast(DatagramPacket sendPacket, int srcPort, int recvLength,
      int timeout, List<InetAddress> addresses, UdpBroadcastHandlerInterface handler)
      throws IOException {
    if (isWindows()) {
      sendBroadcastRecvOnUnicast(sendPacket, srcPort, recvLength, timeout, addresses, handler);
    } else if (isMac()) {
      sendBroadcastRecvOnAny(sendPacket, srcPort, recvLength, timeout, addresses, handler);
    } else {
      sendBroadcastRecvOnBroadcast(sendPacket, srcPort, recvLength, timeout, addresses, handler);
    }
  }

  /**
   * Send UDP broadcasts on all local network interfaces and return all responding Lantronix
   * devices.
   *
   * @param timeout Time to wait for responses in milliseconds.
   *
   * @return List of Lantronix devices. It is already sorted.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getLantronixEthernetDevices(int timeout) throws IOException {
    List<InetAddress> addresses = getAllInterfaceInetAddresses();

    return getLantronixEthernetDevices(timeout, addresses);
  }

  /**
   * Send UDP broadcast from a specific IP address and return all responding Lantronix devices.
   *
   * @param timeout Time to wait for responses in milliseconds.
   * @param addr Address to bind to, i.e. address of the interface to send broadcast on.
   *
   * @return List of Lantronix devices. It is already sorted.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getLantronixEthernetDevices(int timeout, InetAddress addr)
      throws IOException {
    List<InetAddress> addresses = new ArrayList<>(1);

    addresses.add(addr);

    return getLantronixEthernetDevices(timeout, addresses);
  }

  /**
   * Send UDP broadcasts from a specified list of IP addresses and return all responding Lantronix
   * devices.
   *
   * @param timeout Time to wait for responses in milliseconds.
   * @param addresses Addresses to bind to when broadcasting. This effectively determines the
   *        interfaces that sendPacket will be broadcast on.
   *
   * @return List of Lantronix devices. It is already sorted.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getLantronixEthernetDevices(final int timeout,
      List<InetAddress> addresses) throws IOException {
    /*
     * For checking duplicate responses. This might happen if we have multiple interfaces in the
     * same subnet. Since elements in a TreeSet have a natural order, lists constructed from this
     * set will automatically be sorted as well.
     */
    final Set<EthernetDevice> deviceSet = new TreeSet<>();

    byte[] sendData = {0x00, 0x00, 0x00, (byte) 0xF6};
    DatagramPacket sendPacket;

    sendPacket = new DatagramPacket(sendData, sendData.length, BROADCAST_ADDR, 30718);

    /*
     * Lantronix devices answer to our unicast address, in contrast to TUCs that answer by
     * broadcasting. Thus we cannot use sendRecvBroadcast() as it is implemented by
     * sendBroadcastRecvOnBroadcast() on Linux and cannot always receive unicast responses.
     */
    sendBroadcastRecvOnUnicast(sendPacket, 41000, 30, timeout, addresses,
        new UdpBroadcastHandlerInterface() {
          @Override
          public boolean handle(InetAddress localAddress, DatagramPacket recvPacket,
              DatagramSocket socket) throws IOException {
            EthernetDevice dev = new EthernetDevice();

            InetAddress srcAddr = recvPacket.getAddress();
            byte[] recvData = recvPacket.getData();

            if (recvData[0] != 0x00 || recvData[1] != 0x00 || recvData[2] != 0x00
                || recvData[3] != (byte) 0xF7) {
              return true; /* skip this one */
            }

            // get Mac Address
            StringBuffer mac = new StringBuffer();
            for (int i = 24; i < 30; i++) {
              mac.append(String.format(":%02X", recvData[i]));
            }

            dev.setMACAddress(mac.toString().substring(1));
            dev.setIPAddress(srcAddr.getHostAddress());
            dev.setDeviceName(srcAddr.getHostAddress());
            dev.setReachable(srcAddr.isReachable(timeout));

            deviceSet.add(dev);
            return true;
          }
        });

    return new ArrayList<>(deviceSet);
  }

  /**
   * Send UDP broadcasts on all local network interfaces and return all responding metraTec TUC
   * devices.
   *
   * @param udpTimeout Time to wait for responses in milliseconds.
   *
   * @return List of TUC devices. It is sorted already.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getMetratecEthernetDevices(int udpTimeout) throws IOException {
    List<InetAddress> addresses = getAllInterfaceInetAddresses();
    return getMetratecEthernetDevices(udpTimeout, addresses);
  }

  /**
   * Send UDP and UPNP broadcast from a specific IP address and return all responding metraTec TUC devices.
   *
   * @param timeout Time to wait for udp and upnp responses in milliseconds.
   * @param addr Address to bind to, i.e. address of the interface to send broadcast on.
   *
   * @return List of TUC devices. It is sorted already.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getMetratecEthernetDevices(int timeout, InetAddress addr)
      throws IOException {
    List<InetAddress> addresses = new ArrayList<>(1);
    addresses.add(addr);
    return getMetratecEthernetDevices(timeout, addresses);
  }

   /**
   * Send Upd broadcasts on all local network interfaces and return all responding metraTec TUC
   * devices.
   *
   * @param timeout Time to wait for responses in milliseconds.
   *
   * @return List of TUC devices. It is sorted already.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getMetratecEthernetDevicesUdp(int timeout) throws IOException {
    List<InetAddress> addresses = getAllInterfaceInetAddresses();
    return getMetratecEthernetDevices(timeout, addresses);
  }

  /**
   * Send UDP from a specified list of IP addresses and return all responding metraTec
   * TUC devices.
   *
   * @param timeout Time to wait for udp and upnp responses in milliseconds.
   * @param addresses Addresses to bind to when broadcasting. This effectively determines the
   *        interfaces that sendPacket will be broadcast on.
   *
   * @return List of TUC devices. It is sorted already.
   *
   * @throws IOException if an unexpected I/O error occurs.
   */
  public static List<EthernetDevice> getMetratecEthernetDevices(final int timeout,
      List<InetAddress> addresses) throws IOException {
    /*
     * For checking duplicate responses. This might happen if we have multiple interfaces in the
     * same subnet. Since elements in a TreeSet have a natural order, lists constructed from this
     * set will automatically be sorted as well.
     */
    final Set<EthernetDevice> deviceSet = new TreeSet<>();
    
    byte[] sendData = new byte[52];
    DatagramPacket sendPacket;

    sendData[0] = 0x01;
    sendData[1] = 0x13;
    sendData[2] = 0x37;
    sendData[3] = 0x0A;
    sendData[4] = (byte) 0xFF;
    sendData[5] = (byte) 0xFF;
    sendData[6] = (byte) 0xFF;
    sendData[7] = (byte) 0xFF;
    sendData[8] = (byte) 0xFF;
    sendData[9] = (byte) 0xFF;
    sendData[16] = 0x01;

    sendPacket = new DatagramPacket(sendData, sendData.length, BROADCAST_ADDR, 42000);
    sendRecvBroadcast(sendPacket, 41000, 52, timeout, addresses,
        new UdpBroadcastHandlerInterface() {
          @Override
          public boolean handle(InetAddress localAddr, DatagramPacket recvPacket,
              DatagramSocket socket) throws IOException {
            EthernetDevice dev = new EthernetDevice();

            InetAddress srcAddr = recvPacket.getAddress();
            byte[] recvData = recvPacket.getData();

            // get Mac Address
            StringBuffer mac = new StringBuffer();
            for (int i = 10; i < 16; i++) {
              mac.append(String.format(":%02X", recvData[i]));
            }

            dev.setMACAddress(mac.toString().substring(1));
            dev.setIPAddress(srcAddr.getHostAddress());
            dev.setDeviceName(
                new String(recvData, 17, 49 - 17, Charset.forName("ISO-8859-1")).trim());
            dev.setReachable(srcAddr.isReachable(timeout));
            deviceSet.add(dev);
            return true;
          }
        });
    return new ArrayList<>(deviceSet);
  }

  private static String toHexString(byte data[]) {
    StringBuilder strbuf = new StringBuilder(data.length * 2);
    for (int i = 0; i < data.length; i++) {
      if ((data[i] & 0xFF) < 0x10)
        strbuf.append('0');
      strbuf.append(Integer.toHexString(data[i] & 0xFF).toUpperCase());
    }
    return strbuf.toString();
  }
}
