// Flattening the input Stream[Vec] in HeliosCore for simulation
// Vectors don't seem to work cleanly for simulations.
// Having trouble setting elements, even with `simPublic()` everywhere

import spinal.core._
import spinal.lib._
import HeliosParams._

class FlattenedHelios() extends Component {
  val core = new HeliosCore()
  val meas_in_ready = out Bool()
  meas_in_ready := core.meas_in.ready
  val meas_in_valid = in Bool()
  core.meas_in.valid := meas_in_valid
  val meas_in_data = Seq.tabulate(grid_width_x, grid_width_z) { (i, j) =>
    val mij = in Bool()
    core.meas_in.payload(i)(j) := mij
    mij
  }
  /* TODO this is nonsense.
   * The parameters can probably be hard-coded instead of loaded. */
   /*
  val command_ready = out Bool()
  command_ready := core.command.ready
  val command_valid = in Bool()
  core.command.valid := command_valid
  val command_payload = in port Command()
  core.command.payload := command_payload
  */
  val output = out port Flow(Correction())
  output << core.output
  val roots = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
    val rij = out UInt(address_width bits)
    rij := core.roots(k)(i)(j)
    rij
  }
}
