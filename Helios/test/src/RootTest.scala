package helios
package test

import scala.io.Source
import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utest.assert

object RootTest extends TestSuite {
  val params = HeliosParams()
  import params._

  val input_filename =
    f"ext/Helios_scalable_QEC/test_benches/test_data/input_data_${code_distance}_rsc.txt"
  val output_filename =
    f"ext/Helios_scalable_QEC/test_benches/test_data/output_data_${code_distance}_rsc.txt"

  def tests = Tests {
    test("checking roots against test data") {
      SimConfig.compile {
        new FlattenedHelios(params)
      }.doSim { dut =>
        val driver = new HeliosDriver(dut)
        import driver._

        init()
        val input_data = parseInput(input_filename)
        def parseOutputLine(line: String) : Address = {
          val k = Integer.parseInt(line.substring(2, 4), 16)
          val i = Integer.parseInt(line.substring(4, 6), 16)
          val j = Integer.parseInt(line.substring(6, 8), 16)
          Address(k, i, j)
        }
        def parseOutputShot(shot: Seq[String]) : Seq[Seq[Seq[Address]]] = {
          assert(shot.length == grid_size)
          Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) { (k, i, j) =>
            val flat_index = i * grid_width_z + j + k * grid_width_x * grid_width_z
            val line = shot(flat_index)
            parseOutputLine(line)
          }
        }
        def parseOutput(filename: String) = {
          val lines = Source.fromFile(filename).getLines().toList
          assert(lines.length == 100 * (grid_size + 1))
          val shots = Seq.tabulate(100, grid_size) { (i, j) =>
            lines((grid_size + 1) * i + j + 1)
          }
          val grids = shots.map(parseOutputShot)
          grids
        }
        val output_data = parseOutput(output_filename)

        for(i <- 0 until input_data.length) {
          val shot = input_data(i)
          driver.do_shot(shot)
          val roots = driver.read_roots()
          // println(f"roots = $roots")
          // println(f"output(${i}) = ${output_data(i)}")
          // assert(roots == output_data(i))
          //println(f"i = $i")
          assert(roots == output_data(i))
          /*
          if(roots != output_data(i)) {
            difference_count = difference_count + 1
          }
          if(HeliosDriver.syndromeNonzero(shot)) {
            nonzero_count = nonzero_count + 1
          }
          */
        }
        /*
        println(f"input(4) = ${input_data(4)}")
        println(f"expected output(4) = ${output_data(4)}")
        println(f"actual output(4) = ${driver.read_roots()}")
        */
        /*
        println(f"input = ${input_data(2)}")
        val expected = output_data(3)
        //println(f"expected output(0) = ${expected}")
        driver.do_shot(input_data(3))
        val roots = driver.read_roots()
//        println(f"actual output(0) = ${roots}")
        assert(roots == expected)
        */
      } 
    }
  }
}
