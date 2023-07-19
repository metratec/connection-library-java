package com.metratec.lib.connection.server;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;

import com.metratec.lib.connection.ICommConnection;

/**
 * @author man
 *
 */
public interface ClientConnectionListener {
  /**
   * Called if a new client has connected
   * 
   * @param connection the new {@link ICommConnection}
   */
  void clientConnected(ICommConnection connection);

  /**
   * @param e the server error {@link IOException}
   */
  void serverError(IOException e);
}
