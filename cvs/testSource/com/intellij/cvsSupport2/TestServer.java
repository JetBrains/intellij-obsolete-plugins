package com.intellij.cvsSupport2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * author: lesya
 */

class ConnectionHandler implements Runnable {
  private final InputStream myServerInputStream;
  private final OutputStream myServerOutputStream;

  private final InputStream myClientInputStream;
  private final OutputStream myClientOutputStream;

  ConnectionHandler(InputStream serverInputStream,
                           OutputStream serverOutputStream,
                           InputStream clientInputStream,
                           OutputStream clientOutputStream) {
    myServerInputStream = serverInputStream;
    myServerOutputStream = serverOutputStream;

    myClientInputStream = clientInputStream;
    myClientOutputStream = clientOutputStream;
  }

  @Override
  public void run() {
    new Thread(() -> {
      try {
        readRequest();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }, "cvs1").start();

    new Thread(() -> {
      try {
        readResponse();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }, "cvs2").start();

  }

  private void readResponse() throws IOException {
    while (true) {
      int c = myServerInputStream.read();
      if (c != -1) {
        synchronized (this) {
          System.out.print((char)c);
          System.out.flush();
        }
        myClientOutputStream.write(c);
      } else {
        myClientOutputStream.close();
      }
    }
  }

  private void readRequest() throws IOException {
    while (true) {
      int c = myClientInputStream.read();
      if (c != -1) {
        synchronized (this) {
          System.err.print((char)c);
          System.err.flush();
        }
        myServerOutputStream.write(c);
      } else {
        myServerOutputStream.close();
      }
    }

  }
}

public class TestServer {
  private final ServerSocket myServerSocket;
  private final Socket mySocket;
  private final InputStream myInputStream;
  private final OutputStream myOutputStream;

  public TestServer() {
    try {

      myServerSocket = new ServerSocket(2401);

      mySocket = new Socket("localhost", 2403);
      myInputStream = mySocket.getInputStream();
      myOutputStream = mySocket.getOutputStream();


    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void run() throws IOException {
    while (true) {
      Socket socket = myServerSocket.accept();
      ConnectionHandler connectionHandler = new ConnectionHandler(myInputStream, myOutputStream,
                                                                  socket.getInputStream(),
                                                                  socket.getOutputStream());
      new Thread(connectionHandler,"test cvs server").start();
    }

  }

  public static void main(String[] args) throws IOException {
    TestServer testServer = new TestServer();
    testServer.run();
  }
}
