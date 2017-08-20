package zzz.quoridor.game

import zzz.quoridor.ui.Board
import zzz.quoridor.ui.StatusPanel
import zzz.quoridor.io.ServerConnector
import zzz.quoridor.io.Message
import scala.collection.mutable.ListBuffer
import scala.swing.Dialog
import java.awt.Dimension
import javax.xml.bind.DatatypeConverter
import zzz.quoridor.ui.ChatPanel
import scala.swing.MainFrame

class GameController(val color: PawnColor.Value, val nickName: String, val board: Board,
                     val messages: StatusPanel, val chat: ChatPanel, val connection: ServerConnector, frame: MainFrame) {

  val HR = 1000

  val CONNECT_MSG = "CONNECT"
  val DISCONNECT_MSG = "DISCONNECT"
  val BIND_MSG = "BIND"
  val PAWN_MSG = "PAWN"
  val WALL_MSG = "WALL"
  val TURN_MSG = "TURN"
  val WIN_MSG = "WIN"
  val CHAT_MSG = "CHAT"
  val LOCKED_MSG = "LOCKED"

  val RESET_PARAM = "RESET"

  var disableShutdownHook = false
  
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run {
      if (!disableShutdownHook) {
        connection sendMessage (new Message(color, DISCONNECT_MSG))
        if (board.myTurn != null) {
          sendTurnMessage
        }
        connection close
      }
    }
  })

  val nickB64 = DatatypeConverter.printBase64Binary(nickName.getBytes)

  val names = scala.collection.mutable.Map[PawnColor.Value, String]()

  board.tileClickListener = tileListener
  board.wallClickListener = wallListener
  connection.messageListener = messageListener
  connection.errorListener = errorListener
  chat.messageListener = chatListener

  val me = new Pawn(color)
  var players = 0
  
  connection.sendMessage(new Message(color, CONNECT_MSG))

  def chatListener(msg: String) {
    connection.sendMessage(new Message(color, CHAT_MSG, -1, -1,
      DatatypeConverter.printBase64Binary(msg.getBytes)))
  }

  def tileListener(tile: Tile) {
    val temp = board.myTurn;
    board.myTurn = null
    connection.sendMessage(new Message(color, PAWN_MSG, tile.x, tile.y))

    if ((color == PawnColor.BLUE && tile.y == 0)
      || (color == PawnColor.RED && tile.y == board.BOARD_SIZE - 1)
      || (color == PawnColor.YELLOW && tile.x == 0)
      || (color == PawnColor.GREEN && tile.x == board.BOARD_SIZE - 1)) {
      connection.sendMessage(new Message(color, WIN_MSG))
    } else {
      sendTurnMessage
    }
  }

  def wallListener(wall: Wall) {
    val temp = board.myTurn;
    board.myTurn = null
    board.wallsLeft = board.wallsLeft - 1
    wallMessage(wall)
    sendTurnMessage
  }

  def sendTurnMessage {

    var next: PawnColor.Value = null

    if (color == PawnColor.BLUE) {
      if (board.pawns.contains(PawnColor.GREEN)) {
        next = PawnColor.GREEN
      } else if (board.pawns.contains(PawnColor.RED)) {
        next = PawnColor.RED
      } else if (board.pawns.contains(PawnColor.YELLOW)) {
        next = PawnColor.YELLOW
      }
    } else if (color == PawnColor.GREEN) {
      if (board.pawns.contains(PawnColor.RED)) {
        next = PawnColor.RED
      } else if (board.pawns.contains(PawnColor.YELLOW)) {
        next = PawnColor.YELLOW
      } else if (board.pawns.contains(PawnColor.BLUE)) {
        next = PawnColor.BLUE
      }
    } else if (color == PawnColor.RED) {
      if (board.pawns.contains(PawnColor.YELLOW)) {
        next = PawnColor.YELLOW
      } else if (board.pawns.contains(PawnColor.BLUE)) {
        next = PawnColor.BLUE
      } else if (board.pawns.contains(PawnColor.GREEN)) {
        next = PawnColor.GREEN
      }
    } else if (color == PawnColor.YELLOW) {
      if (board.pawns.contains(PawnColor.BLUE)) {
        next = PawnColor.BLUE
      } else if (board.pawns.contains(PawnColor.GREEN)) {
        next = PawnColor.GREEN
      } else if (board.pawns.contains(PawnColor.RED)) {
        next = PawnColor.RED
      }
    }

    if (next == null) {
      next = color;
    }
    
    connection.sendMessage(new Message(next, TURN_MSG))

  }

  private def wallMessage(wall: Wall) {
    if (wall.orientation == WallOrientation.HORIZONTAL) {
      connection.sendMessage(new Message(color, WALL_MSG, wall.x + HR, wall.y + HR))
    } else {
      connection.sendMessage(new Message(color, WALL_MSG, wall.x, wall.y))
    }
  }

  def errorListener(e: Exception) {
    Dialog.showConfirmation(board,
      "You lost connection with the server", "Client disconnected", Dialog.Options.Default, Dialog.Message.Error, null)
    disableShutdownHook = true
    sys.exit(0)
  }

  def messageListener(message: Message) {
    message.action match {

      case CONNECT_MSG => {
        
        board.myTurn = null
        messages reset
        
        board.walls.clear
        chat clear

        connection.sendMessage(new Message(color, PAWN_MSG, me.x, me.y, RESET_PARAM))
        connection.sendMessage(new Message(color, BIND_MSG, -1, -1, nickB64))

        if (color == message.color) {
          connection.sendMessage(new Message(color, TURN_MSG))
        }

        chat #= "New game started"

      }

      case BIND_MSG => {

        val n = new String(DatatypeConverter.parseBase64Binary(message.param))
        messages.setNickName(message.color, n)
        names.put(message.color, n)

        chat #= n + " has joined the room"

      }

      case PAWN_MSG => {

        var pawn: Pawn = null

        if (board.pawns.contains(message.color)) {
          pawn = board.pawns.get(message.color).get

          if (message.param != RESET_PARAM) {

            var n: String = null

            if (color == message.color) {
              n = "You"
            } else {
              n = names.getOrElse(message.color, message.color.toString())
            }

            chat #= n + " moved from " + (pawn.x + 1) + "x" + (pawn.y + 1) + " to " + (message.x + 1) + "x" + (message.y + 1)
          }

        } else {
          pawn = new Pawn(message.color)
          board.pawns += (pawn.color -> pawn)
          players = players + 1
          println("Players: " + players)
          if (players > 2) {
            board.wallsLeft = 5
          } else {
            board.wallsLeft = 10
          }
          board.pawns.keys.foreach { x => messages.setWalls(x, board.wallsLeft) }
        }

        pawn.x = message.x
        pawn.y = message.y

        board repaint

      }

      case WALL_MSG => {

        var orientation: WallOrientation.Value = null
        var x: Int = 0
        var y: Int = 0

        if (message.x >= HR) {
          orientation = WallOrientation.HORIZONTAL
          x = message.x - HR
          y = message.y - HR
        } else {
          orientation = WallOrientation.VERTICAL
          x = message.x
          y = message.y
        }

        val newWall = new Wall(orientation, x, y)

        if (!board.walls.contains(newWall)) {

          var n: String = null

          if (color == message.color) {
            n = "You"
          } else {
            n = names.getOrElse(message.color, message.color.toString())
          }

          var o: String = null

          if (orientation == WallOrientation.HORIZONTAL) {
            o = "horizontal"
          } else {
            o = "vertical"
          }

          chat #= n + " placed a " + o + " wall on " + (x + 1) + "x" + (y + 1)

          messages.decreaseWalls(message.color)
          board.walls += newWall
          board repaint
        }

      }

      case TURN_MSG => {

        board invalidateBoard

        board.myTurn = null

        messages.setTurn(message.color)

        if (message.color == color) {
          board.possibleMoves.clear
          board calculatePossiblePawnMoves (board.possibleMoves, board.pawns.get(message.color).get)
          //chat #= "It's your turn"
          if (board.possibleMoves.isEmpty && board.wallsLeft == 0) {
            connection sendMessage (new Message(color, LOCKED_MSG))
          } else {
            board.myTurn = color
            frame.peer.toFront
          }
        } else {
          //chat #= "It's " + names.getOrElse(message.color, message.color) + "'s turn"
        }

        board repaint

      }

      case WIN_MSG => {

        disableShutdownHook = true
        connection close
        
        var n: String = null

        if (color == message.color) {
          n = "You"
        } else {
          n = names.getOrElse(message.color, message.color.toString())
        }

        Dialog.showConfirmation(board, n
          + " won the game", "Game over", Dialog.Options.Default, Dialog.Message.Info, null)
          
        sys.exit(0)

      }

      case DISCONNECT_MSG => {

        players = players - 1
        
        if (message.color != me.color) {
          board.pawns.remove(message.color)
          messages setQuit (message.color)

          if (board.myTurn != null) {
            board.possibleMoves.clear
            board calculatePossiblePawnMoves (board.possibleMoves, board.pawns.get(color).get)
          }

          board repaint

        }

        val msg = names.getOrElse(message.color, message.color) + " left the room"

        chat #= msg

      }

      case CHAT_MSG => {
        chat #= names.getOrElse(message.color, message.color) + " says: " +
          new String(DatatypeConverter.parseBase64Binary(message.param))
      }

      case LOCKED_MSG => {

        if (message.color == color) {
          chat #= "You have no possible moves"
          sendTurnMessage
        } else {
          chat #= names.getOrElse(message.color, message.color) + " has no possible moves"
        }

      }

    }
  }

}