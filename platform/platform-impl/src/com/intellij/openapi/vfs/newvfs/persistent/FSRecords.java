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

/*
 * @author max
 */
package com.intellij.openapi.vfs.newvfs.persistent;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.Forceable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.IntArrayList;
import com.intellij.util.io.PersistentStringEnumerator;
import com.intellij.util.io.ResizeableMappedFile;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.io.storage.Storage;
import gnu.trove.TIntArrayList;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"PointlessArithmeticExpression", "HardCodedStringLiteral"})
public class FSRecords implements Disposable, Forceable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.vfs.persistent.FSRecords");

  private static final int VERSION = 8;

  private static final int PARENT_OFFSET = 0;
  private static final int PARENT_SIZE = 4;
  private static final int NAME_OFFSET = PARENT_OFFSET + PARENT_SIZE;
  private static final int NAME_SIZE = 4;
  private static final int FLAGS_OFFSET = NAME_OFFSET + NAME_SIZE;
  private static final int FLAGS_SIZE = 4;
  private static final int ATTREF_OFFSET = FLAGS_OFFSET + FLAGS_SIZE;
  private static final int ATTREF_SIZE = 4;
  private static final int TIMESTAMP_OFFSET = ATTREF_OFFSET + ATTREF_SIZE;
  private static final int TIMESTAMP_SIZE = 8;
  private static final int MODCOUNT_OFFSET = TIMESTAMP_OFFSET + TIMESTAMP_SIZE;
  private static final int MODCOUNT_SIZE = 4;
  private static final int LENGTH_OFFSET = MODCOUNT_OFFSET + MODCOUNT_SIZE;
  private static final int LENGTH_SIZE = 8;

  private static final int RECORD_SIZE = LENGTH_OFFSET + LENGTH_SIZE;

  private static final byte[] ZEROES = new byte[RECORD_SIZE];

  private static final int HEADER_VERSION_OFFSET = 0;
  private static final int HEADER_RESERVED_4BYTES_OFFSET = 4; // Reserverd
  private static final int HEADER_GLOBAL_MODCOUNT_OFFSET = 8;
  private static final int HEADER_CONNECTION_STATUS_OFFSET = 12;
  private static final int HEADER_SIZE = HEADER_CONNECTION_STATUS_OFFSET + 4;

  private static final int CONNECTED_MAGIC = 0x12ad34e4;
  private static final int SAFELY_CLOSED_MAGIC = 0x1f2f3f4f;
  private static final int CORRUPTED_MAGIC = 0xabcf7f7f;

  private static final String CHILDREN_ATT = "FsRecords.DIRECTORY_CHILDREN";
  private static final Object lock = new Object();
  private DbConnection myConnection;

  private volatile static int ourLocalModificationCount = 0;

  private static final int FREE_RECORD_FLAG = 0x100;
  private static final int ALL_VALID_FLAGS = PersistentFS.ALL_VALID_FLAGS | FREE_RECORD_FLAG;

  static {
    //noinspection ConstantConditions
    assert HEADER_SIZE <= RECORD_SIZE;
  }

  private static class DbConnection {
    private static int refCount = 0;
    private static final Object LOCK = new Object();
    private static final TObjectIntHashMap<String> myAttributeIds = new TObjectIntHashMap<String>();

    private static PersistentStringEnumerator myNames;
    private static Storage myAttributes;
    private static ResizeableMappedFile myRecords;
    private static final TIntArrayList myFreeRecords = new TIntArrayList();

    private static boolean myDirty = false;
    private static ScheduledFuture<?> myFlushingFuture;
    private static boolean myCorrupted = false;

    public static DbConnection connect() {
      synchronized (LOCK) {
        if (refCount == 0) {
          init();
          scanFreeRecords();
          setupFlushing();
        }
        refCount++;
      }

      return new DbConnection();
    }

    private static void scanFreeRecords() {
      final int filelength = (int)getRecords().length();
      LOG.assertTrue(filelength % RECORD_SIZE == 0);

      int count = filelength / RECORD_SIZE;
      for (int n = 2; n < count; n++) {
        if ((getFlags(n) & FREE_RECORD_FLAG) != 0) {
          addFreeRecord(n);
        }
      }
    }

    public static int getFreeRecord() {
      if (myFreeRecords.isEmpty()) return 0;
      return myFreeRecords.remove(myFreeRecords.size() - 1);
    }

    private static void createBrokenMarkerFile() {
      File brokenMarker = getCorruptionMarkerFile();
      try {
        final FileWriter writer = new FileWriter(brokenMarker);
        writer.write("These files are corrupted and must be rebuilt from the scratch on next startup");
        writer.close();
      }
      catch (IOException e) {
        // No luck.
      }
    }

    private static File getCorruptionMarkerFile() {
      return new File(basePath(), "corruption.marker");
    }

    private static void init() {
      final File basePath = basePath();
      basePath.mkdirs();

      final File namesFile = new File(basePath, "names.dat");
      final File attributesFile = new File(basePath, "attrib.dat");
      final File recordsFile = new File(basePath, "records.dat");

      if (!namesFile.exists()) {
        invalidateIndex();
      }

      try {
        if (getCorruptionMarkerFile().exists()) {
          invalidateIndex();
          throw new IOException("Corruption marker file found");
        }

        myNames = new PersistentStringEnumerator(namesFile);
        myAttributes = Storage.create(attributesFile.getCanonicalPath());
        myRecords = new ResizeableMappedFile(recordsFile, 20 * 1024);

        if (myRecords.length() == 0) {
          cleanRecord(0); // Clean header
          cleanRecord(1); // Create root record
          setCurrentVersion();
        }

        if (getVersion() != VERSION) {
          throw new IOException("FS repository version mismatch");
        }

        if (myRecords.getInt(HEADER_CONNECTION_STATUS_OFFSET) != SAFELY_CLOSED_MAGIC) {
          throw new IOException("FS repository wasn't safely shut down");
        }
        markDirty();
      }
      catch (IOException e) {
        LOG.info("Filesystem storage is corrupted or does not exist. [Re]Building. Reason: " + e.getMessage());
        try {
          closeFiles();

          boolean deleted = true;
          deleted &= FileUtil.delete(getCorruptionMarkerFile());
          deleted &= deleteWithSubordinates(namesFile);
          deleted &= Storage.deleteFiles(attributesFile.getCanonicalPath());
          deleted &= deleteWithSubordinates(recordsFile);

          if (!deleted) {
            throw new IOException("Cannot delete filesystem storage files");
          }
        }
        catch (final IOException e1) {
          final Runnable warnAndShutdown = new Runnable() {
            public void run() {
              boolean unitTest = ApplicationManager.getApplication().isUnitTestMode();
              if (!(unitTest || ApplicationManager.getApplication().isHeadlessEnvironment())) {
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                              "Files in " + basePath.getPath() + " are locked. IntelliJ IDEA will not be able to start up",
                                              "Fatal Error",
                                              JOptionPane.ERROR_MESSAGE);
              }
              if (unitTest) {
                e1.printStackTrace();
              }
              Runtime.getRuntime().halt(1);
            }
          };

          if (EventQueue.isDispatchThread()) {
            warnAndShutdown.run();
          }
          else {
            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(warnAndShutdown);
          }

          throw new RuntimeException("Can't rebuild filesystem storage ", e1);
        }

        init();
      }
    }

    private static void invalidateIndex() {
      LOG.info("Marking VFS as corrupted");
      FileUtil.createIfDoesntExist(new File(PathManager.getIndexRoot(), "corruption.marker"));
    }

    private static File basePath() {
      return new File(PathManager.getSystemPath() + "/caches/");
    }

    private static boolean deleteWithSubordinates(File file) {
      final String baseName = file.getName();
      final File[] files = file.getParentFile().listFiles(new FileFilter() {
        public boolean accept(final File pathname) {
          return pathname.getName().startsWith(baseName);
        }
      });

      boolean ok = true;
      if (files != null) {
        for (File f : files) {
          ok &= FileUtil.delete(f);
        }
      }

      return ok;
    }

    private static void markDirty() throws IOException {
      if (!myDirty) {
        myDirty = true;
        myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, CONNECTED_MAGIC);
      }
    }

    private static void setupFlushing() {
      myFlushingFuture = JobScheduler.getScheduler().scheduleAtFixedRate(new Runnable() {
        int lastModCount = 0;
        public void run() {
          if (lastModCount == ourLocalModificationCount && !HeavyProcessLatch.INSTANCE.isRunning()) {
            flushSome();
          }
          lastModCount = ourLocalModificationCount;
        }
      }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    public static void force() {
      synchronized (lock) {
        try {
          markClean();
        }
        catch (IOException e) {
          // Ignore
        }
        if (myNames != null) {
          myNames.force();
          myAttributes.force();
          myRecords.force();
        }
      }
    }

    public static void flushSome() {
      synchronized (lock) {
        myNames.force();
        boolean attrsClean = myAttributes.flushSome();

        if (attrsClean) {
          try {
            markClean();
          }
          catch (IOException e) {
            // Ignore
          }
          myRecords.force();
        }
      }
    }

    public static boolean isDirty() {
      return myDirty || myNames.isDirty() || myAttributes.isDirty() || myRecords.isDirty();
    }


    private static int getVersion() throws IOException {
      final int storageVersion = myAttributes.getVersion();
      final int recordsVersion = myRecords.getInt(HEADER_VERSION_OFFSET);
      if (storageVersion != recordsVersion) return -1;

      return recordsVersion;
    }

    private static void setCurrentVersion() throws IOException {
      myRecords.putInt(HEADER_VERSION_OFFSET, VERSION);
      myAttributes.setVersion(VERSION);
      myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, SAFELY_CLOSED_MAGIC);
    }

    public static void cleanRecord(final int id) throws IOException {
      myRecords.put(id * RECORD_SIZE, ZEROES, 0, RECORD_SIZE);
    }

    public static PersistentStringEnumerator getNames() {
      return myNames;
    }

    public static Storage getAttributes() {
      return myAttributes;
    }

    public static ResizeableMappedFile getRecords() {
      return myRecords;
    }

    public void dispose() throws IOException {
      synchronized (LOCK) {
        refCount--;
        if (refCount == 0) {
          closeFiles();
        }
      }
    }

    private static void closeFiles() throws IOException {
      if (myFlushingFuture != null) {
        myFlushingFuture.cancel(false);
        myFlushingFuture = null;
      }

      if (myNames != null) {
        myNames.close();
        myNames = null;
      }

      if (myAttributes != null) {
        myAttributes.dispose();
        myAttributes = null;
      }

      if (myRecords != null) {
        markClean();
        myRecords.close();
        myRecords = null;
      }
    }

    private static void markClean() throws IOException {
      if (myDirty) {
        myDirty = false;
        myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, myCorrupted ? CORRUPTED_MAGIC : SAFELY_CLOSED_MAGIC);
      }
    }

    private static int getAttributeId(String attId) throws IOException {
      if (myAttributeIds.containsKey(attId)) {
        return myAttributeIds.get(attId);
      }

      int id = myNames.enumerate(attId);
      myAttributeIds.put(attId, id);
      return id;
    }

    private static RuntimeException handleError(final Throwable e) {
      if (!myCorrupted) {
        createBrokenMarkerFile();
        myCorrupted = true;
        force();
      }

      return new RuntimeException(e);
    }

    public static void addFreeRecord(final int id) {
      myFreeRecords.add(id);
    }
  }

  public FSRecords() {
  }

  public void connect() {
    myConnection = DbConnection.connect();
  }

  private static ResizeableMappedFile getRecords() {
    return DbConnection.getRecords();
  }

  private static Storage getAttributes() {
    return DbConnection.getAttributes();
  }

  public static PersistentStringEnumerator getNames() {
    return DbConnection.getNames();
  }

  public static int createRecord() {
    synchronized (lock) {
      try {
        DbConnection.markDirty();

        final int free = DbConnection.getFreeRecord();
        if (free == 0) {
          final int filelength = (int)getRecords().length();
          LOG.assertTrue(filelength % RECORD_SIZE == 0);
          int newrecord = filelength / RECORD_SIZE;
          DbConnection.cleanRecord(newrecord);
          assert filelength + RECORD_SIZE == getRecords().length();
          return newrecord;
        }
        else {
          DbConnection.cleanRecord(free);
          return free;
        }
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public void deleteRecordRecursively(int id) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        incModCount(id);
        doDeleteRecursively(id);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  private void doDeleteRecursively(final int id) {
    for (int subrecord : list(id)) {
      doDeleteRecursively(subrecord);
    }

    deleteRecord(id);
  }

  private void deleteRecord(final int id) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        int att_page = getAttributeRecordId(id);
        if (att_page != 0) {
          final DataInputStream attStream = getAttributes().readStream(att_page);
          while (attStream.available() > 0) {
            attStream.readInt(); // Attribute ID;
            int attAddress = attStream.readInt();
            getAttributes().deleteRecord(attAddress);
          }
          attStream.close();
          getAttributes().deleteRecord(att_page);
        }

        DbConnection.cleanRecord(id);
        addToFreeRecordsList(id);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  private void addToFreeRecordsList(int id) throws IOException {
    DbConnection.addFreeRecord(id);
    setFlags(id, FREE_RECORD_FLAG, false);
  }

  public int[] listRoots() throws IOException {
    synchronized (lock) {
      DbConnection.markDirty();
      final DataInputStream input = readAttribute(1, CHILDREN_ATT);
      if (input == null) return ArrayUtil.EMPTY_INT_ARRAY;

      int[] result;
      try {
        final int count = input.readInt();
        result = ArrayUtil.newIntArray(count);
        for (int i = 0; i < count; i++) {
          input.readInt(); // Name
          result[i] = input.readInt(); // Id
        }
      }
      finally {
        input.close();
      }

      return result;
    }
  }

  public void force() {
    DbConnection.force();
  }

  public boolean isDirty() {
    return DbConnection.isDirty();
  }

  public int findRootRecord(String rootUrl) throws IOException {
    synchronized (lock) {
      DbConnection.markDirty();
      final int root = getNames().enumerate(rootUrl);

      final DataInputStream input = readAttribute(1, CHILDREN_ATT);
      int[] names = ArrayUtil.EMPTY_INT_ARRAY;
      int[] ids = ArrayUtil.EMPTY_INT_ARRAY;

      if (input != null) {
        try {
          final int count = input.readInt();
          names = ArrayUtil.newIntArray(count);
          ids = ArrayUtil.newIntArray(count);
          for (int i = 0; i < count; i++) {
            final int name = input.readInt();
            final int id = input.readInt();
            if (name == root) {
              return id;
            }

            names[i] = name;
            ids[i] = id;
          }
        }
        finally {
          input.close();
        }
      }

      final DataOutputStream output = writeAttribute(1, CHILDREN_ATT);
      int id;
      try {
        id = createRecord();
        output.writeInt(names.length + 1);
        for (int i = 0; i < names.length; i++) {
          output.writeInt(names[i]);
          output.writeInt(ids[i]);
        }
        output.writeInt(root);
        output.writeInt(id);
      }
      finally {
        output.close();
      }

      return id;
    }
  }

  public void deleteRootRecord(int id) throws IOException {
    synchronized (lock) {
      DbConnection.markDirty();
      final DataInputStream input = readAttribute(1, CHILDREN_ATT);
      assert input != null;
      int count;
      int[] names;
      int[] ids;
      try {
        count = input.readInt();

        names = ArrayUtil.newIntArray(count);
        ids = ArrayUtil.newIntArray(count);
        for (int i = 0; i < count; i++) {
          names[i] = input.readInt();
          ids[i] = input.readInt();
        }
      }
      finally {
        input.close();
      }

      final int index = ArrayUtil.find(ids, id);
      assert index >= 0;

      names = ArrayUtil.remove(names, index);
      ids = ArrayUtil.remove(ids, index);

      final DataOutputStream output = writeAttribute(1, CHILDREN_ATT);
      try {
        output.writeInt(count - 1);
        for (int i = 0; i < names.length; i++) {
          output.writeInt(names[i]);
          output.writeInt(ids[i]);
        }
      }
      finally {
        output.close();
      }
    }
  }

  public int[] list(int id) {
    synchronized (lock) {
      try {
        final DataInputStream input = readAttribute(id, CHILDREN_ATT);
        if (input == null) return ArrayUtil.EMPTY_INT_ARRAY;

        final int count = input.readInt();
        final int[] result = ArrayUtil.newIntArray(count);
        for (int i = 0; i < count; i++) {
          result[i] = input.readInt();
        }
        input.close();
        return result;
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public boolean wereChildrenAccessed(int id) {
    try {
      synchronized (lock) {
        int encodedAttId = DbConnection.getAttributeId(CHILDREN_ATT);
        final int att = findAttributePage(id, encodedAttId, false);
        return att != 0;
      }
    }
    catch (Throwable e) {
      throw DbConnection.handleError(e);
    }
  }
  
  public void updateList(int id, int[] children) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        final DataOutputStream record = writeAttribute(id, CHILDREN_ATT);
        record.writeInt(children.length);
        for (int child : children) {
          if (child == id) {
            LOG.error("Cyclic parent child relations");
          }
          else {
            record.writeInt(child);
          }
        }
        record.close();
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  private static void incModCount(int id) throws IOException {
    ourLocalModificationCount++;
    final int count = getModCount() + 1;
    getRecords().putInt(HEADER_GLOBAL_MODCOUNT_OFFSET, count);

    int parent = id;
    while (parent != 0) {
      setModCount(parent, count);
      parent = getParent(parent);
    }
  }

  public static int getLocalModCount() {
    return ourLocalModificationCount; // This is volatile, only modified under Application.runWriteAction() lock.
  }

  public static int getModCount() {
    synchronized (lock) {
      return getRecords().getInt(HEADER_GLOBAL_MODCOUNT_OFFSET);
    }
  }

  public static int getParent(int id) {
    synchronized (lock) {
      try {
        final int parentId = getRecords().getInt(id * RECORD_SIZE + PARENT_OFFSET);
        if (parentId == id) {
          LOG.error("Cyclic parent child relations in the database. id = " + id);
          return 0;
        }

        return parentId;
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public void setParent(int id, int parent) {
    if (id == parent) {
      LOG.error("Cyclic parent/child relations");
      return;
    }

    synchronized (lock) {
      try {
        DbConnection.markDirty();
        incModCount(id);
        getRecords().putInt(id * RECORD_SIZE + PARENT_OFFSET, parent);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static String getName(int id) {
    synchronized (lock) {
      try {
        final int nameId = getRecords().getInt(id * RECORD_SIZE + NAME_OFFSET);
        return nameId != 0 ? getNames().valueOf(nameId) : "";
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public void setName(int id, String name) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        incModCount(id);
        getRecords().putInt(id * RECORD_SIZE + NAME_OFFSET, getNames().enumerate(name));
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static int getFlags(int id) {
    synchronized (lock) {
      return getRecords().getInt(id * RECORD_SIZE + FLAGS_OFFSET);
    }
  }

  public void setFlags(int id, int flags, final boolean markAsChange) {
    synchronized (lock) {
      try {
        if (markAsChange) {
          DbConnection.markDirty();
          incModCount(id);
        }
        getRecords().putInt(id * RECORD_SIZE + FLAGS_OFFSET, flags);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static long getLength(int id) {
    synchronized (lock) {
      return getRecords().getLong(id * RECORD_SIZE + LENGTH_OFFSET);
    }
  }

  public void setLength(int id, long len) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        incModCount(id);
        getRecords().putLong(id * RECORD_SIZE + LENGTH_OFFSET, len);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static long getTimestamp(int id) {
    synchronized (lock) {
      return getRecords().getLong(id * RECORD_SIZE + TIMESTAMP_OFFSET);
    }
  }

  public void setTimestamp(int id, long value) {
    synchronized (lock) {
      try {
        DbConnection.markDirty();
        incModCount(id);
        getRecords().putLong(id * RECORD_SIZE + TIMESTAMP_OFFSET, value);
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static int getModCount(int id) {
    synchronized (lock) {
      return getRecords().getInt(id * RECORD_SIZE + MODCOUNT_OFFSET);
    }
  }

  private static void setModCount(int id, int value) throws IOException {
    getRecords().putInt(id * RECORD_SIZE + MODCOUNT_OFFSET, value);
  }

  private static int getAttributeRecordId(final int id) throws IOException {
    return getRecords().getInt(id * RECORD_SIZE + ATTREF_OFFSET);
  }

  @Nullable
  public DataInputStream readAttribute(int id, String attId) {
    try {
      synchronized (attId) {
        final int att;
        synchronized (lock) {
          int encodedAttId = DbConnection.getAttributeId(attId);
          att = findAttributePage(id, encodedAttId, false);
          if (att == 0) return null;
        }

        return getAttributes().readStream(att);
      }
    }
    catch (Throwable e) {
      throw DbConnection.handleError(e);
    }
  }

  private int findAttributePage(int fileId, int attributeId, boolean createIfNotFound) throws IOException {
    if (fileId <= 0) {
      throw DbConnection.handleError(new AssertionError("assert fileId > 0 failed"));
    }

    if ((getFlags(fileId) & FREE_RECORD_FLAG) != 0) { // TODO: This assertion is a bit timey, will remove when bug is caught.
      throw DbConnection.handleError(new AssertionError("Trying to find an attribute of deleted page"));
    }

    int attrsRecord = getAttributeRecordId(fileId);

    if (attrsRecord == 0) {
      if (!createIfNotFound) return 0;

      attrsRecord = getAttributes().createNewRecord();
      getRecords().putInt(fileId * RECORD_SIZE + ATTREF_OFFSET, attrsRecord);
    }
    else {
      final DataInputStream attrRefs = getAttributes().readStream(attrsRecord);
      try {
        while (attrRefs.available() > 0) {
          final int attIdOnPage = attrRefs.readInt();
          final int attAddress = attrRefs.readInt();

          if (attIdOnPage == attributeId) return attAddress;
        }
      }
      finally {
        attrRefs.close();
      }
    }

    if (createIfNotFound) {
      Storage.AppenderStream appender = getAttributes().appendStream(attrsRecord);
      appender.writeInt(attributeId);
      int attAddress = getAttributes().createNewRecord();
      appender.writeInt(attAddress);
      appender.close();
      return attAddress;
    }

    return 0;
  }

  private class AttributeOutputStream extends DataOutputStream {
    private final String myAttributeId;
    private final int myFileId;

    private AttributeOutputStream(final int fileId, final String attributeId) {
      super(new ByteArrayOutputStream());
      myFileId = fileId;
      myAttributeId = attributeId;
    }

    public void close() throws IOException {
      super.close();

      try {
        synchronized (myAttributeId) {
          final int att;
          synchronized (lock) {
            DbConnection.markDirty();
            incModCount(myFileId);
            final int encodedAttId = DbConnection.getAttributeId(myAttributeId);
            att = findAttributePage(myFileId, encodedAttId, true);
          }

          final DataOutputStream sinkStream = getAttributes().writeStream(att);
          sinkStream.write(((ByteArrayOutputStream)out).toByteArray());
          sinkStream.close();
        }
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  @NotNull
  public DataOutputStream writeAttribute(final int id, final String attId) {
    return new AttributeOutputStream(id, attId);
  }

  public void dispose() {
    synchronized (lock) {
      try {
        DbConnection.force();
        DbConnection.closeFiles();
      }
      catch (Throwable e) {
        throw DbConnection.handleError(e);
      }
    }
  }

  public static void invalidateCaches() {
    DbConnection.createBrokenMarkerFile();
  }

  public static void checkSanity() {
    long startTime = System.currentTimeMillis();
    synchronized (lock) {
      final int fileLength = (int)getRecords().length();
      assert fileLength % RECORD_SIZE == 0;
      int recordCount = fileLength / RECORD_SIZE;

      IntArrayList usedAttributeRecordIds = new IntArrayList();
      IntArrayList validAttributeIds = new IntArrayList();
      for(int id=2; id<recordCount; id++) {
        int flags = getFlags(id);
        assert (flags & ~ALL_VALID_FLAGS) == 0;
        if ((flags & FREE_RECORD_FLAG) != 0) {
          LOG.assertTrue(DbConnection.myFreeRecords.contains(id), "Record, marked free, not in free list: " + id);
        }
        else {
          LOG.assertTrue(!DbConnection.myFreeRecords.contains(id), "Record, not marked free, in free list: " + id);
          checkRecordSanity(id, recordCount, usedAttributeRecordIds, validAttributeIds);
        }
      }
    }

    long endTime = System.currentTimeMillis();
    System.out.println("Sanity check took " + (endTime-startTime) + " ms");
  }


  private static void checkRecordSanity(final int id, final int recordCount, final IntArrayList usedAttributeRecordIds,
                                        final IntArrayList validAttributeIds) {
    int parentId = getParent(id);
    assert parentId >= 0 && parentId < recordCount;
    if (parentId > 0) {
      final int parentFlags = getFlags(parentId);
      assert (parentFlags & FREE_RECORD_FLAG) == 0;
      assert (parentFlags & PersistentFS.IS_DIRECTORY_FLAG) != 0;
    }

    String name = getName(id);
    LOG.assertTrue(parentId == 0 || name.length() > 0, "File with empty name found under " + getName(parentId) + ", id=" + id);

    int attributeRecordId;
    try {
      attributeRecordId = getAttributeRecordId(id);
    }
    catch(IOException ex) {
      throw DbConnection.handleError(ex);
    }

    assert attributeRecordId >= 0;
    if (attributeRecordId > 0) {
      try {
        checkAttributesSanity(attributeRecordId, usedAttributeRecordIds, validAttributeIds);
      }
      catch (IOException ex) {
        throw DbConnection.handleError(ex);
      }
    }

    long length = getLength(id);
    assert length >= -1: "Invalid file length found for " + name + ": " + length;
  }

  private static void checkAttributesSanity(final int attributeRecordId, final IntArrayList usedAttributeRecordIds,
                                            final IntArrayList validAttributeIds) throws IOException {
    assert !usedAttributeRecordIds.contains(attributeRecordId);
    usedAttributeRecordIds.add(attributeRecordId);

    final DataInputStream dataInputStream = getAttributes().readStream(attributeRecordId);
    try {
      final int streamSize = dataInputStream.available();
      assert (streamSize % 8) == 0;
      for(int i=0; i<streamSize / 8; i++) {
        int attId = dataInputStream.readInt();
        int attDataRecordId = dataInputStream.readInt();
        assert !usedAttributeRecordIds.contains(attDataRecordId);
        usedAttributeRecordIds.add(attDataRecordId);
        if (!validAttributeIds.contains(attId)) {
          assert getNames().valueOf(attId).length() > 0;
          validAttributeIds.add(attId);
        }
        getAttributes().checkSanity(attDataRecordId);
      }
    }
    finally {
      dataInputStream.close();
    }
  }
}
