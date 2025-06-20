import spinal.core._
import spinal.lib._
import spinal.core.sim._
import HeliosParams._

object HeliosDriver {
  def simPublics(dut: FlattenedHelios) = {
    dut.core.controller.global_stage.simPublic()
    dut.core.controller.measurement_rounds.simPublic()
    for(k <- 0 until grid_width_u;
        i <- 0 until grid_width_x;
        j <- 0 until grid_width_z) {
      dut.core.graph.processing_unit(k)(i)(j).busy.simPublic()
      dut.core.graph.processing_unit(k)(i)(j).solver.valids.simPublic()
      for(h <- 0 until neighbor_count) {
        dut.core.graph.processing_unit(k)(i)(j).solver.values(h).simPublic()
      }
    }
  }
}

class HeliosDriver(dut: FlattenedHelios) {
  val cd = dut.clockDomain
  def init() = {
    dut.command_valid #= false
    dut.meas_in_valid #= false
    cd.forkStimulus(10)
    cd.assertReset()
    sleep(100)
    cd.deassertReset()
    assert(!cd.waitSamplingWhere(10) {dut.command_ready.toBoolean })
    dut.command_valid #= true
    dut.command_payload #= Command.start_decoding
    cd.waitSampling()
    dut.command_valid #= false
    assert(!cd.waitSamplingWhere(10) {dut.command_ready.toBoolean })
  }

  def log_stage() = {
    println(f"stage = ${dut.core.controller.global_stage.toEnum} at t = ${simTime()}")
  }

  def do_shot(meas_in: Seq[Seq[Seq[Boolean]]]) = {
    //println(f"shot started at t = ${simTime()}")
    // wait until ready
    assert(!cd.waitSamplingWhere(20) { dut.command_ready.toBoolean })
    assert(dut.command_ready.toBoolean)
    // Handshake
    dut.command_valid #= true
    dut.command_payload #= Command.measurement_data
    cd.waitSampling()
    dut.command_valid #= false
    // pass input
    for(k <- 0 until grid_width_u) {
      assert(!cd.waitSamplingWhere(1000) { dut.meas_in_ready.toBoolean })
      assert(!dut.meas_in_valid.toBoolean)
      assert(dut.meas_in_ready.toBoolean)
      dut.meas_in_valid #= true
      for(i <- 0 until grid_width_x;
          j <- 0 until grid_width_z) {
        dut.meas_in_data(i)(j) #= meas_in(k)(i)(j)
      }
      cd.waitSampling()
      dut.meas_in_valid #= false
    }
    println(f"inputs set at t = ${simTime()}")
    // wait until output valid
    for(_ <- 0 until 15) {
      cd.waitSampling()
      log_stage()
      log_roots()
      log_busy()
      log_solver_valids()
      println()
    }
    // assert(!dut.meas_in_ready.toBoolean)
    // assert(!cd.waitSamplingWhere(1000) { dut.output.valid.toBoolean })
  }

  def read_roots() : Seq[Seq[Seq[Address]]] = {
    Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      Address(dut.roots(k)(i)(j).toInt)
    }
  }

  def log_busy() = {
    val busys = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      dut.core.graph.processing_unit(k)(i)(j).busy.toBoolean
    }
    println(f"busy = ${busys}")
  }

  def log_solver_valids() = {
    val in_valids = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z, neighbor_count) { (k, i, j, h) =>
      dut.core.graph.processing_unit(k)(i)(j).solver.values(h).valid.toBoolean
    }
    val out_valids = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      dut.core.graph.processing_unit(k)(i)(j).solver.valids.toInt
    }
    println(f"solver in valids = ${in_valids}")
    println(f"solver out valids = ${out_valids}")
  }

  def log_roots() = {
    println(read_roots())
  }
}

// This is super inconsistent if it should be multiply or bitshift
// they aren't equivalent since these things aren't powers of two
// TODO Maybe can just change things to this multiplication everywhere?
case class Address(k: Int, i: Int, j: Int) {
  def flatIndex = k * grid_width_x * grid_width_z + i * grid_width_z + j
}

object Address {
  def apply(flatIndex: Int) : Address = {
    val j = flatIndex % grid_width_z
    val ki = flatIndex / grid_width_z
    val i = ki % grid_width_x
    val k = ki / grid_width_x
    Address(k, i, j)
  }
}