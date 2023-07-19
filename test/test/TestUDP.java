/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.io.IOException;
import java.util.List;

import com.metratec.lib.connection.EthernetDevice;
import com.metratec.lib.connection.UdpConnection;

@SuppressWarnings("javadoc")
public class TestUDP {

  public static void main(String[] args) {
    // try
    // {
    // ArrayList<String> data = UDPConnection.sendBroadcastLantronix();
    // System.out.println(data.size());
    // for(String s:data)
    // {
    // System.out.println(s);
    // }
    // }
    // catch (IOException e1)
    // {
    // e1.printStackTrace();
    // }

    try {
      List<EthernetDevice> devices = UdpConnection.getMetratecEthernetDevices(2000);
      if (devices.size() == 0) {
        System.out.println("no device found");
      }
      for (int i = 0; i < devices.size(); i++) {
        System.out.println(i);
        System.out.println("Devicename: " + devices.get(i).getDeviceName());
        System.out.println("IP Address: " + devices.get(i).getIPAddress());
        System.out.println("Mac Address: " + devices.get(i).getMACAddress());
        System.out.println("Reachable: " + devices.get(i).isReachable());

        // try
        // {
        // if(UDPConnection.configMetratecEthernetDeviceDHCP("00:50:C2:DA:20:00"))
        // {
        // System.out.println("OK");
        // }
        // else
        // {
        // System.out.println("NOK");
        // }
        // }
        // catch (CommConnectionException e)
        // {
        // e.printStackTrace();
        // }

        // try
        // {
        // if(UDPConnection.configMetratecEthernetDeviceStatic(
        // devices.get(i).getMACAddress(),
        // "192.168.2.61",
        // "255.255.255.0",
        // "0.0.0.0"))
        // {
        // System.out.println("OK");
        // }
        // else
        // {
        // System.out.println("NOK");
        // }
        // }
        // catch (CommConnectionException e)
        // {
        // e.printStackTrace();
        // }


      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
