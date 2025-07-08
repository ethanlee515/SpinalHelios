package helios
package test

import scala.io.Source
import utest._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utest.assert
import TestParams._

object CorrectionTest extends TestSuite {
  val params = HeliosParams()
  import params._

  val input_filename =
    f"ext/Helios_scalable_QEC/test_benches/test_data/input_data_${code_distance}_rsc.txt"
  val output_filename =
    f"ext/Helios_scalable_QEC/test_benches/test_data/corrections_${code_distance}.txt"

  val ns_len = (grid_width_x - 1) * grid_width_z
  val ew_len = (grid_width_x - 1) * grid_width_z + 1
  val ud_len = grid_width_x * grid_width_z
  val corrections_per_layer = ns_len + ew_len + ud_len

  def parseOutputLine(line: String) : Seq[Boolean] = {
    assert(line.length == (corrections_per_layer + 3) / 4)
    val v = BigInt(line.trim, 16)
    Seq.tabulate(corrections_per_layer) {i =>
      ((v >> i) & 1) == 1
    }
  }

  def parseOutputShot(shot: Seq[String]) : Seq[Seq[Boolean]] = {
    assert(shot.length == grid_width_u)
    shot.map(parseOutputLine)
  }

  def parseOutput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == num_shots * grid_width_u)
    val shots = Seq.tabulate(num_shots, grid_width_u) { (i, j) =>
      lines(grid_width_u * i + j)
    }
    shots.map(parseOutputShot)
  }

  val output_data = parseOutput(output_filename)

  def tests = Tests {
    test("checking corrections against test data") {
      SimConfig.compile {
        val dut = new FlattenedHelios(params)
        // HeliosDriver.simPublics(dut)
        dut
      }.doSim { dut =>
        val driver = new HeliosDriver(dut)
        driver.init()
        val input_data = driver.parseInput(input_filename)
        for(s <- 0 until input_data.length) {
          driver.do_shot(input_data(s))
          for(k <- 0 until grid_width_u;
              i <- 0 until corrections_per_layer) {
            val expected_corr = output_data(s)(k)(i)
            val corr = driver.getCorrection(k, i).toBoolean
            assert(corr == expected_corr)
          }
        }
      } 
    }
  }
}
