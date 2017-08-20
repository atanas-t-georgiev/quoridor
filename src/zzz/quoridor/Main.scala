package zzz.quoridor

import scala.swing.MainFrame
import scala.swing.BorderPanel
import zzz.quoridor.ui.Board
import zzz.quoridor.game.GameController
import zzz.quoridor.game.Pawn
import zzz.quoridor.game.PawnColor
import zzz.quoridor.io.ServerConnector
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import zzz.quoridor.ui.StatusPanel
import scala.swing.BorderPanel.Position._
import javax.sound.midi.Soundbank
import zzz.quoridor.ui.ChatPanel
import javax.swing.UIManager
import javax.swing.border.LineBorder
import java.awt.Color

object Main {

  val ABOUT = "Quoridor v1.4 (c) 2016, 2017 Atanas Georgiev\n"

  val gameFrame = new MainFrame
  val board = new Board
  val messagesPanel = new StatusPanel
  val chat = new ChatPanel

  def main(args: Array[String]) {

    if (args.length < 5) {
      println("Usage: quoridor <host> <port> <room> <nick_name>")
      return
    }

    var port: Int = 0

    try {
      port = args(2).toInt
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException
      }
    } catch {
      case e: NumberFormatException => {
        println("ERROR: Invalid port number: " + args(0))
        return
      }
      case e: IllegalArgumentException => {
        println("ERROR: Port number out of range [1-65535]: " + args(0))
        return
      }
    }

    /*var color: PawnColor.Value = null;

    try {
      color = PawnColor.withName(args(0) toUpperCase)
    } catch {
      case _: Throwable => {
        println("ERROR: Invalid color. Must be one of [BLUE, RED, GREEN, YELLOW]")
        return
      }
    }*/

    val nickName = new StringBuilder

    for (i <- 4 until args.length) {
      nickName.append(args(i))
      if (args.length > i + 1) {
        nickName.append(" ")
      }
    }

    val serverConnector = new ServerConnector(args(1), port)

    try {
      serverConnector open
    } catch {
      case e: Throwable => {
        println("ERROR: Cannot connect to server: " + e.getMessage)
        sys.exit(10)
      }
    }

    System.out.println(ABOUT);

    var color: PawnColor.Value = null;

    try {
      serverConnector enterRoom (args(3))
      color = PawnColor.withName(serverConnector receiveColor)
    } catch {
      case e: Throwable => {
        println("ERROR: Cannot enter room: " + e.getMessage)
        sys.exit(11)
      }
    }

    println("Entered room [" + args(3) + "]")

    val controller = new GameController(color, nickName.toString, board, messagesPanel, chat, serverConnector, gameFrame)
    
    val gamePanel = new BorderPanel {
      layout(board) = Center
      layout(messagesPanel) = South
    }

    val mainPanel = new BorderPanel {
      layout(gamePanel) = Center
      layout(chat) = East
    }
    
    gamePanel.border = new LineBorder(Color.BLACK, 10, false)

    UIManager setLookAndFeel (UIManager.getSystemLookAndFeelClassName)

    board.preferredSize = new Dimension(540, 540)
    messagesPanel.preferredSize = new Dimension(540, 50)
    chat.preferredSize = new Dimension(300, 590)

    gameFrame.iconImage = gameFrame.toolkit.getImage(getClass().getResource("pawn.png").toURI().toURL())
    gameFrame.title = "Quoridor [" + args(3) + "]"
    gameFrame.contents = mainPanel
    gameFrame.resizable = false
    gameFrame.pack

    val dimension = Toolkit.getDefaultToolkit().getScreenSize()
    val x = ((dimension.width - gameFrame.bounds.width) / 2)
    val y = ((dimension.height - gameFrame.bounds.height) / 2)
    gameFrame.location = new Point(x, y)
    
    gameFrame.visible = true;
    
    serverConnector start

  }

}