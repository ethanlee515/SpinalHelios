import spinal.core._

object Stage extends SpinalEnum {
  val measurement_loading, grow, merge, peeling, result_valid,
    measurement_preparing = newElement()
}

object Boundary extends Enumeration {
  type Boundary = Value
  val no_boundary, a_boundary, nexist_edge = Value
}

object Command extends SpinalEnum {
  val start_decoding, measurement_data = newElement()
}

object HeliosParams {
  val max_weight = 2
  val grid_width_x = 4
  // is this ever not 1?
  val grid_width_z = 1
  val grid_width_u = 3
  val iteration_counter_width = 8
  val max_delay = 3
  val neighbor_count = 6
  /* derived params */
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
}