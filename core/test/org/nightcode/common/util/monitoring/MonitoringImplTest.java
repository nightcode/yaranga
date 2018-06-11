/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nightcode.common.util.monitoring;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.easymock.Capture;
import org.easymock.EasyMock;

import junit.framework.TestCase;

/**
 * Unit test for {@link MonitoringImpl}.
 */
public class MonitoringImplTest extends TestCase {
  
  public void testRegisterMonitoringComponent()
      throws IOException, ExecutionException, InterruptedException {
    MonitoringVisitor monitoringVisitorMock = EasyMock.createMock(MonitoringVisitor.class);

    Capture<MonitoringVisitor> monitoringVisitorCapture = EasyMock.newCapture();
    
    MonitoringComponent monitoringComponentMock = EasyMock.createMock(MonitoringComponent.class);
    monitoringComponentMock.retrieveData(EasyMock.capture(monitoringVisitorCapture));
    EasyMock.expectLastCall().once();
    
    EasyMock.replay(monitoringVisitorMock, monitoringComponentMock);
    
    Monitoring monitoring = new MonitoringImpl(monitoringVisitorMock);
    monitoring.registerMonitoringComponent(monitoringComponentMock);
    
    monitoring.start().get();
    Thread.sleep(100);
    monitoring.stop().get();
    
    assertEquals(monitoringVisitorMock, monitoringVisitorCapture.getValue());
    
    EasyMock.verify(monitoringVisitorMock, monitoringComponentMock);
  }
}
