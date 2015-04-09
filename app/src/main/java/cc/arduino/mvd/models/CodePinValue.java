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

import java.io.Serializable;

/**
 * @author Andreas Goransson, 2015-04-05
 */
public class CodePinValue implements Serializable {

  private String code;

  private String pin;

  private String value;

  public CodePinValue(String code, String pin, String value) {
    this.code = code;
    this.pin = pin;
    this.value = value;
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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean equals(CodePinValue codePinValue) {
    return (codePinValue.getPin().equals(code) && codePinValue.getPin().equals(pin) && codePinValue.getValue().equals(value));
  }

  @Override
  public String toString() {
    return "CodePinValue{" +
        "code='" + code + '\'' +
        ", pin='" + pin + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
