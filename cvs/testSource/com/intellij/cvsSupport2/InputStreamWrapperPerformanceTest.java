package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.javacvsImpl.CvsCommandStopper;
import com.intellij.cvsSupport2.javacvsImpl.io.InputStreamWrapper;
import com.intellij.cvsSupport2.javacvsImpl.io.ReadThread;
import com.intellij.cvsSupport2.javacvsImpl.io.ReadWriteStatistics;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.WaitFor;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NotNull;
import org.netbeans.lib.cvsclient.ICvsCommandStopper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotEquals;

/**
 * author: lesya
 */
public class InputStreamWrapperPerformanceTest extends HeavyPlatformTestCase {
  private static final int SIZE1 = 1000000;
  private static final int SIZE2 = 10;

  @Override
  public void tearDown() throws Exception {
    new WaitFor(5000) {
      @Override
      protected boolean condition() {
        return ReadThread.READ_THREADS.isEmpty();
      }
    }.assertCompleted(ReadThread.READ_THREADS.toString());
    super.tearDown();
  }

  public void testReadLongFile() throws IOException {
    int BUFFER_SIZE = 128 * 1024;
    byte[] chunk = new byte[BUFFER_SIZE];
    int size = 100000000;
    long start;

    InputStream baseStream = new ByteArrayInputStream(new byte[size + 100]);
    try (InputStream stream = new InputStreamWrapper(baseStream, new CvsCommandStopper(), createStatistics())) {
      start = System.currentTimeMillis();
      while (size > 0) {
        final int bytesToRead = Math.min(size, chunk.length);
        final int bytesRead = stream.read(chunk, 0, bytesToRead);
        if (bytesRead < 0) {
          break;
        }
        size -= bytesRead;
      }
    }

    System.out.println(System.currentTimeMillis() - start);
  }

  public void testLock() throws IOException {
    for (int i = 0; i < 10000; i++) {
      try (InputStream stream = new InputStreamWrapper(createSmallInputStream(), new CvsCommandStopper(), createStatistics())) {
        int v = stream.read();
        assertNotEquals(-1, v);
      }
    }
  }

  public void test1() throws IOException {
    long timeForStandardReading = read(createInputStream(), SIZE1);
    long timeForInputStreamWrapperReading =
      read(new InputStreamWrapper(createInputStream(), new CvsCommandStopper(), createStatistics()), SIZE1);

    System.out.println(timeForStandardReading);
    System.out.println(timeForInputStreamWrapperReading);
  }

  public void test2() throws IOException {
    long timeForStandardReading = read(createSlowInputStream(), 10);
    long timeForInputStreamWrapperReading =
      read(new InputStreamWrapper(createSlowInputStream(), new CvsCommandStopper(), createStatistics()), 10);

    System.out.println(timeForStandardReading);
    System.out.println(timeForInputStreamWrapperReading);
  }

  public void test3() throws IOException, InterruptedException {
    ReadThread readThread = new ReadThread(new BufferedInputStream(createInputStream()), new CvsCommandStopper());
    Thread thread = new Thread(readThread, "read cvs");
    thread.start();
    readThread.waitForStart();
    for (int i = 0; i < SIZE1; i++) {
      assertEquals(String.valueOf(i), (byte)i, (byte)readThread.read());
    }
    assertEquals(-1, readThread.read());
    thread.join();
  }

  public void test4() throws IOException {
    try {
      Semaphore semaphore = new Semaphore();
      try (InputStream stream = new InputStreamWrapper(createSynchronizedInputStream(semaphore), createTestStopper(), createStatistics())) {
        assertEquals(1, stream.read());
      }
      finally {
        semaphore.up();
      }
      fail();
    }
    catch (ProcessCanceledException ignored) {

    }
  }

  private static ReadWriteStatistics createStatistics() {
    return new ReadWriteStatistics(Progress.DEAF);
  }


  private static long read(InputStream inputStream, int size) throws IOException {
    long start = System.currentTimeMillis();
    try {
      for (int i = 0; i < size; i++) {
        int result = inputStream.read();
        assertEquals(inputStream.getClass().getName() + " " + i, (byte)i, (byte)result);
      }
      int result = inputStream.read();
      assertEquals(inputStream.getClass().getName(), -1, result);
      return System.currentTimeMillis() - start;
    }
    finally {
      inputStream.close();
    }
  }

  private static InputStream createInputStream() {
    byte[] bytes = new byte[SIZE1];
    for (int i = 0; i < SIZE1; i++) bytes[i] = (byte)i;
    return new ByteArrayInputStream(bytes);
  }

  private static InputStream createSmallInputStream() {
    return new ByteArrayInputStream("11111".getBytes(StandardCharsets.UTF_8));
  }

  private static InputStream createSlowInputStream() {
    byte[] bytes = new byte[SIZE2];
    for (int i = 0; i < SIZE2; i++) bytes[i] = (byte)i;
    return new ByteArrayInputStream(bytes) {
      @Override
      public synchronized int read() {
        TimeoutUtil.sleep(1500);
        return super.read();
      }
    };
  }

  private static InputStream createSynchronizedInputStream(final Semaphore semaphore) {
    return new InputStream() {
      @Override
      public int read() {
        semaphore.down();
        semaphore.waitFor();
        return 1;
      }

      @Override
      public int read(byte @NotNull [] b, int off, int len) {
        semaphore.down();
        semaphore.waitFor();
        return 1;
      }
    };
  }

  private static ICvsCommandStopper createTestStopper() {
    return new ICvsCommandStopper() {
      private int myCounter;
      private volatile boolean myPing;

      @Override
      public boolean isAborted() {
        myPing = true;
        return ++myCounter == 5;
      }

      @Override
      public boolean isAlive() {
        return myPing;
      }

      @Override
      public void resetAlive() {
        myPing = false;
      }
    };
  }
}
