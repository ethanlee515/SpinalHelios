import spinal.core._
import spinal.lib._
import HeliosParams._

class UnifiedController() extends Component {
  val pu_count = grid_width_x * grid_width_z * grid_width_u
  /* IO and states */
  val global_stage = out port Reg(Stage()) init(Stage.measurement_preparing)
  val global_stage_previous = Reg(Stage()) init(Stage.measurement_preparing)
  val odd_clusters_PE = in port Bits(pu_count bits)
  val busy_PE = in port Bits(pu_count bits)
  val measurements =
    out port Reg(Vec.fill(grid_width_x, grid_width_z)(Bool()))
  // rewriting input as streams backed by registers
  val meas_in = slave Stream(Vec.fill(grid_width_x, grid_width_z)(Bool()))
  // This actually corresponds to `output_fifo_valid`
  // FIFO and serializer scrapped, and payload is redundant.
  val output_valid = out port Reg(Bool()) init(False)
  val iteration_counter = Reg(UInt(iteration_counter_width bits))
  val cycle_counter = Reg(UInt(32 bits)) init(0)
  val busy = Reg(Bool())
  val odd_clusters = Reg(Bool())
  /* logic */
  meas_in.ready := (global_stage === Stage.measurement_preparing)
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
  val measurement_rounds = Reg(UInt(16 bits)) init(0)
  switch(global_stage) {
    is(Stage.measurement_preparing) {
      when(meas_in.fire) {
        measurements := meas_in.payload
        global_stage := Stage.measurement_loading
        delay_counter := 0
        measurement_rounds := measurement_rounds + 1
      }
    }
    is(Stage.measurement_loading) {
      global_stage := (measurement_rounds < grid_width_u) ?
        Stage.measurement_preparing | Stage.grow
      delay_counter := 0
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
      global_stage := Stage.measurement_preparing
      measurement_rounds := 0
    }
  }
  output_valid := (global_stage === Stage.result_valid)
}
