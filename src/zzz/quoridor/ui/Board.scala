package zzz.quoridor.ui

import scala.swing.Component
import scala.swing.event.UIElementResized
import scala.swing.event.MouseMoved
import scala.swing.event.MousePressed
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import java.awt.Graphics2D
import java.awt.Color
import java.awt.RenderingHints
import java.awt.BasicStroke
import java.awt.Stroke
import zzz.quoridor.game.WallOrientation
import zzz.quoridor.game.Pawn
import zzz.quoridor.game.BoardElement
import zzz.quoridor.game.Wall
import zzz.quoridor.game.PawnColor
import zzz.quoridor.game.Tile

object Board {

  val BG_COLOR = Color.decode("#000000");
  val GRID_COLOR = Color.decode("#404040");

  val WALL_COLOR = Color.decode("#c0c0c0");

  val BLUE_COLOR = Color.decode("#80bfff");
  val GREEN_COLOR = Color.decode("#80ff80");
  val RED_COLOR = Color.decode("#ff8080");
  val YELLOW_COLOR = Color.decode("#ffe680");

  val GHOST_COLOR = Color.decode("#909090");

}

class Board extends Component {

  val BOARD_SIZE = 9
  val BORDER_PERCENTAGE = 0.2
  val HALF_BORDER_PERCENTAGE = BORDER_PERCENTAGE / 2

  private var tileSize: Double = 0
  private var halfTileSize: Double = 0

  private var pawnSize: Double = 0
  private var halfPawnSize: Double = 0

  private var strokeSize: Double = 0
  private var halfStrokeSize: Double = 0
  private var borderStroke: Stroke = new BasicStroke

  private var tile = new Tile(-1, -1)

  private var wall = new Wall(null, -1, -1)
  private var prevWall = new Wall(null, -1, -1)

  var pawns = new HashMap[PawnColor.Value, Pawn]
  var walls = new ListBuffer[Wall]

  var wallClickListener: (Wall => Unit) = null
  var tileClickListener: (Tile => Unit) = null

  var wallsLeft = 10
  @volatile var myTurn: PawnColor.Value = null
  var possibleMoves = new ListBuffer[BoardElement]
  
  private var repaintCount: Long = 0;

  def invalidateBoard {
    wall.invalidate
    prevWall.invalidate
    tile.invalidate
  }

  override def paint(g: Graphics2D) {

    repaintCount += 1;
    println ("Repaint: " + repaintCount);

    g setRenderingHint (RenderingHints KEY_ANTIALIASING, RenderingHints VALUE_ANTIALIAS_ON)
    g setColor (Board.BG_COLOR)
    g fillRect (0, 0, size.getWidth toInt, size.getHeight toInt)

    g setColor (Board.GRID_COLOR)
    g setStroke (borderStroke)

    for (x <- 0 until BOARD_SIZE) {
      for (y <- 0 until BOARD_SIZE) {
        g drawRect ((x * tileSize) toInt, (y * tileSize) toInt, tileSize toInt, tileSize toInt)
      }
    }

    g setColor (Board.YELLOW_COLOR)
    g fillRect (0, 0, halfStrokeSize toInt, size.getHeight toInt)
    g setColor (Board.GREEN_COLOR)
    g fillRect ((size.getWidth - halfStrokeSize) toInt, 0, halfStrokeSize toInt, size.getHeight toInt)
    g setColor (Board.RED_COLOR)
    g fillRect (0, (size.getHeight - halfStrokeSize) toInt, size.getWidth toInt, halfStrokeSize toInt)
    g setColor (Board.BLUE_COLOR)
    g fillRect (0, 0, size.getWidth toInt, halfStrokeSize toInt)

    pawns.values foreach (pawn => {

      g setColor (pawn.color match {
        case PawnColor.BLUE   => Board.BLUE_COLOR
        case PawnColor.GREEN  => Board.GREEN_COLOR
        case PawnColor.RED    => Board.RED_COLOR
        case PawnColor.YELLOW => Board.YELLOW_COLOR
      })

      g fillOval (
        ((pawn x) * tileSize + halfTileSize - halfPawnSize) toInt,
        ((pawn y) * tileSize + halfTileSize - halfPawnSize) toInt,
        pawnSize toInt,
        pawnSize toInt)

      if (myTurn == pawn.color) {

        possibleMoves foreach (tile => {
          g drawRect ((tile.x * tileSize + strokeSize) toInt, (tile.y * tileSize + strokeSize) toInt,
            (tileSize - strokeSize * 2) toInt, (tileSize - strokeSize * 2) toInt)
        })

      }

    })

    g setColor (Board.WALL_COLOR)

    walls foreach (wall => {
      drawWall(g, wall)
    })

    if (myTurn != null && wall.isValid) {
      g setColor (Board.GHOST_COLOR)
      drawWall(g, wall)
    }

  }

  private def drawWall(g: Graphics2D, wall: Wall) {

    if (wall.orientation == WallOrientation.HORIZONTAL) {
      g fillRect (
        ((wall x) * tileSize + halfStrokeSize) toInt,
        (((wall y) + 1) * tileSize - halfStrokeSize) toInt,
        ((tileSize * 2) - strokeSize) toInt,
        strokeSize toInt)
    } else {
      g fillRect (
        (((wall x) + 1) * tileSize - halfStrokeSize) toInt,
        ((wall y) * tileSize + halfStrokeSize) toInt,
        strokeSize toInt,
        ((tileSize * 2) - strokeSize) toInt)
    }

  }

  private def isGamePossible: Boolean = {

    pawns.values.foreach(p => {
      if (!isGamePossibleForPawn(p)) {
        return false
      }
    })

    return true

  }

  private def isGamePossibleForPawn(pawn: Pawn): Boolean = {

    for (i <- 0 until BOARD_SIZE) {
      if (pawn.color == PawnColor.BLUE) {
        if (areTilesConnected(pawn, new Tile(i, 0))) {
          return true
        }
      } else if (pawn.color == PawnColor.RED) {
        if (areTilesConnected(pawn, new Tile(i, BOARD_SIZE - 1))) {
          return true
        }
      } else if (pawn.color == PawnColor.YELLOW) {
        if (areTilesConnected(pawn, new Tile(0, i))) {
          return true
        }
      } else if (pawn.color == PawnColor.GREEN) {
        if (areTilesConnected(pawn, new Tile(BOARD_SIZE - 1, i))) {
          return true
        }
      }
    }

    return false

  }

  private def areTilesConnected(pawn: BoardElement, tile: BoardElement,
                                checked: ListBuffer[BoardElement] = new ListBuffer[BoardElement]): Boolean = {

    if (pawn == tile) {
      return true
    } else {
      val l = new ListBuffer[BoardElement]
      calculatePossibleMoves(l, pawn)
      checked += pawn
      for (p <- l) {
        if (!checked.contains(p)) {
          if (areTilesConnected(p, tile, checked)) {
            return true
          }
        }
      }
      return false
    }

  }

  private def calculatePossibleMoves(list: ListBuffer[BoardElement], tile: BoardElement) {

    var possibleLeft: Boolean = true
    var possibleRight: Boolean = true
    var possibleUp: Boolean = true
    var possibleDown: Boolean = true

    walls foreach (wall => {
      if (tile.x == wall.x) {
        if (tile.y == wall.y) {
          if (wall.orientation == WallOrientation.HORIZONTAL) {
            possibleDown = false
          } else if (wall.orientation == WallOrientation.VERTICAL) {
            possibleRight = false
          }
        } else if (tile.y == wall.y + 1 && wall.orientation == WallOrientation.VERTICAL) {
          possibleRight = false
        } else if (tile.y == wall.y + 1 && wall.orientation == WallOrientation.HORIZONTAL) {
          possibleUp = false
        }
      } else if (tile.x == wall.x + 1) {
        if (tile.y == wall.y) {
          if (wall.orientation == WallOrientation.HORIZONTAL) {
            possibleDown = false
          } else if (wall.orientation == WallOrientation.VERTICAL) {
            possibleLeft = false
          }
        } else if (tile.y == wall.y + 1 && wall.orientation == WallOrientation.VERTICAL) {
          possibleLeft = false
        } else if (tile.y == wall.y + 1 && wall.orientation == WallOrientation.HORIZONTAL) {
          possibleUp = false
        }
      }
    })

    if (possibleLeft) {
      if (tile.x > 0) {
        list += new Tile(tile.x - 1, tile.y)
      }
    }

    if (possibleRight) {
      if (tile.x < 9 - 1) {
        list += new Tile(tile.x + 1, tile.y)
      }
    }

    if (possibleUp) {
      if (tile.y > 0) {
        list += new Tile(tile.x, tile.y - 1)
      }
    }

    if (possibleDown) {
      if (tile.y < 9 - 1) {
        list += new Tile(tile.x, tile.y + 1)
      }
    }

  }

  def isTileOccupied(tile: BoardElement): Boolean = {

    pawns.values foreach (pawn => {
      if (tile.x == pawn.x && tile.y == pawn.y) {
        return true
      }
    })

    return false

  }

  def calculatePossiblePawnMoves(list: ListBuffer[BoardElement], tile: BoardElement, allowJumps: Boolean = true) {

    var sum: ListBuffer[ListBuffer[BoardElement]] = null

    if (allowJumps) {
      sum = new ListBuffer[ListBuffer[BoardElement]]
    }

    calculatePossibleMoves(list, tile)

    list foreach (x => {
      if (isTileOccupied(x)) {
        if (allowJumps) {
          val second = new ListBuffer[BoardElement]
          calculatePossiblePawnMoves(second, x, false)
          sum += second
        }
        list -= x
      }

    })

    if (allowJumps) {
      sum.foreach(x => {
        list.appendAll(x)
      })
    }

  }

  listenTo(this)
  listenTo(mouse.moves)
  listenTo(mouse.clicks)

  reactions += {

    case e: UIElementResized => {

      println("Board Size: " + (size.getWidth toInt) + "x" + (size.getHeight toInt))
      tileSize = size.getWidth / BOARD_SIZE
      halfTileSize = tileSize / 2
      pawnSize = tileSize / 2.5
      halfPawnSize = pawnSize / 2
      strokeSize = tileSize * BORDER_PERCENTAGE
      halfStrokeSize = strokeSize / 2
      borderStroke = new BasicStroke(strokeSize toFloat)
      repaint;

    }

    case e: MouseMoved => {

      if ((e.point.x > tileSize * HALF_BORDER_PERCENTAGE)
        && (e.point.y > tileSize * HALF_BORDER_PERCENTAGE)
        && (e.point.x < size.getWidth - tileSize * HALF_BORDER_PERCENTAGE)
        && (e.point.y < size.getHeight - tileSize * HALF_BORDER_PERCENTAGE)) {

        var tX = (e.point x) / tileSize
        var tY = (e.point y) / tileSize

        tile.x = math.floor(tX).toInt
        tile.y = math.floor(tY).toInt

        wall invalidate

        if (tX - tile.x + HALF_BORDER_PERCENTAGE > 1) {
          wall.x = tile.x
        }

        if (tX - tile.x - HALF_BORDER_PERCENTAGE < 0) {
          wall.x = tile.x - 1
        }

        if (tY - tile.y + HALF_BORDER_PERCENTAGE > 1) {
          wall.y = tile.y
        }

        if (tY - tile.y - HALF_BORDER_PERCENTAGE < 0) {
          wall.y = tile.y - 1
        }

        if (wall.x != -1) {
          wall.y = tile.y;
          wall.orientation = WallOrientation.VERTICAL
        } else if (wall.y != -1) {
          wall.x = tile.x;
          wall.orientation = WallOrientation.HORIZONTAL
        }

        if (wall.x > BOARD_SIZE - 2) {
          wall.x = BOARD_SIZE - 2;
        }
        
        if (wall.y > BOARD_SIZE - 2) {
          wall.y = BOARD_SIZE - 2;
        }
        
        walls.foreach(w => {
          if ((wall.x == w.x && wall.y == w.y)
            || (w.orientation == WallOrientation.HORIZONTAL && wall.orientation == WallOrientation.HORIZONTAL
              && wall.y == w.y && (wall.x == w.x - 1 || wall.x == w.x + 1))
              || (w.orientation == WallOrientation.VERTICAL && wall.orientation == WallOrientation.VERTICAL
                && wall.x == w.x && (wall.y == w.y - 1 || wall.y == w.y + 1))) {
            wall.invalidate
            tile.invalidate
          }

        })

        if (!possibleMoves.contains(tile)) {
          tile invalidate
        }

        if (wallsLeft < 1) {
          wall invalidate
        }

        if (myTurn != null && !wall.equals(prevWall)) {
          walls += wall
          val possible = isGamePossible
          walls -= wall
          if (!possible) {
            wall invalidate
          } 
          repaint
        }

        prevWall(wall)
        
      } else {
        tile invalidate
      }

    }

    case e: MousePressed => {

      if (myTurn != null) {

        if (wall isValid) {
          println("Clicked " + wall)
          if (wallClickListener != null) {
            wallClickListener(wall copy)
          }
        } else if (tile isValid) {
          println("Clicked " + tile)
          if (tileClickListener != null) {
            tileClickListener(tile copy)
          }
        }

      }

    }

  }

}