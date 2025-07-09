package helios
import spinal.core._
import spinal.lib._

class HeliosCore(params: HeliosParams) extends Component {
  import params._
  /* IO */
  val meas_in = slave Stream(Vec.fill(grid_width_x, grid_width_z)(Bool()))
  // TODO maybe make this debug `simPublic`` instead of `out port`
  val corrections = out port Flow(Correction(params))
  val output = out port Flow(Vec.fill(code_distance, code_distance)(Bool()))
  /* logic */
  val graph = new DecodingGraph(params)
  val controller = new UnifiedController(params)
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
  corrections.payload := graph.correction
  corrections.valid := controller.output_valid
  val flattener = new CorrectionFlattener(params)
  flattener.layered_correction << corrections
  output << flattener.flat_correction
}

object CompileVerilog extends App {
  SpinalVerilog(new HeliosCore(HeliosParams()))
}
