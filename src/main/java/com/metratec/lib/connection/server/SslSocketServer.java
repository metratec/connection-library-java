package com.metratec.lib.connection.server;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.ICommConnection;
import com.metratec.lib.connection.SslTcpConnection;

/**
 * @author man
 *
 */
public class SslSocketServer extends AbstractSocketServer {
  /**
   * @param keyStoreFile key store file
   */
  public static void setKeyStoreFile(String keyStoreFile) {
    System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
  }

  /**
   * @param keyStorePassword key store password
   */
  public static void setKeyStorePassword(String keyStorePassword) {
    System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
  }

  @Override
  protected ServerSocket createServerSocket(int serverPort) throws CommConnectionException {
    SSLServerSocketFactory sslServerSocketFactory =
        (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    try {
      return sslServerSocketFactory.createServerSocket(serverPort);
    } catch (IOException e) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, e.getMessage());
    }
  }

  @Override
  protected ServerSocket createServerSocket(int serverPort, int backlog, InetAddress bindAddr)
      throws CommConnectionException {
    SSLServerSocketFactory sslServerSocketFactory =
        (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    try {
      return sslServerSocketFactory.createServerSocket(serverPort, backlog, bindAddr);
    } catch (IOException e) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, e.getMessage());
    }
  }

  @Override
  protected ICommConnection createClientCommConnection(Socket socket) throws CommConnectionException {
    if (socket instanceof SSLSocket) {
      return new SslTcpConnection((SSLSocket) socket, true);
    }
    throw new CommConnectionException(ICommConnection.WRONG_PARAMETER,
        "Socket is not an instance of SSLSocket");
  }

}
