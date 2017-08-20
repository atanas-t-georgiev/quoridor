package zzz.quoridor.game

trait BoardElement {

  var x: Int
  var y: Int

  def isValid: Boolean = x >= 0 && y >= 0

  def invalidate {
    x = -1
    y = -1
  }

  override def equals(other: Any): Boolean = {
    other match {
      case other: BoardElement => {
        return x == other.x && y == other.y
      }
      case _ => false
    }
  }

}