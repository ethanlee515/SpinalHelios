import spinal.core._
import spinal.lib._
import spinal.core.sim._
import HeliosParams._

class Driver(dut: FlattenedHelios) {
  val cd = dut.clockDomain
  def init() = {
    dut.start #= false
    dut.meas_in_valid #= false
    cd.forkStimulus(10)
    cd.assertReset()
    sleep(100)
    cd.deassertReset()
    cd.waitSampling(3)
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
    dut.start #= true
    cd.waitSampling()
    dut.start #= false
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
    // for(_ <- 0 until 15) {
    //   cd.waitSampling()
    //   println(f"t = ${simTime()}, stage = ${dut.core.controller.global_stage.toEnum}")
    // }
    // assert(!dut.meas_in_ready.toBoolean)
    assert(!cd.waitSamplingWhere(1000) { dut.output.valid.toBoolean })
  }

  def read_roots() : Seq[Seq[Seq[Address]]] = {
    assert(dut.output.valid.toBoolean)
    Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      Address(dut.roots(k)(i)(j).toInt)
    }
  }
}

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