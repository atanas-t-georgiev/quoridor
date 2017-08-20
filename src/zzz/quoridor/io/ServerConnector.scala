package zzz.quoridor.io

import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

class ServerConnector(val host: String, val port: Int) extends Thread {

  private var socket: Socket = null;
  private var reader: BufferedReader = null;
  
  var messageListener: Message => Unit = null
  var errorListener: Exception => Unit = null

  def open {
    socket = new Socket(host, port)
    reader = new BufferedReader(new InputStreamReader(socket getInputStream))
  }

  def close {
    try {
      socket close
    } catch {
      case _: Throwable =>
    }
  }
  
  def enterRoom(room: String) {
    socket.getOutputStream write (("ROOM " + room + "\n") getBytes)
    socket.getOutputStream flush
  }
  
  def receiveColor: String = {
    
    val line = reader.readLine()
    val tokens = line.split(" ")
    
    if (tokens.length == 2 && tokens(0) == "ACCEPTED") {
      return tokens(1)
    } else {
      throw new Exception("Room full")
    }
    
  }
  
  def sendMessage(message: Message) {
    socket.getOutputStream write ((message.toString + "\n") getBytes)
    socket.getOutputStream flush
    
    println("Message sent: " + message)
  }

  override def run {

    try {

      while (!Thread.interrupted) {

        val line = reader readLine

        if (line == null) {
          throw new IOException("End of stream")
        }

        println("Message received: " + line)
        
        messageListener(Message(line))
        
      }

    } catch {
      case e: Exception => {
        if (errorListener != null) {
          errorListener(e)
        }
      }
    }

  }

}