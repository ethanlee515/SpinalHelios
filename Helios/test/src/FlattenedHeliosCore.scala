package helios
package test

// Flattening the input Stream[Vec] in HeliosCore for simulation
// Vectors don't seem to work cleanly for simulations.
// Having trouble setting elements, even with `simPublic()` everywhere

import spinal.core._
import spinal.lib._
import HeliosParams._
import spinal.core.sim._

class FlattenedHelios(val params: HeliosParams) extends Component {
  import params._
  val core = new HeliosCore(params)
  val meas_in_ready = out Bool()
  meas_in_ready := core.meas_in.ready
  val meas_in_valid = in Bool()
  core.meas_in.valid := meas_in_valid
  val meas_in_data = Seq.tabulate(grid_width_x, grid_width_z) { (i, j) =>
    val mij = in Bool()
    core.meas_in.payload(i)(j) := mij
    mij
  }

  // flattening the correction output flow
  // apparently simPublic doesn't work on vec/bits...
  val output_valid = out Bool()
  output_valid := core.output.valid

  val ns_indices = {
    for(k <- 0 until grid_width_u;
        i <- 1 until grid_width_x;
        j <- 1 to grid_width_z) yield (k, i, j)
  }
  val ns = ns_indices.map { case (k, i, j) =>
    val b = out Bool()
    b := core.output.payload.ns(k, i, j)
    (k, i, j) -> b
  }.toMap
  val ew_indices = {
    val s1 = {
      for(k <- 0 until grid_width_u;
          i <- 1 until grid_width_x;
          j <- 0 until grid_width_z) yield (k, i, j)
    }
    val s2 = {
      val i = grid_width_x - 1
      val j = grid_width_z
      for(k <- 0 until grid_width_u) yield (k, i, j)
    }
    s1 ++ s2
  }
  val ew = ew_indices.map { case (k, i, j) =>
    val b = out Bool()
    b := core.output.payload.ew(k, i, j)
    (k, i, j) -> b
  }.toMap
  val ud_indices = {
  for(k <- 0 until grid_width_u;
      i <- 0 until grid_width_x;
      j <- 0 until grid_width_z) yield (k, i, j)
  }
  val ud = ud_indices.map { case (k, i, j) =>
    val b = out Bool()
    b := core.output.payload.ud(k, i, j)
    (k, i, j) -> b
  }.toMap

  // Calling `simPublic` on whatever needed
  core.controller.global_stage.simPublic()
  core.controller.measurement_rounds.simPublic()
  for(k <- 0 until grid_width_u;
      i <- 0 until grid_width_x;
      j <- 0 until grid_width_z) {
    core.graph.processing_unit(k)(i)(j).busy.simPublic()
    core.graph.processing_unit(k)(i)(j).solver.valids.simPublic()
    core.graph.processing_unit(k)(i)(j).root.simPublic()
    for(h <- 0 until neighbor_count) {
      core.graph.processing_unit(k)(i)(j).solver.values(h).simPublic()
    }
  }
  core.output.payload.ns_tail.simPublic()
  core.output.payload.ew_tail.simPublic()
  core.output.payload.ew_last.simPublic()
  core.output.payload.ud_tail.simPublic()
}
