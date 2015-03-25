 /* Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */



public class Timer {
  private  long start[];
  private  long end[];
  private  int num = 10;
  
  public Timer(int n) {
      num = n;
      init();
  }

  public Timer() {
      init();
  } 

  private void init() {
     start = new long[num];
      end = new long[num];
      for (int i=0; i<num; i++){
        start[i] = 0;
        end[i] = 0;
      }
 }

  public void start(int idx) {
       start[idx] = System.currentTimeMillis();
    }
  
  public  void end(int idx) {
       end[idx] += System.currentTimeMillis()-start[idx];
    }
  
  public  void dump() {
      for (int i=0; i<num; i++) {
        if (end[i]!=0) {
  	  System.out.println("Timer["+i+"]:" + end[i] + "ms\n");
        }
      }
    }
}
