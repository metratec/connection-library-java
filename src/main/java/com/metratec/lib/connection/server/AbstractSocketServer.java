package com.metratec.lib.connection.server;

/*******************************************************************************
 * Copyright (c) 2023 metraTec.com
 *
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.ICommConnection;

/**
 * @author man
 *
 */
public abstract class AbstractSocketServer {
  private AcceptNewConnectionsThread acceptNewConnectionsThread;
  private final Logger logger = LoggerFactory.getLogger(AbstractSocketServer.class);

  /**
   * 
   */
  public AbstractSocketServer() {}

  /**
   * @return true if the server is waiting for new client
   */
  public boolean isRunning() {
    return null != acceptNewConnectionsThread && acceptNewConnectionsThread.isAlive();
  }


  /**
   * Start the waiting for new client to connect
   * 
   * @param serverPort the port number, or {@code 0} to use a port number that is automatically
   *        allocated.
   * @param backlog requested maximum length of the queue of incoming connections.
   * @param bindAddr the local InetAddress the server will bind to socket
   * @param listener the {@link ClientConnectionListener}
   * @throws CommConnectionException throwed if the server can not be created
   */
  public void start(int serverPort, int backlog, InetAddress bindAddr,
      ClientConnectionListener listener) throws CommConnectionException {
    if (isRunning()) {
      return;
    }
    acceptNewConnectionsThread =
        new AcceptNewConnectionsThread(createServerSocket(serverPort, backlog, bindAddr), listener);
    acceptNewConnectionsThread.start();
  }

  /**
   * Start the waiting for new client to connect
   * 
   * @param serverPort server port
   * @param listener the {@link ClientConnectionListener}
   * @throws CommConnectionException throwed if the server can not be created
   */
  public void start(int serverPort, ClientConnectionListener listener)
      throws CommConnectionException {
    if (isRunning()) {
      return;
    }
    acceptNewConnectionsThread =
        new AcceptNewConnectionsThread(createServerSocket(serverPort), listener);
    acceptNewConnectionsThread.start();
  }

  /**
   * Called to create the new {@link ServerSocket}
   * 
   * @param serverPort server port
   * @return the created server socket
   * @throws CommConnectionException throwed if the server can not be created
   */
  protected abstract ServerSocket createServerSocket(int serverPort) throws CommConnectionException;

  /**
   * Called to create the new {@link ServerSocket}
   * 
   * @param serverPort the port number, or {@code 0} to use a port number that is automatically
   *        allocated.
   * @param backlog requested maximum length of the queue of incoming connections.
   * @param bindAddr the local InetAddress the server will bind to* @return the created server
   *        socket
   * @return the created server socket
   * @throws CommConnectionException throwed if the server can not be created
   */
  protected abstract ServerSocket createServerSocket(int serverPort, int backlog,
      InetAddress bindAddr) throws CommConnectionException;

  /**
   * Called if a new client want connect
   * 
   * @param socket the client socket
   * @return the new {@link ICommConnection}
   * @throws CommConnectionException if an error occurs
   */
  protected abstract ICommConnection createClientCommConnection(Socket socket)
      throws CommConnectionException;

  /**
   * stop the server
   */
  public void stop() {
    if (null == acceptNewConnectionsThread) {
      return;
    }
    acceptNewConnectionsThread.halt();
    waitForShutDown();
  }

  private void waitForShutDown() {
    if (null != acceptNewConnectionsThread && acceptNewConnectionsThread.isAlive()) {
      while (acceptNewConnectionsThread.isAlive()) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  /**
   * @return the current using server port or -1 if the server not started
   */
  public int getPort() {
    if (isRunning()) {
      return acceptNewConnectionsThread.getPort();
    } else {
      return -1;
    }
  }


  /**
   * @author man
   *
   */
  private class AcceptNewConnectionsThread extends Thread {
    private int port;
    private ClientConnectionListener listener;
    private boolean isRunning;
    private ServerSocket serverSocket;


    /**
     * @param serverSocket the server socket
     * @param listener the {@link ClientConnectionListener}
     */
    AcceptNewConnectionsThread(ServerSocket serverSocket, ClientConnectionListener listener) {
      this.serverSocket = serverSocket;
      this.listener = listener;
    }

    @Override
    public void run() {
      isRunning = true;

      while (isRunning) {
        try {
          Socket socket = serverSocket.accept();
          try {
            callListener(createClientCommConnection(socket));
          } catch (CommConnectionException e) {
            // connection error - disconnect the client
            if (logger.isDebugEnabled()) {
              String message = serverSocket + " error create client connection " + e.getMessage();
              if (logger.isTraceEnabled()) {
                logger.trace(message, e);
              } else {
                logger.debug(message);
              }
            }
            socket.close();
          }
        } catch (IOException e) {
          String message = serverSocket + " io error " + e.getMessage();
          if (logger.isDebugEnabled()) {
            if (logger.isTraceEnabled()) {
              logger.trace(message, e);
            } else {
              logger.debug(message);
            }
          }
          if (isRunning) {
            if (serverSocket.isClosed()) {
              isRunning = false;
            }
            listener.serverError(e);
          }
          // else {
          // //program stopped
          // }
        }
      }
    }

    private void callListener(final ICommConnection connection) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            listener.clientConnected(connection);
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              String message =
                  serverSocket + " error call client connection listener " + e.getMessage();
              if (logger.isTraceEnabled()) {
                logger.trace(message, e);
              } else {
                logger.debug(message);
              }
            }
          }
        }
      }).start();
    }

    /**
     * @return the current server port
     */
    public int getPort() {
      return port;
    }

    /**
     * stop the waiting for new clients
     */
    public void halt() {
      isRunning = false;
      try {
        serverSocket.close();
      } catch (IOException e) {
        if (logger.isDebugEnabled()) {
          String message = serverSocket + " error close socket " + e.getMessage();
          if (logger.isTraceEnabled()) {
            logger.trace(message, e);
          } else {
            logger.debug(message);
          }
        }
      }
    }
  }

}
