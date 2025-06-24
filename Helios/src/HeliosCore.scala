import spinal.core._
import spinal.lib._
import HeliosParams._

class HeliosCore() extends Component {
  val meas_in = slave Stream(Vec.fill(grid_width_x, grid_width_z)(Bool()))
  val output = out port Flow(Correction())
  val graph = new DecodingGraph()
  val controller = new UnifiedController()
  controller.meas_in << meas_in
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
