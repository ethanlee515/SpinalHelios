// Ported from "neighbor_link_internal_v2.v"

import spinal.core._
import spinal.lib._

class NeighborLinkInternal(
  address_width: Int = 6,
  max_weight: Int = 2
) extends Component {
  // `+ 1` to store from 0 to max_weight *inclusive*
  val link_bit_width = log2Up(max_weight + 1)
  val exposed_data_size = address_width + 1 + 1 + 1 + 1 + 3
  /* IO */
  val global_stage = in port Stage()
  val fully_grown = out Bool()
  val a_increase = in Bool()
  val b_increase = in Bool()
  val is_boundary = out Bool()
  val a_is_error = in Bool()
  val b_is_error = in Bool()
  val is_error = out Reg(Bool())
  val a_input_data = in Bits(exposed_data_size bits)
  val b_input_data = in Bits(exposed_data_size bits)
  val a_output_data = out Bits(exposed_data_size bits)
  val b_output_data = out Bits(exposed_data_size bits)
  val weight_in = in UInt(link_bit_width bits)
  val boundary_condition_in = in port BoundaryCondition()
  val is_error_systolic_in = in Bool()
  val weight_out = out reg(UInt(link_bit_width bits))
  val boundary_condition_out = out port reg(BoundaryCondition())
  /* logic */
}
