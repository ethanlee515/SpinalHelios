import spinal.core._

object Stage extends SpinalEnum {
  val idle, measurement_loading, grow, merge, peeling, result_valid,
    parameters_loading, measurement_preparing = newElement()
}

object Boundary extends SpinalEnum {
  val no_boundary, a_boundary, nexist_edge, connected_to_a_FIFO =
    newElement()
}

object HeliosParams {
  val max_weight = 2
  val grid_width_x = 4
  // is this ever not 1?
  val grid_width_z = 1
  val grid_width_u = 3
  val iteration_counter_width = 8
  val max_delay = 2
  val neighbor_count = 6
  /* derived params */
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
}