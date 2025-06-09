// Ported from "design/stage_controller/control_node_single_FPGA.v"

import spinal.core._
import spinal.lib._

class UnifiedController(
  grid_width_x: Int = 4,
  grid_width_z: Int = 1,
  grid_width_u: Int = 3,
  iteration_counter_width: Int = 8,
  max_delay: Int = 2
) extends Component {
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
  val bytes_per_round = (grid_width_x * grid_width_z + 7) >> 3
  val aligned_pu_per_round = bytes_per_round << 3
  val pu_count_per_round = grid_width_x * grid_width_z
  val pu_count = pu_count_per_round * grid_width_u
  val ns_error_count_per_round = (grid_width_x - 1) * grid_width_z
  val ew_error_count_per_round = (grid_width_x - 1) * grid_width_z + 1
  val ud_error_count_per_round = grid_width_x * grid_width_z
  val correction_count_per_round = ns_error_count_per_round +
    ew_error_count_per_round + ud_error_count_per_round
  /* IO and states */
  val global_stage = out port Reg(Stage()) init(Stage.idle)
  val global_stage_previous = Reg(Stage()) init(Stage.idle)
  val odd_clusters_PE = Bits(pu_count bits)
  val busy_PE = Bits(pu_count bits)
  val measurements = out port Reg(Bits(aligned_pu_per_round bits))
  // rewriting as streams backed by registers
  val input = slave Stream(Bits(8 bits))
  val input_r = Reg(Bool()) init(False)
  input.ready := input_r
  // This corresponds to `output_fifo_valid`
  // FIFO and serializer scrapped, and payload is redundant.
  val output_valid = out port Reg(Bool())
  val result_valid = Reg(Bool()) init(False) // TODO what is this? 
  val iteration_counter = Reg(UInt(iteration_counter_width bits))
  val cycle_counter = Reg(UInt(32 bits)) init(0)
  val busy = Reg(Bool())
  val odd_clusters = Reg(Bool())
  /* logic */
  input_r := global_stage === Stage.idle ||
    global_stage === Stage.measurement_preparing
  busy := busy_PE.orR
  odd_clusters := odd_clusters_PE.orR
  switch(global_stage) {
    is(Stage.measurement_loading) {
      cycle_counter := U(1)
    }
    is(Stage.grow, Stage.merge, Stage.peeling) {
      cycle_counter := cycle_counter + U(1)
    }
  }
  global_stage_previous := global_stage
  val delay_counter = Reg(UInt(log2Up(max_delay + 1) bits)) init(0)
  val messages_per_round_of_measurement = Reg(UInt(16 bits))
  val measurement_rounds = Reg(UInt(16 bits))
/*
  switch(global_stage) {
    // TODO
    ???
  }
*/
  // TODO delete me
  // this is so partial Verilog can be generated
  measurements := B(0)
  output_valid := False
  global_stage := Stage.idle
}

object ControllerVerilog extends App {
  SpinalVerilog(new UnifiedController())
}
