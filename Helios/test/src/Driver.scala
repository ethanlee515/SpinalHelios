import scala.io.Source
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import HeliosParams._

object HeliosDriver {
  // Poor man's whiteboxer
  def simPublics(dut: FlattenedHelios) = {
    dut.core.controller.global_stage.simPublic()
    dut.core.controller.measurement_rounds.simPublic()
    for(k <- 0 until grid_width_u;
        i <- 0 until grid_width_x;
        j <- 0 until grid_width_z) {
      dut.core.graph.processing_unit(k)(i)(j).busy.simPublic()
      dut.core.graph.processing_unit(k)(i)(j).solver.valids.simPublic()
      dut.core.graph.processing_unit(k)(i)(j).root.simPublic()
      for(h <- 0 until neighbor_count) {
        dut.core.graph.processing_unit(k)(i)(j).solver.values(h).simPublic()
      }
    }
    dut.core.output.payload.ns_tail.simPublic()
    dut.core.output.payload.ew_tail.simPublic()
    dut.core.output.payload.ew_last.simPublic()
    dut.core.output.payload.ud_tail.simPublic()
  }

  def parseInput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == 13000)
    val shots = Seq.tabulate(1000, 12) { (i, j) =>
      val line = lines(13 * i + j + 1)
      assert(line == "00000000" || line == "00000001")
      line == "00000001"
    }
    val grids = shots.map(shot => {
      Seq.tabulate(3, 4, 1) { (k, i, j) =>
        shot(Address(k, i, j).flatIndex)
      }
    })
    grids
  }
}

class HeliosDriver(dut: FlattenedHelios) {
  val cd = dut.clockDomain
  def init() = {
    // dut.command_valid #= false
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
    /*
    for(_ <- 0 until 15) {
      cd.waitSampling()
      log_stage()
      log_roots()
      log_busy()
      log_solver_valids()
      println()
    }
    */
    assert(!cd.waitSamplingWhere(1000) { dut.output_valid.toBoolean })
  }

  def read_roots() : Seq[Seq[Seq[Address]]] = {
    Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
      val root = dut.core.graph.processing_unit(k)(i)(j).root
      Address(root.toInt)
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
}

case class Address(k: Int, i: Int, j: Int) {
// println(f"x width = ${x_bit_width}, z width = ${z_bit_width}")
  def flatIndex = {
    val res = (k << (x_bit_width + z_bit_width)) + (i << z_bit_width) + j
    // println(f"flat index of ($k, $i, $j) = $res")
    res
  }
}

object Address {
  def apply(flatIndex: Int) : Address = {
    val j = flatIndex & ((1 << z_bit_width) - 1)
    val ki = flatIndex >> z_bit_width
    val i = ki & ((1 << x_bit_width) - 1)
    val k = ki >> x_bit_width
    Address(k, i, j)
  }
}