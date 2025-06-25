import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utest.assert

object SolverTest extends TestSuite {
  def tests = Tests {
    test("trying out wrapped solver") {
      SimConfig.compile { new WrappedSolver }.doSim { dut =>
      /*
        for(i <- 0 until 6) {
            dut.valids(i) #= false
        }
        */
        dut.valids #= 0
        sleep(1)
        println(f"out valids = ${dut.r0.toBoolean}")
      }
    }

    test("compiling wrapped solver") {
        SpinalVerilog { new WrappedSolver }
    }
  }
}