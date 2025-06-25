import scala.io.Source
import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utest.assert

object CorrectionTest extends TestSuite {
  val input_filename =
    "ext/Helios_scalable_QEC/test_benches/test_data/input_data_7_rsc.txt"
  val output_filename =
    "ext/Helios_scalable_QEC/test_benches/test_data/corrections.txt"

  // val input_data = HeliosDriver.parseInput(input_filename)

  val ns_len = (grid_width_x - 1) * grid_width_z
  val ew_len = (grid_width_x - 1) * grid_width_z + 1
  val ud_len = grid_width_x * grid_width_z
  val corrections_per_layer = ns_len + ew_len + ud_len

  def parseOutputLine(line: String) : Seq[Boolean] = {
    val v = Integer.parseInt(line.trim, 16)
    Seq.tabulate(corrections_per_layer) {i =>
      (v >> i) == 1
    }
  }

  def parseOutputShot(shot: Seq[String]) : Seq[Seq[Boolean]] = {
    assert(shot.length == 3)
    shot.map(parseOutputLine)
  }

  def parseOutput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == 3000)
    val shots = Seq.tabulate(1000, 3) { (i, j) =>
      lines(3 * i + j)
    }
    shots.map(parseOutputShot)
  }
  
  // indices: (shot, layer, flattened x-z)
  // val output_data : Seq[Seq[Seq[Boolean]]] = parseOutput(output_filename)

  def tests = Tests {
    test("checking corrections against test data") {
      /*
      SimConfig.compile {
        val dut = new FlattenedHelios
        HeliosDriver.simPublics(dut)
        dut
      }.doSim { dut =>
        val driver = new HeliosDriver(dut)
        driver.init()
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
    */
    }
  }
}
