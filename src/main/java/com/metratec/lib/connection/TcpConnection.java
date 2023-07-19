package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a tcp port
 *
 * @author man
 */
public class TcpConnection extends AbstractTcpConnection {

  private final Logger logger = LoggerFactory.getLogger(TcpConnection.class);

  /**
   * Construct a new TCPIPConnection class, with the given parameters.
   *
   * @param ip Device IP address
   *
   * @param port Device port
   */
  public TcpConnection(String ip, int port) {
    super(ip, port);
  }

  /**
   * @param socket {@link Socket}
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public TcpConnection(Socket socket) throws CommConnectionException {
    super(socket);
  }

  /**
   * @param socket the new socket
   * @param isServerConnection set to true if the connection is created by accepting a client - so
   *        no reconnect is available
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public TcpConnection(Socket socket, boolean isServerConnection) throws CommConnectionException {
    super(socket, isServerConnection);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.metratec.lib.connection.ICommConnection#recv(byte[], int, int)
   */
  @Override
  public void recv(byte[] b, int off, int len) throws CommConnectionException {
    try {
      super.recv(b, off, len);
    } finally {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("{} recv {}", toString(), new String(b));
      }
    }
  }

  /* (non-Javadoc)
   * @see com.metratec.lib.connection.ICommConnection#send(java.lang.String)
   */
  @Override
  public void send(String senddata) throws CommConnectionException {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} send {}", toString(), senddata.toString());
    }
    super.send(senddata);
  }
  /*
   * (non-Javadoc)
   * 
   * @see com.metratec.lib.connection.ICommConnection#receive(int[])
   */
  @Override
  public StringBuilder receive(int... terminators) throws CommConnectionException {
    StringBuilder s = super.receive(terminators);
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} recv {}", toString(), s.toString());
    }
    return s;
  }

  @Override
  protected Socket createNewSocket(String ipAddress, int port) throws IOException {
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(ipAddress, port), getConnectionTimeout());
    return socket;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getIPAddress()).append(':').append(getPort()).toString();
  }


  private Logger getLogger() {
    return logger;
  }
}
