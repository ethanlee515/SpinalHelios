import spinal.core._

object Stage extends SpinalEnum {
  val idle, measurement_loading, grow, merge, peeling, result_valid,
    parameters_loading, measurement_preparing = newElement()
}

object Header extends SpinalEnum {
  val start_decoding_message, measurement_data_header = newElement()
}

object BoundaryCondition extends SpinalEnum {
  val no_boundary, a_boundary, non_existent_edge, connected_to_a_FIFO =
    newElement()
}
