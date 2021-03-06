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
package com.intellij.idea;

import com.intellij.idea.SocketLock;
import junit.framework.TestCase;

/**
 * @author mike
 */
public class LockSupportTest extends TestCase {
  public void testLock() throws Exception {
    final SocketLock lock = new SocketLock();
    assertTrue(lock.lock("abc"));
    lock.dispose();
  }

  public void testTwoLocks() throws Exception {
    final SocketLock lock1 = new SocketLock();
    final SocketLock lock2 = new SocketLock();

    assertTrue(lock1.lock("1"));
    assertTrue(lock1.lock("1.1"));
    assertTrue(lock2.lock("2"));
    assertTrue(!lock1.lock("2"));
    assertTrue(!lock2.lock("1"));
    assertTrue(!lock2.lock("1.1"));

    lock1.dispose();
    lock2.dispose();
  }

  public void testDispose() throws Exception {
    final SocketLock lock1 = new SocketLock();
    final SocketLock lock2 = new SocketLock();

    assertTrue(lock1.lock("1"));
    assertTrue(!lock2.lock("1"));

    lock1.dispose();
    assertTrue(lock2.lock("1"));
    lock2.dispose();
  }

  public void testUnlock() throws Exception {
    final SocketLock lock1 = new SocketLock();
    final SocketLock lock2 = new SocketLock();

    assertTrue(lock1.lock("1"));
    assertTrue(!lock2.lock("1"));

    lock1.unlock("1");
    assertTrue(lock2.lock("1"));
    lock1.dispose();
    lock2.dispose();
  }
}
