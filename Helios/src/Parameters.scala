package helios
import spinal.core._

case class HeliosParams(
    code_distance: Int = 7,
    // iteration_counter_width: Int = 8,
    max_delay: Int = 3,
    neighbor_count: Int = 6,
    weight_ns: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
    weight_ew: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
    weight_ud: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
) {
  val grid_width_x = code_distance + 1
  val grid_width_z = code_distance / 2
  val grid_width_u = code_distance
  val grid_size = grid_width_u * grid_width_x * grid_width_z
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
  // max weights
  val weights_ns = Seq.tabulate(grid_width_u, grid_width_x + 1, grid_width_z + 1)(weight_ns)
  val max_weight_ns = weights_ns.flatten.flatten.max
  val weights_ew = Seq.tabulate(grid_width_u, grid_width_x + 1, grid_width_z + 1)(weight_ew)
  val max_weight_ew = weights_ew.flatten.flatten.max
  val weights_ud = Seq.tabulate(grid_width_u + 1, grid_width_x, grid_width_z)(weight_ud)
  val max_weight_ud = weights_ud.flatten.flatten.max
  val max_weight = List(max_weight_ns, max_weight_ew, max_weight_ud).max
}

object Stage extends SpinalEnum {
  val measurement_loading, grow, merge, peeling, result_valid, measurement_preparing = newElement()
}

object Boundary extends Enumeration {
  type Boundary = Value
  val no_boundary, a_boundary, nexist_edge = Value
}
