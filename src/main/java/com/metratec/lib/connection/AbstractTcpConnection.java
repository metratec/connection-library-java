package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents tcp connections
 *
 * @author man
 */
public abstract class AbstractTcpConnection extends ICommConnection {

  private final Logger logger = LoggerFactory.getLogger(AbstractTcpConnection.class);
  private Socket socket = null;
  private String ipAddress = null;
  private int port = 0;
  /** output stream */
  private OutputStream outputstream = null;
  /** output stream */
  private InputStream inputstream = null;
  // protected boolean _init = false;
  private int recvTimeout = 2000;
  private int connectTimeout = 1000;
  private boolean isServerConnection = false;

  /**
   * Construct a new instance, with the given parameters.
   *
   * @param ip Device IP address
   *
   * @param port Device port
   */
  protected AbstractTcpConnection(String ip, int port) {
    this.ipAddress = ip;
    this.port = port;
  }

  /**
   * @param socket the new socket
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public AbstractTcpConnection(Socket socket) throws CommConnectionException {
    this(socket, false);
  }

  /**
   * @param socket the new socket
   * @param isServerConnection set to true if the connection is created by accepting a client - so
   *        no reconnect is available
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public AbstractTcpConnection(Socket socket, boolean isServerConnection)
      throws CommConnectionException {
    if (!socket.isConnected()) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, "Socket is not connected");
    }
    this.socket = socket;
    this.isServerConnection = isServerConnection;
    ipAddress = socket.getInetAddress().getHostAddress();
    port = socket.getPort();
    try {
      initializeSocket();
    } catch (IOException e) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, e.getMessage());
    }
  }

  @Override
  public void disconnect() throws CommConnectionException {
    if (inputstream != null) {
      try {
        try {
          socket.shutdownOutput();
        } catch (IOException | UnsupportedOperationException e1) {
          logger.trace("{} output shutdown warning - {}", this.toString(), e1.getMessage());
        }
        try {
          socket.shutdownInput();
        } catch (IOException | UnsupportedOperationException e1) {
          logger.trace("{} input shutdown warning - {}", this.toString(), e1.getMessage());
        }
        inputstream = null;
        outputstream = null;
        socket.close();
      } catch (IOException e) {
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      }
    }
  }

  /**
   * @param ipAddress ip address
   * @param port port
   * @return the new socket
   * @throws IOException if an error occurs
   */
  protected abstract Socket createNewSocket(String ipAddress, int port) throws IOException;

  /**
   * @return the isServerConnection
   */
  public boolean isServerConnection() {
    return isServerConnection;
  }

  @Override
  public void connect() throws CommConnectionException {
    if (isConnected()) {
      return;
    }
    if (isServerConnection) {
      throw new CommConnectionException(ICommConnection.NOT_AVAILABLE,
          "Connection was inialized by the client");
    }
    if (port == 0 || ipAddress == null) {
      throw new CommConnectionException(WRONG_PARAMETER, "ip-address and/or port is not set");
    }
    try {
      socket = createNewSocket(ipAddress, port);
      initializeSocket();

    } catch (UnknownHostException e) {
      throw new CommConnectionException(ETHERNET_UNKNOWN_HOST, e.getMessage());
    } catch (IOException e) {
      throw new CommConnectionException(ETHERNET_TIMEOUT, e.getMessage());
    }
  }

  private void initializeSocket() throws IOException {
    socket.setSoTimeout(recvTimeout);
    outputstream = new BufferedOutputStream(socket.getOutputStream());
    inputstream = new BufferedInputStream(socket.getInputStream());
  }

  /**
   * sets the used IP address
   *
   * @param ipaddress IP address like "192.168.1.1"
   */
  public void setIPAddress(String ipaddress) {
    ipAddress = ipaddress;
  }

  /**
   *
   * @return the used IP address
   */
  public String getIPAddress() {
    return ipAddress;
  }

  /**
   * sets the used port
   *
   * @param port port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   *
   * @return the used port
   */
  public int getPort() {
    return port;
  }

  @Override
  public boolean isConnected() {
    if (null == socket || socket.isClosed()) {
      return false;
    } else {
      return socket.isConnected();
    }
  }

  @Override
  public int recv() throws CommConnectionException {
    try {
      int c = inputstream.read();
      if (c < 0) {
        try {
          disconnect();
        } catch (CommConnectionException e) {
          if (logger.isDebugEnabled()) {
            String message = this.toString() + " error disconnect " + e.getMessage();
            if (logger.isTraceEnabled()) {
              logger.trace(message, e);
            } else {
              logger.debug(message);
            }
          }
        }
        throw new CommConnectionException(CONNECTION_LOST, "socket closed");
      }
      return c;
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (SocketTimeoutException e) {
      // means no Data Available
      return -1;
    } catch (IOException e) {
      try {
        disconnect();
      } catch (CommConnectionException e1) {
        if (logger.isDebugEnabled()) {
          String message = this.toString() + " error disconnect " + e.getMessage();
          if (logger.isTraceEnabled()) {
            logger.trace(message, e);
          } else {
            logger.debug(message);
          }
        }
      }
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      // if (e.getMessage().contains("Connection reset")
      // || e.getMessage().contains("connection abort")) {
      // try {
      // disconnect();
      // } catch (CommConnectionException e1) {
      // e1.printStackTrace();
      // }
      // throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      // } else {
      // throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      // }
    }
  }

  @Override
  public void setRecvTimeout(int timeout) throws CommConnectionException {
    recvTimeout = timeout;
    if (isConnected()) {
      try {
        socket.setSoTimeout(recvTimeout);
      } catch (SocketException e) {
        throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
      }
    }
  }

  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    try {
      outputstream.write(senddata);
      outputstream.flush();
    } catch (NullPointerException e) {
      if (senddata == null) {
        throw new CommConnectionException(WRONG_PARAMETER, "data are null");
      } else {
        throw new CommConnectionException(NOT_INITIALISED, "not initialize");
      }
    } catch (IOException e) {
      try {
        disconnect();
      } catch (CommConnectionException e1) {
        if (logger.isDebugEnabled()) {
          String message = this.toString() + " error disconnect " + e.getMessage();
          if (logger.isTraceEnabled()) {
            logger.trace(message, e);
          } else {
            logger.debug(message);
          }
        }
      }
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      // if (e.getMessage().contains("Connection reset")) {
      // try {
      // disconnect();
      // } catch (CommConnectionException e1) {
      // e1.printStackTrace();
      // }
      // throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      // } else {
      // throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      // }
    }
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    try {
      return inputstream.available();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
    }
  }

  @Override
  public InputStream getInputStream() {
    return inputstream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputstream;
  }

  /**
   * @return the {@link Socket}
   */
  public Socket getSocket() {
    return socket;
  }

  @Override
  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<>();
    info.put("type", "ethernet");
    info.put("ip", ipAddress);
    info.put("port", port);
    return info;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) throws IllegalArgumentException {

    String port = settings.get("port");
    if (null != port) {
      try {
        this.port = Integer.parseInt(port);
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Port should be a number");
      }
    }
    String ip = settings.get("ip");
    if (null != ip) {
      this.ipAddress = ip;
    }

  }

  @Override
  public int getRecvTimeout() {
    return recvTimeout;
  }

  /**
   * Test if a Ethernet Device is reachable.
   *
   * @param ipAdress Device IP Address
   *
   * @param time Timeout
   *
   * @return true if reachable, else false
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>ETHERNET_UNKNOWN_HOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   */
  public static boolean isAlive(String ipAdress, int time) throws CommConnectionException {
    try {
      return InetAddress.getByName(ipAdress).isReachable(time);
    } catch (UnknownHostException e) {
      throw new CommConnectionException(ETHERNET_UNKNOWN_HOST, e.getMessage());
    } catch (IOException e) {
      throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  /**
   * Test if the Ethernet Device is reachable.
   *
   * @param time Timeout
   *
   * @return true if reachable, else false
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>ETHERNET_UNKNOWN_HOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   */

  public boolean isAlive(int time) throws CommConnectionException {
    return isAlive(ipAddress, time);
  }

  @Override
  public void setConnectionTimeout(int time) {
    connectTimeout = time;
  }

  @Override
  public int getConnectionTimeout() {
    return connectTimeout;
  }

}
