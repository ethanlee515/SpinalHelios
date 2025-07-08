package helios
package test

import scala.io.Source
import spinal.core._
import spinal.lib._
import spinal.core.sim._

class HeliosDriver(dut: FlattenedHelios) {
  val cd = dut.clockDomain
  import dut.params._
  def init() = {
    dut.meas_in_valid #= false
    cd.forkStimulus(10)
    cd.assertReset()
    sleep(100)
    cd.deassertReset()
    assert(!cd.waitSamplingWhere(1000) { dut.meas_in_ready.toBoolean })
  }

  def getCorrection(k: Int, flatIndex: Int) : Bool = {
    val ns_len = (grid_width_x - 1) * grid_width_z
    val ew_len = (grid_width_x - 1) * grid_width_z + 1
    if(flatIndex < ns_len) {
      val i = (flatIndex / grid_width_z) + 1
      val j = (flatIndex % grid_width_z) + 1
      return dut.ns(k, i, j)
    } else if(flatIndex < ns_len + ew_len - 1) {
      val ew_index = flatIndex - ns_len
      val i = (ew_index / grid_width_z) + 1
      val j = ew_index % grid_width_z
      return dut.ew(k, i, j)
    } else if(flatIndex == ns_len + ew_len - 1) {
      val i = grid_width_x - 1
      val j = grid_width_z
      return dut.ew(k, i, j)
    } else {
      val ud_index = flatIndex - ns_len - ew_len
      val i = ud_index / grid_width_z
      val j = ud_index % grid_width_z
      return dut.ud(k, i, j)
    }
  }

  def log_stage() = {
    val stage = dut.core.controller.global_stage.toEnum
    println(f"stage = ${stage} at t = ${simTime()}")
  }

  def do_shot(meas_in: Seq[Seq[Seq[Boolean]]]) = {
    //println(f"shot started at t = ${simTime()}")
    for(k <- 0 until grid_width_u) {
      assert(!cd.waitSamplingWhere(1000) { dut.meas_in_ready.toBoolean })
      dut.meas_in_valid #= true
      for(i <- 0 until grid_width_x;
          j <- 0 until grid_width_z) {
        dut.meas_in_data(i)(j) #= meas_in(k)(i)(j)
      }
      cd.waitSampling()
      dut.meas_in_valid #= false
    }
    assert(!cd.waitSamplingWhere(1000) { dut.output_valid.toBoolean })
  }

  def read_roots() : Seq[Seq[Seq[Address]]] = {
    Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      val root = dut.core.graph.processing_unit(k)(i)(j).root
      Address.unpack(root.toInt)
    }
  }

  def log_busy() = {
    val busys = Seq.tabulate(
      grid_width_u, grid_width_x, grid_width_z
    ) { (k, i, j) =>
      dut.core.graph.processing_unit(k)(i)(j).busy.toBoolean
    }
    println(f"busy = ${busys}")
  }

  def log_solver_valids() = {
    val in_valids = Seq.tabulate(
      grid_width_u, grid_width_x, grid_width_z, neighbor_count
    ) { (k, i, j, h) =>
      dut.core.graph.processing_unit(k)(i)(j).solver.values(h).valid.toBoolean
    }
    val out_valids = Seq.tabulate(
      grid_width_u, grid_width_x, grid_width_z
    ) { (k, i, j) =>
      dut.core.graph.processing_unit(k)(i)(j).solver.valids.toInt
    }
    println(f"solver in valids = ${in_valids}")
    println(f"solver out valids = ${out_valids}")
  }

  def log_roots() = {
    println(read_roots())
  }

  def parseInput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == 100 * (grid_size + 1))
    val shots = Seq.tabulate(100, grid_size) { (i, j) =>
      val line = lines((grid_size + 1) * i + j + 1)
      assert(line == "00000000" || line == "00000001")
      line == "00000001"
    }
    val grids = shots.map(shot => {
      Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
        shot(Address(k, i, j).index)
      }
    })
    grids
  }

  def syndromeNonzero(shot: Seq[Seq[Seq[Boolean]]]) : Boolean = {
    for(a <- shot;
        b <- a;
        c <- b) {
          if(c) {
            return true
          }
        }
    return false
  }

  case class Address(k: Int, i: Int, j: Int) {
    def index = {
      k * grid_width_x * grid_width_z + i * grid_width_z + j
    }
  }

  object Address {
    def fromIndex(flatIndex: Int) : Address = {
      val j = flatIndex % grid_width_z
      val ki = flatIndex / grid_width_z
      val i = ki % grid_width_x
      val k = ki / grid_width_x
      Address(k, i, j)
    }

    def unpack(packed: Int) : Address = {
      val j = packed & ((1 << z_bit_width) - 1)
      val ki = packed >> z_bit_width
      val i = ki & ((1 << x_bit_width) - 1)
      val k = ki >> x_bit_width
      Address(k, i, j)
    }
  }
}
