import scala.io.Source
import utest._

object HelloTest extends TestSuite {
  val filename =
    "ext/Helios_scalable_QEC/test_benches/test_data/input_data_3_rsc.txt"

  val grid_width_x = 4
  val grid_width_z = 1
  val grid_width_u = 3

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
        val flat_index = i * grid_width_z + j + k * grid_width_x * grid_width_z
        shot(flat_index)
      }
    })
    grids
  }

  def parseOutput(filename: String) = {
    val lines = Source.fromFile(filename).getLines().toList
    assert(lines.length == 13000)
    val grids = lines.map(line => {
      Seq.tabulate(3, 4, 1) { (k, i, j) =>
        val u = Integer.parseInt(line.substring(2, 4), 16)
        val x = Integer.parseInt(line.substring(4, 6), 16)
        val z = Integer.parseInt(line.substring(6, 8), 16)
        (u, x, z)
      }
    })
  }

  def tests = Tests {
    test("hello") {      
      val grid = parseInput(filename)
      println(grid(0))
      println(grid(0x15))
      println(grid(17))
      assert(55 + 45 == 100)
    }
  }
}
