package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a ssl tcp port<br>
 * <br>
 * <u>Notes:</u> <i>{@link InputStream#available()} allways return 0 !</i>
 *
 * @author man
 */
public class SslTcpConnection extends AbstractTcpConnection {
  private final Logger logger = LoggerFactory.getLogger(SslTcpConnection.class);

  /**
   * @param trustStoreFile trust store file
   */
  public static void setTrustStoreFile(String trustStoreFile) {
    System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
  }

  /**
   * @param trustStorePassword trust store password
   */
  public static void setTrustStorePassword(String trustStorePassword) {
    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
  }

  /**
   * Construct a new SslSocketConnection class, with the given parameters.
   *
   * @param ip Device IP address
   * @param port Device port
   */
  public SslTcpConnection(String ip, int port) {
    super(ip, port);
  }

  /**
   * @param socket {@link SSLSocket}
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public SslTcpConnection(SSLSocket socket) throws CommConnectionException {
    super(socket);
  }


  /**
   * @param socket the new @link SSLSocket}
   * @param isServerConnection set to true if the connection is created by accepting a client - so
   *        no reconnect is available
   * @throws CommConnectionException throwed with error code {@link ICommConnection#NOT_INITIALISED}
   *         if the given socket is not connected
   */
  public SslTcpConnection(SSLSocket socket, boolean isServerConnection)
      throws CommConnectionException {
    super(socket, isServerConnection);
  }

  @Override
  protected SSLSocket createNewSocket(String ipAddress, int port)
      throws UnknownHostException, IOException {
    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket socket = (SSLSocket) sslsocketfactory.createSocket();
    socket.connect(new InetSocketAddress(ipAddress, port), getConnectionTimeout());
    socket.setSoTimeout(getConnectionTimeout());
    socket.startHandshake();
    return socket;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getIPAddress()).append(':').append(getPort()).toString();
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

  private Logger getLogger() {
    return logger;
  }

}
