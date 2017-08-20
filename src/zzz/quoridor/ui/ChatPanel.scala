package zzz.quoridor.ui

import scala.swing.BorderPanel
import scala.swing.TextArea
import scala.swing.TextField
import scala.swing.event.EditDone
import java.awt.Color
import java.awt.Dimension
import javax.swing.border.LineBorder
import scala.swing.ScrollPane
import java.awt.Point

class ChatPanel extends BorderPanel {

  val bgColor = Color.decode("#333333")
  val bgColorChat = Color.decode("#676767")
  val fgColor = Color.decode("#eeeeee")


  val log = new TextArea;
  var text = new TextField

  val scroll = new ScrollPane(log)
  
  log.background = bgColor
  text.background = bgColorChat
  log.foreground = fgColor
  text.foreground = fgColor

  var messageListener: String => Unit = null

  log.lineWrap = true
  log.focusable = false
  log.border = new LineBorder(bgColor, 4, false)
  log.editable = false
  
  text.border = new LineBorder(bgColorChat, 4, false)
  text.editable = true

  text.preferredSize = new Dimension(300, 35)

  scroll.border = new LineBorder(bgColor, 1, false)
  
  layout(scroll) = BorderPanel.Position.Center
  layout(text) = BorderPanel.Position.South

  def #=(message: String) {
    log.append(message + "\n")
    scroll.peer.getViewport.setViewPosition(new Point(0, log.size.height))
  }
  
  def clear {
    log.text = ""
  }

  listenTo(text)

  text.reactions += {

    case e: EditDone => {
      val t = text.text
      text.text = ""
      if (messageListener != null && t != null && !t.isEmpty) {
        messageListener(t)
      }

    }

  }

}