// Ported from "processing_unit_single_FPGA_v2.v"

import spinal.core._
import spinal.lib._

class ProcessingUnit(
  address_width: Int = 6,
  neighbor_count: Int = 6,
  address: Int = 0
) extends Component {
  val exposed_data_size = address_width + 1 + 1 + 1 + 1 + 3
  /* IO */
  val measurement = in Bool()
  val measurement_out = out Bool()
  val global_stage = in port Stage()
  val neighbor_fully_grown = in Bits(neighbor_count bits)
//  val neighbor_is_error = out Bits(neighbor_count bits)
  val input_data = in port Vec.fill(neighbor_count)(Bits(exposed_data_size bits))
  val output_data = out port Vec.fill(neighbor_count)(Bits(exposed_data_size bits))
  val root = out port Reg(Bits(address_width bits))
/*
  val odd = out Bool()
  val busy = out Bool()
*/
  /* logic */
  // unpacking `input_data`
  val neighbor_root = Vec(input_data.map(_(0 until address_width)))
  val neighbor_parent_vector = Vec(input_data.map(_(address_width))).asBits
  val parent_odd = Vec(input_data.map(_(address_width + 1))).asBits
  val child_cluster_parity = Vec(input_data.map(_(address_width + 2))).asBits
  val child_touching_boundary = Vec(input_data.map(_(address_width + 3))).asBits
  val child_peeling_complete = Vec(input_data.map(_(address_width + 4))).asBits
  val child_touching_m = Vec(input_data.map(_(address_width + 5))).asBits
  val parent_peeling_parity_completed = Vec(input_data.map(_(address_width + 6))).asBits
  // unpacking `output_data`
  // output looks like a wire, but it's in fact backed by these registers.
  val parent_vector = Reg(Bits(neighbor_count bits))
  val odd_to_children = Reg(Bits(neighbor_count bits))
  val cluster_parity = Reg(Bool())
  val cluster_touching_boundary = Reg(Bool())
  val peeling_complete = Reg(Bool())
  val peeling_m = Reg(Bool())
  val peeling_parity_completed = Reg(Bool())
  for(i <- 0 until neighbor_count) {
    output_data(i)(0 until address_width) := root
    output_data(i)(address_width) := parent_vector(i)
    output_data(i)(address_width + 1) := odd_to_children(i)
    output_data(i)(address_width + 2) := cluster_parity
    output_data(i)(address_width + 3) := cluster_touching_boundary
    output_data(i)(address_width + 4) := peeling_complete
    output_data(i)(address_width + 5) := peeling_m
    output_data(i)(address_width + 6) := peeling_parity_completed
  }
  // book-keeping
  val stage = Reg(Stage()) init(Stage.idle)
  val last_stage = Reg(Stage()) init(Stage.idle)
  stage := global_stage
  last_stage := stage
  // `measurement_out` is also backed by register
  val m = Reg(Bool()) init(False)
  when(stage === Stage.measurement_loading) {
    m := measurement
  }
  measurement_out := m

}

object ProcUnitVerilog extends App {
  SpinalVerilog(new ProcessingUnit())
}
