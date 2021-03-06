/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.xdebugger;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XDependentBreakpointManager;
import org.jdom.Element;

/**
 * @author nik
 */
public class XDependentBreakpointsTest extends XBreakpointsTestCase {
  private XDependentBreakpointManager myDependentBreakpointManager;


  protected void setUp() throws Exception {
    super.setUp();
    myDependentBreakpointManager = myBreakpointManager.getDependentBreakpointManager();
  }

  public void testDelete() throws Exception {
    XLineBreakpoint<?> master = createMaster();
    XLineBreakpoint<?> slave = createSlave();
    myDependentBreakpointManager.setMasterBreakpoint(slave, master, true);
    assertSame(master, myDependentBreakpointManager.getMasterBreakpoint(slave));
    assertTrue(myDependentBreakpointManager.isLeaveEnabled(slave));
    assertSame(slave, assertOneElement(myDependentBreakpointManager.getSlaveBreakpoints(master)));
    assertSame(slave, assertOneElement(myDependentBreakpointManager.getAllSlaveBreakpoints()));
    
    myBreakpointManager.removeBreakpoint(master);
    assertNull(myDependentBreakpointManager.getMasterBreakpoint(slave));
    assertEmpty(myDependentBreakpointManager.getAllSlaveBreakpoints());
  }

  public void testSerialize() throws Exception {
    XLineBreakpoint<?> master = createMaster();
    XLineBreakpoint<?> slave = createSlave();
    myDependentBreakpointManager.setMasterBreakpoint(slave, master, true);

    Element element = save();
    myDependentBreakpointManager.clearMasterBreakpoint(slave);
    //System.out.println(JDOMUtil.writeElement(element, SystemProperties.getLineSeparator()));
    load(element);

    XBreakpoint<?>[] breakpoints = myBreakpointManager.getAllBreakpoints();
    assertEquals(2, breakpoints.length);
    XLineBreakpoint newMaster = (XLineBreakpoint)breakpoints[0];
    XLineBreakpoint newSlave = (XLineBreakpoint)breakpoints[1];
    assertEquals("file://master", newMaster.getFileUrl());
    assertEquals("file://slave", newSlave.getFileUrl());
    assertSame(newMaster, myDependentBreakpointManager.getMasterBreakpoint(newSlave));
    assertTrue(myDependentBreakpointManager.isLeaveEnabled(newSlave));
  }

  private XLineBreakpoint<MyBreakpointProperties> createSlave() {
    return myBreakpointManager.addLineBreakpoint(MY_LINE_BREAKPOINT_TYPE, "file://slave", 2, new MyBreakpointProperties());
  }

  private XLineBreakpoint<MyBreakpointProperties> createMaster() {
    return myBreakpointManager.addLineBreakpoint(MY_LINE_BREAKPOINT_TYPE, "file://master", 1, new MyBreakpointProperties());
  }
}
