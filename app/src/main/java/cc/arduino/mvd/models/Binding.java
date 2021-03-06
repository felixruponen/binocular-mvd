/*
 * Copyright 2015 Arduino Verkstad AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.arduino.mvd.models;

import com.orm.SugarRecord;

/**
 * @author Andreas Goransson, 2015-03-21
 */
public class Binding extends SugarRecord<Binding> {

  public String mac;

  public String name;

  public String service;

  public String code;

  public String pin;

  public Binding() {
  }

  public Binding(String mac, String name, String service, String code, String pin) {
    this.mac = mac;
    this.name = name;
    this.service = service;
    this.code = code;
    this.pin = pin;
  }

  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getPin() {
    return pin;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  @Override
  public String toString() {
    return name + " : " + code + "/" + pin + " --> " + service;
  }
}