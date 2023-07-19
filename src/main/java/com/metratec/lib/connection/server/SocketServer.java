package com.metratec.lib.connection.server;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.ICommConnection;
import com.metratec.lib.connection.TcpConnection;

/**
 * @author man
 *
 */
public class SocketServer extends AbstractSocketServer {

  @Override
  protected ServerSocket createServerSocket(int serverPort) throws CommConnectionException {
    try {
      return new ServerSocket(serverPort);
    } catch (IOException e) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, e.getMessage());
    }
  }

  @Override
  protected ServerSocket createServerSocket(int serverPort, int backlog, InetAddress bindAddr)
      throws CommConnectionException {
    try {
      return new ServerSocket(serverPort, backlog, bindAddr);
    } catch (IOException e) {
      throw new CommConnectionException(ICommConnection.NOT_INITIALISED, e.getMessage());
    }
  }

  @Override
  protected ICommConnection createClientCommConnection(Socket socket) throws CommConnectionException {
    return new TcpConnection(socket, true);
  }


}
