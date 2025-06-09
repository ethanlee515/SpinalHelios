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
  val start_decoding_message = B(1, 8 bits)
  val measurement_data_header = B(2, 8 bits)
  /* IO and states */
  val global_stage = out port Reg(Stage()) init(Stage.idle)
  val global_stage_previous = Reg(Stage()) init(Stage.idle)
  val odd_clusters_PE = in port Bits(pu_count bits)
  val busy_PE = in port Bits(pu_count bits)
  val measurements = out port Reg(Bits(aligned_pu_per_round bits))
  // rewriting as streams backed by registers
  val input = slave Stream(Bits(8 bits))
  val input_ready = Reg(Bool()) init(False)
  input.ready := input_ready
  // This corresponds to `output_fifo_valid`
  // FIFO and serializer scrapped, and payload is redundant.
  val output_valid = out port Reg(Bool()) init(False)
  val result_valid = Reg(Bool()) init(False) // TODO what is this? 
  val iteration_counter = Reg(UInt(iteration_counter_width bits))
  val cycle_counter = Reg(UInt(32 bits)) init(0)
  val busy = Reg(Bool())
  val odd_clusters = Reg(Bool())
  /* logic */
  input_ready := global_stage === Stage.idle ||
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

  switch(global_stage) {
    is(Stage.idle) {
      when(input.valid && input_ready) {
        when(input.payload === start_decoding_message) {
          global_stage := Stage.parameters_loading
          delay_counter := 0
          result_valid := False
        }
        when(input.payload === measurement_data_header){
          global_stage := Stage.measurement_preparing
          delay_counter := 0
          result_valid := False
          measurement_rounds := 0
        }
      }
    }
    is(Stage.parameters_loading) {
      global_stage := Stage.idle
      messages_per_round_of_measurement := 0
      measurement_rounds := 0
    }
    is(Stage.measurement_preparing) {
      when(input.valid && input_ready) {
        val first_byte = (aligned_pu_per_round - 1 downto
          aligned_pu_per_round - 8)
        measurements(first_byte) := input.payload
        if(aligned_pu_per_round > 8) {
          val src = aligned_pu_per_round - 1 downto 8
          val dest = aligned_pu_per_round - 9 downto 0
          measurements(dest) := measurements(src)
        }
        messages_per_round_of_measurement :=
          messages_per_round_of_measurement + 1
        when((messages_per_round_of_measurement + 1) * 8 >=
          pu_count_per_round) {
          global_stage := Stage.measurement_loading
          delay_counter := 0
          messages_per_round_of_measurement := 0
          measurement_rounds := measurement_rounds + 1
        }
      }
    }
    is(Stage.measurement_loading) {
      global_stage := (measurement_rounds < grid_width_u) ?
        Stage.measurement_preparing | Stage.grow
      delay_counter := 0
      result_valid := False
    }
    is(Stage.grow) {
      global_stage := Stage.merge
      delay_counter := 0
      measurement_rounds := 0
    }
    is(Stage.merge) {
      when(delay_counter >= max_delay) {
        when(!busy) {
          global_stage := !odd_clusters ? Stage.peeling | Stage.grow
          delay_counter := 0
        }
      } otherwise {
        delay_counter := delay_counter + 1
      }
    }
    is(Stage.peeling) {
      when(delay_counter >= max_delay) {
        when(!busy) {
          global_stage := Stage.result_valid
          delay_counter := 0
        }
      } otherwise {
        delay_counter := delay_counter + 1
      }
    }
    is(Stage.result_valid) {
      measurement_rounds := measurement_rounds + 1
      when(measurement_rounds >= grid_width_u - 1) {
        global_stage := Stage.idle
      }
      delay_counter := 0
      result_valid := True
    }
  }
  output_valid := (global_stage === Stage.result_valid)
}

object ControllerVerilog extends App {
  SpinalVerilog(new UnifiedController())
}
