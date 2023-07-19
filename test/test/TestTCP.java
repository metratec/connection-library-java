/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.TcpConnection;

@SuppressWarnings("javadoc")
public class TestTCP {

  /**
   * @param args
   */
  public static void main(String[] args) {
    TcpConnection tcp = new TcpConnection("192.168.2.239", 10001);
    try {
      tcp.connect();
      System.out.println(tcp.isConnected());
      tcp.disconnect();
      System.out.println(tcp.isConnected());
      tcp.connect();
      System.out.println(tcp.isConnected());
      for (int i = 0; i < 10; i++) {
        // System.out.println("isConnected: "+tcp.getSocket().isConnected());
        // System.out.println("isInputShutdown: "+tcp.getSocket().isInputShutdown());
        // System.out.println("isOutputShutdown: "+tcp.getSocket().isOutputShutdown());
        // System.out.println("isBound: "+tcp.getSocket().isBound());
        System.out.println("available: " + tcp.getInputStream().available());
        try {
          System.out.println("read: " + tcp.getInputStream().read());
        } catch (SocketTimeoutException e) {
          System.out.println("SocketTimeoutException " + e.getMessage());
        }
        Thread.sleep(1000);

      }
      tcp.disconnect();
      System.out.println(tcp.isConnected());

    } catch (CommConnectionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
