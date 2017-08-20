package zzz.quoridor.ui

import scala.swing.GridPanel
import scala.swing.Label
import scala.swing.Orientation
import zzz.quoridor.game.PawnColor
import java.awt.Color
import scala.swing.Alignment
import javax.swing.border.LineBorder

class StatusPanel extends GridPanel(2, 2) {

  val bgColor = Color.decode("#111111")
  val bgColorActive = Color.decode("#444444")
  
  class StatusLine(val textColor: Color, val nickName: Label = new Label(""), val walls: Label = new Label("")) extends GridPanel(1, 2) {

    border = new LineBorder(Color.BLACK, 2, false)
    
    var wallsLeft = 10

    background = bgColor

    nickName.foreground = textColor
    walls.foreground = textColor

    contents += nickName
    contents += walls

  }

  private val lines = Map(
    PawnColor.BLUE -> new StatusLine(Board.BLUE_COLOR),
    PawnColor.GREEN -> new StatusLine(Board.GREEN_COLOR),
    PawnColor.RED -> new StatusLine(Board.RED_COLOR),
    PawnColor.YELLOW -> new StatusLine(Board.YELLOW_COLOR))

  lines.values.foreach { x => contents += x }

  def setTurn(c: PawnColor.Value) {

    lines.foreach(line => {
      if (c == line._1) {
        line._2.background = bgColorActive
      } else {
        line._2.background = bgColor
      }
    })

  }

  def setNickName(c: PawnColor.Value, nick: String) {

    val line = lines get (c) get

    line.nickName.text = nick

  }

  def setWalls(c: PawnColor.Value, walls: Int) {

    val line = lines get (c) get

    line.wallsLeft = walls
    updateWalls(c)
  }

  def setQuit(c: PawnColor.Value) {

    val line = lines get (c) get

    line.walls.text = "left the room"
    
  }

  def decreaseWalls(c: PawnColor.Value) {

    val line = lines get (c) get

    line.wallsLeft = line.wallsLeft - 1

  }

  def updateWalls(c: PawnColor.Value) {

    val line = lines get (c) get

    if (line.wallsLeft == 0) {
      line.walls.text = "has no walls"
    } else if (line.wallsLeft == 1) {
      line.walls.text = "has 1 wall"
    } else {
      line.walls.text = "has " + line.wallsLeft + " walls"
    }

  }

}