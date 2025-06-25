import spinal.core._

object HeliosParams {
  val code_distance = 7
  val iteration_counter_width = 8
  val max_delay = 3
  val neighbor_count = 6
  val weight_ns = 2
  val weight_ew = 2
  val weight_ud = 2
  val max_weight = 2
  /* derived params */
  val grid_width_x = code_distance + 1
  val grid_width_z = code_distance / 2
  val grid_width_u = code_distance
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
}

object Stage extends SpinalEnum {
  val measurement_loading, grow, merge, peeling, result_valid,
    measurement_preparing = newElement()
}

object Boundary extends Enumeration {
  type Boundary = Value
  val no_boundary, a_boundary, nexist_edge = Value
}