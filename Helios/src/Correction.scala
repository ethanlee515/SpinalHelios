package helios
import spinal.core._
import spinal.lib._

class EdgeId2D(val exists: Boolean)
final case class NorthSouthEdge(s: Int, t: Int, override val exists: Boolean = true) extends EdgeId2D(exists)
final case class EastWestEdge(s: Int, t: Int, override val exists: Boolean = true) extends EdgeId2D(exists)

class CoordinateTransformer(params: HeliosParams) {
  import params._
  def getEdgeId(x: Int, y: Int) : EdgeId2D = {
    /* ns */
    if(x % 2 == 0 && y % 2 == 1) {
      return NorthSouthEdge(x + 1, y / 2 + 1)
    }
    if(x % 2 == 1 && y % 2 == 0) {
      if(y == 0) {
        return NorthSouthEdge(x + 1, 0, false)
      }
      if(y < code_distance - 1) {
        return NorthSouthEdge(x + 1, y / 2)
      }
      if(y == code_distance - 1) {
        return NorthSouthEdge(x + 1, grid_width_z)
      }
    }
    /* ew */
    if(x % 2 == 1 && y % 2 == 1) {
      return EastWestEdge(x + 1, y / 2)
    }
    if(x % 2 == 0 && y % 2 == 0) {
      if(y == 0) {
        return EastWestEdge(x + 1, 0)
      }
      if(y > 0 && y < code_distance - 1) {
        return EastWestEdge(x + 1, y / 2)
      }
      if(y == code_distance - 1) {
        if(x < code_distance - 1) {
          return EastWestEdge(x + 1,  grid_width_z, false)
        }
        if(x == code_distance - 1) {
          return EastWestEdge(grid_width_x - 1, grid_width_z)
        }
      }
    }
    // Matching above should be exhaustive
    println(f"bad: data($x, $y) missing")
    return ???
  }
}

case class Correction(params: HeliosParams) extends Bundle {
  // Some 0-th slots are unused and squashed
  import params._
  val ns_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_last = Bits(grid_width_u bits)
  val ud_tail = Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  // Dealing with 1-indexing correspondingly
  def ns(k: Int, s: Int, t: Int) = ns_tail(k)(s - 1)(t - 1)
  def ew(k: Int, s: Int, t: Int) = {
    if(t != grid_width_z) {
      ew_tail(k)(s - 1)(t)
    } else {
      assert(s == grid_width_x - 1)
      ew_last(k)
    }
  }
  def ud(k: Int, s: Int, t: Int) = ud_tail(k)(s)(t)

  val transformer = new CoordinateTransformer(params)
  import transformer._

  def getEdge(k: Int, edge: EdgeId2D) = {
    assert(edge.exists)
    edge match {
      case NorthSouthEdge(s, t, _) => ns(k, s, t)
      case EastWestEdge(s, t, _) => ew(k, s, t)
    }
  }

  def data(k: Int, x: Int, y: Int) : Bool = {
    val edge_id = getEdgeId(x, y)
    if(edge_id.exists) {
      return getEdge(k, getEdgeId(x, y))
    } else {
      return False
    }
  }
}

class CorrectionFlattener(params: HeliosParams) extends Component {
  import params._
  /* IO */
  val layered_correction = in port Flow(Correction(params))
  val flat_correction = out port Flow(Vec.fill(code_distance, code_distance)(Bool()))
  // data
  val out_payload = Reg(Vec.fill(code_distance, code_distance)(Bool()))
  val out_valid = Reg(Bool())
  flat_correction.payload := out_payload
  flat_correction.valid := out_valid
  for(i <- 0 until code_distance;
      j <- 0 until code_distance) {
    val v_ij = Vec.tabulate(grid_width_z) { k =>
      layered_correction.data(k, i, j)
    }
    out_payload(i)(j) := v_ij.xorR
  }
  out_valid := layered_correction.valid
}