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

package com.intellij.history.core.revisions;

import com.intellij.history.core.LocalVcsTestCase;
import com.intellij.history.core.changes.ChangeSet;
import com.intellij.history.core.changes.CreateFileChange;
import org.junit.Test;

public class RevisionsCauseChangesTest extends LocalVcsTestCase {
  ChangeSet cs = cs("Action", new CreateFileChange(1, "f", null, -1, false));

  @Test
  public void testCurrentRevisionIsBefore() {
    Revision r = new CurrentRevision(null);
    assertNull(r.getCauseChangeName());
    assertNull(r.getCauseChange());
  }

  @Test
  public void testRevisionBeforeChangeIsBefore() {
    Revision r = new RevisionBeforeChange(null, null, null, cs);
    assertNull(r.getCauseChangeName());
    assertNull(r.getCauseChange());
  }

  @Test
  public void testRevisionAfterChangeIsBefore() {
    Revision r = new RevisionAfterChange(null, null, null, cs);
    assertEquals("Action", r.getCauseChangeName());
    assertEquals(cs, r.getCauseChange());
  }
}
