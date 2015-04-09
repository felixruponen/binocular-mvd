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
 * @author Andreas Goransson, 2015-04-04
 */
public class ServiceRoute extends SugarRecord<ServiceRoute> {

  private String service1;

  private String service2;

  public ServiceRoute() {
  }

  public ServiceRoute(String service1, String service2) {
    this.service1 = service1;
    this.service2 = service2;
  }

  public String getService1() {
    return service1;
  }

  public void setService1(String service1) {
    this.service1 = service1;
  }

  public String getService2() {
    return service2;
  }

  public void setService2(String service2) {
    this.service2 = service2;
  }


  @Override
  public String toString() {
    return "Route " + service1 + " - " + service2;
  }
}
