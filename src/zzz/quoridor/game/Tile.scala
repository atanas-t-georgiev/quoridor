package zzz.quoridor.game

class Tile(var x: Int, var y: Int) extends BoardElement {

  def copy: Tile = new Tile(x, y)
  
  override def toString: String = "Tile [" + x + "x" + y + "]"
  
}