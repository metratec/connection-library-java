package com.metratec.lib.connection;

import java.io.Serializable;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

/**
 * Class for an ethernet device
 *
 * @author Matthias Neumann (neumann@metratec.com)
 */
public class EthernetDevice implements Serializable, Comparable<EthernetDevice> {
  /**
   * 
   */
  private static final long serialVersionUID = 1706679564211621882L;
  /**
   * the IP address of the device
   */
  private String ipAddress = null;
  /**
   * the MAC address of the device
   */
  private String macAddress = null;
  /**
   * the device name of the device
   */
  private String deviceName = null;
  /**
   * reachable flag
   */
  private boolean isReachable = false;

  /**
   * Constructs a new Ethernet device.
   */
  public EthernetDevice() {}

  /**
   * Constructs a new Ethernet device with the specific parameters.
   *
   * @param ipAddress the IP address of the device, can read with the {@link #getIPAddress()}
   *        method.
   *
   * @param macAddress the MAC address of the device, can read with the {@link #getMACAddress()}
   *        method.
   *
   * @param deviceName the device name, can read with the {@link #getDeviceName()} method.
   *
   * @param isReachable reachable flag, can read with the {@link #isReachable()} method.
   */
  public EthernetDevice(String ipAddress, String macAddress, String deviceName,
      boolean isReachable) {
    this.ipAddress = ipAddress;
    this.macAddress = macAddress;
    this.deviceName = deviceName;
    this.isReachable = isReachable;
  }

  /**
   * @return the IP Address
   */
  public String getIPAddress() {
    return ipAddress;
  }

  /**
   * @return MAC Address
   */
  public String getMACAddress() {
    return macAddress;
  }

  /**
   * @return the device name
   */
  public String getDeviceName() {
    return deviceName;
  }

  /**
   * @return true if the Ethernet device is reachable (is in a reachable subnet), else false
   */
  public boolean isReachable() {
    return isReachable;
  }

  /**
   * set the IP Address
   *
   * @param ipAddress IP Address
   */
  public void setIPAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * set the MAC Address
   *
   * @param macAddress MAC Address
   */
  public void setMACAddress(String macAddress) {
    this.macAddress = macAddress;
  }

  /**
   * set the device name
   *
   * @param deviceName device name
   */
  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  /**
   * set the reachable flag
   *
   * @param isReachable true if the Ethernet device is reachable (is in a reachable subnet), else
   *        false
   */
  public void setReachable(boolean isReachable) {
    this.isReachable = isReachable;
  }

  /**
   * Null-safe string comparison ({@link String#compareTo(String)} isn't null-safe). Null values are
   * treated like empty strings.
   *
   * @param str1 One string, can be null
   * @param str2 Another string, can be null
   * @return 0 if str1 is equal to str2; less than 0 if str1 is lexicographically less than str2;
   *         otherwise greater than 0.
   */
  private static int stringCompare(String str1, String str2) {
    return (str1 == null ? "" : str1).compareTo(str2 == null ? "" : str2);
  }

  /**
   * Compare this device to other device.
   *
   * The primary key is the device IP address and the secondary key is the MAC address, i.e. they
   * are only considered equal if both the IP and MAC are the same. Addresses are currently compared
   * like strings since the original design of EthernetDevice is not type-safe. IP and MAC addresses
   * can be null.
   *
   * @param obj Device to compare to.
   * @return Negative value if this device is "less than" obj, 0 if this device "equals" obj,
   *         positive value if this device is "greater than" obj.
   */
  @Override
  public int compareTo(EthernetDevice obj) {
    /*
     * NOTE: Perhaps counter-intuitively, this does not always return 0 if the MAC addresses are
     * equal. E.g. if only one of the IP addresses is null, they are not considered equal. In
     * practice, this should be an issue since both the IP and MAC addresses will be set in objects
     * created by this library.
     */
    int cmp = stringCompare(ipAddress, obj.ipAddress);
    return cmp == 0 ? stringCompare(macAddress, obj.macAddress) : cmp;
  }

  /**
   * Compare to another ethernet device.
   *
   * They are considered equal if they have the same IP and MAC addresses. More precisely, the
   * definition of equivalence is the same as for the {@link #compareTo(EthernetDevice)} method.
   *
   * @param obj Ethernet device to compare.
   *
   * @return True if the objects are equal, otherwise false.
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof EthernetDevice && compareTo((EthernetDevice) obj) == 0;
  }

}
