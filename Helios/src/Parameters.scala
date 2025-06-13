import spinal.core._

object Stage extends SpinalEnum {
  val idle, measurement_loading, grow, merge, peeling, result_valid,
    parameters_loading, measurement_preparing = newElement()
}

object Boundary extends SpinalEnum {
  val no_boundary, a_boundary, nexist_edge, connected_to_a_FIFO =
    newElement()
}
