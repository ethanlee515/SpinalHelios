package helios
import spinal.core._
import spinal.lib._

case class Correction(params: HeliosParams) extends Bundle {
  // Some 0-th slots are unused and squashed
  import params._
  val ns_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_last = Bits(grid_width_u bits)
  val ud_tail = Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  // Dealing with 1-indexing correspondingly
  def ns(k: Int, i: Int, j: Int) = ns_tail(k)(i - 1)(j - 1)
  def ew(k: Int, i: Int, j: Int) = {
    if(j != grid_width_z) {
      ew_tail(k)(i - 1)(j)
    } else {
      assert(i == grid_width_x - 1)
      ew_last(k)
    }
  }
  def ud(k: Int, i: Int, j: Int) = ud_tail(k)(i)(j)
  def data(k: Int, x: Int, y: Int) : Bool = {
    /* ns */
    // (even, odd) = ??
    if(x % 2 == 0 && y % 2 == 1) {
      val i = x + 1
      val j = y / 2 + 1
      return ns(k, i, j)
    }
    // (odd, even) = ??
    if(x % 2 == 1 && y % 2 == 0) {
      if(y == 0) {
        return False
      }
      if(y < code_distance - 1) {
        val i = x
        val j = y / 2 + 1
        return ns(k, i, j)
      }
      if(y == code_distance - 1) {
        val i = x + 1
        val j = grid_width_z
        return ns(k, i, j)
      }
    }
    /* ew */
    // (odd, odd) = ??
    if(x % 2 == 1 && y % 2 == 1) {
      val i = x + 1
      val j = y / 2
      return ew(k, i, j)
    }
    // (even, even) = ??
    if(x % 2 == 0 && y % 2 == 0) {
      if(y == 0) {
        val i = x + 1
        val j = 0
        return ew(k, i, j)
      }
      if(y > 0 && y < code_distance - 1) {
        val i = x + 1
        val j = y / 2
        return ew(k, i, j)
      }
      if(y == code_distance - 1) {
        if(x < code_distance - 1) {
          return False
        }
        if(x == code_distance - 1) {
          val i = grid_width_x - 1
          val j = grid_width_z - 1
          return ew(k, i, j)
        }
      }
    }
    // Matching above should be exhaustive
    println(f"bad: data($k, $x, $y) missing")
    return ???
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