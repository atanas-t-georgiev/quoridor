package zzz.quoridor.io

import zzz.quoridor.game.PawnColor

class Message(val color: PawnColor.Value, val action: String, val x: Int = -1, val y: Int = -1, val param: String = null) {

  override def toString: String = {
    if (x >= 0 && y >= 0) {
      if (param == null) {
        color + " " + action + " " + x + " " + y
      } else {
        color + " " + action + " " + x + " " + y + " " + param
      }
    } else {
      if (param == null) {
        color + " " + action
      } else {
        color + " " + action + " " + param
      }
    }
  }

}

object Message {

  def apply(s: String): Message = {

    val tokens = s.split(" ")

    tokens.length match {
      case 5 => return new Message(PawnColor.withName(tokens(0)), tokens(1), tokens(2) toInt, tokens(3) toInt, tokens(4))
      case 4 => return new Message(PawnColor.withName(tokens(0)), tokens(1), tokens(2) toInt, tokens(3) toInt)
      case 3 => return new Message(PawnColor.withName(tokens(0)), tokens(1), -1, -1, tokens(2))
      case 2 => return new Message(PawnColor.withName(tokens(0)), tokens(1))
      case _ => throw new IllegalArgumentException("Input string is invalid")
    }

  }

}