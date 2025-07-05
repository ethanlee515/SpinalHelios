package helios
import spinal.core._

// object HeliosParams {
//   val code_distance = 7
//   val iteration_counter_width = 8
//   val max_delay = 3
//   val neighbor_count = 6
//   val weight_ns = 2
//   val weight_ew = 2
//   val weight_ud = 2
//   val max_weight = 2
//   /* derived params */
//   val grid_width_x = code_distance + 1
//   val grid_width_z = code_distance / 2
//   val grid_width_u = code_distance
//   val grid_size = grid_width_u * grid_width_x * grid_width_z
//   val x_bit_width = log2Up(grid_width_x)
//   val z_bit_width = log2Up(grid_width_z)
//   val u_bit_width = log2Up(grid_width_u)
//   val address_width = x_bit_width + z_bit_width + u_bit_width
// }

case class HeliosParams(
    code_distance: Int = 7,
    // iteration_counter_width: Int = 8,
    max_delay: Int = 3,
    neighbor_count: Int = 6,
    weight_ns: Int = 2,
    weight_ew: Int = 2,
    weight_ud: Int = 2,
    max_weight: Int = 2
) {
  val grid_width_x = code_distance + 1
  val grid_width_z = code_distance / 2
  val grid_width_u = code_distance
  val grid_size = grid_width_u * grid_width_x * grid_width_z
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
}

object Stage extends SpinalEnum {
  val measurement_loading, grow, merge, peeling, result_valid, measurement_preparing = newElement()
}

object Boundary extends Enumeration {
  type Boundary = Value
  val no_boundary, a_boundary, nexist_edge = Value
}
