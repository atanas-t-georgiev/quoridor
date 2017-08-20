package zzz.quoridor.game

class Wall(var orientation: WallOrientation.Value, var x: Int, var y: Int) extends BoardElement {
  
  override def toString: String = "Wall [" + orientation + " " + x + "x" + y + "]"
  
  def apply(other: Wall) {
    orientation = other.orientation
    x = other.x
    y = other.y
  }
  
  def copy: Wall = new Wall(orientation, x, y)
 
  override def equals(other: Any): Boolean = {
    other match {
      case other: Wall => {
        return orientation == other.orientation && x == other.x && y == other.y
      }
      case _ => false
    }
  }
  
}