// Ported from "design/wrappers/Helios_single_FPGA_core.v"

import spinal.core._
import spinal.lib._

class HeliosCore (
  grid_width_x: Int = 4,
  grid_width_z: Int = 1, // is this ever not 1?
  grid_width_u: Int = 3,
  max_weight: Int = 2
) extends Component {
  val meas_in = slave Stream(Vec.fill(grid_width_x)(Bits(grid_width_z bits)))
  val command = slave Stream(Command())
  val output =
    out port Flow(Correction(grid_width_x, grid_width_z, grid_width_u))
  val graph = new DecodingGraph(grid_width_x, grid_width_z, grid_width_u)
  val controller =
    new UnifiedController(grid_width_x, grid_width_z, grid_width_u, 8, 3)
  controller.meas_in << meas_in
  controller.command << command
  graph.measurements := controller.measurements
  for(k <- 0 until grid_width_u;
      i <- 0 until grid_width_x;
      j <- 0 until grid_width_z) {
    val id = k * grid_width_x * grid_width_z + i * grid_width_z + j
    controller.busy_PE(id) := graph.busy(k)(i)(j)
    controller.odd_clusters_PE(id) := graph.odd_clusters(k)(i)(j)
  }
  graph.global_stage := controller.global_stage
  output.payload := graph.correction
  output.valid := controller.output_valid
}

object CompileVerilog extends App {
  SpinalVerilog(new HeliosCore())
}
