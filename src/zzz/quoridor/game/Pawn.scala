package zzz.quoridor.game

class Pawn(var color: PawnColor.Value) extends BoardElement {

  var x: Int = color match {
    case PawnColor.BLUE => 4
    case PawnColor.GREEN => 0
    case PawnColor.RED => 4
    case PawnColor.YELLOW => 8
  }
  
  var y: Int = color match {
    case PawnColor.BLUE => 8
    case PawnColor.GREEN => 4
    case PawnColor.RED => 0
    case PawnColor.YELLOW => 4
  }
  
}