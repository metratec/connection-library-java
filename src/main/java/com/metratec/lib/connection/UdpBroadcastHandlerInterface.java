package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Interface for creating functor objects for {@link UdpConnection#sendRecvBroadcast}.
 */
public interface UdpBroadcastHandlerInterface {

  /**
   * Handle a broadcast received.
   *
   * This may be any broadcast or only broadcasts on the local interface addresses passed to
   * {@link UdpConnection#sendRecvBroadcast}.
   *
   * @param localAddress Address of the interface that received the broadcast.
   *
   * @param packet Broadcast packet.
   *
   * @param socket Socket that may be used to send broadcasts back to the client that sent "packet".
   *
   * @return true if the server should continue to run, false if it should terminate.
   *
   * @exception IOException if an I/O error occured.
   */
  boolean handle(InetAddress localAddress, DatagramPacket packet, DatagramSocket socket)
      throws IOException;
}
