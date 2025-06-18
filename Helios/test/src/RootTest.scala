import scala.io.Source
import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utest.assert

object RootTest extends TestSuite {
  val input_filename =
    "ext/Helios_scalable_QEC/test_benches/test_data/input_data_3_rsc.txt"
  val output_filename =
    "ext/Helios_scalable_QEC/test_benches/test_data/output_data_3_rsc.txt"

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

  val input_data = parseInput(input_filename)

  def parseOutputLine(line: String) : Address = {
    val k = Integer.parseInt(line.substring(2, 4), 16)
    val i = Integer.parseInt(line.substring(4, 6), 16)
    val j = Integer.parseInt(line.substring(6, 8), 16)
    Address(k, i, j)
  }

  def parseOutputShot(shot: Seq[String]) : Seq[Seq[Seq[Address]]] = {
    assert(shot.length == 12)
    Seq.tabulate(3, 4, 1) { (k, i, j) =>
      val flat_index = i * grid_width_z + j + k * grid_width_x * grid_width_z
      val line = shot(flat_index)
      parseOutputLine(line)
    }
  }

  def parseOutput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == 13000)
    val shots = Seq.tabulate(1000, 12) { (i, j) =>
      lines(13 * i + j + 1)
    }
    val grids = shots.map(parseOutputShot)
    grids
  }
  
  val output_data = parseOutput(output_filename)

  def tests = Tests {
    test("checking roots against test data") {
      var ctr = 0
      SimConfig.compile {
        val dut = new FlattenedHelios
        dut.core.controller.global_stage.simPublic()
        dut.core.controller.measurement_rounds.simPublic()
        dut
      }.doSim { dut =>
        val driver = new Driver(dut)
        driver.init()
        driver.do_shot(input_data(0))
        val roots = driver.read_roots()
        assert(roots == output_data(0))
      } 
    }
  }
}
