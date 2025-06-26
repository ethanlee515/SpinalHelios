import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import spinal.core.formal._
import spinal.core.assert

object SolverTest extends TestSuite {
  def tests = Tests {
    test("Checking tree solver using SymbiYosys") {
      FormalConfig.withBMC(2).doVerify(new Component {
        val dut = FormalDut(new SolverChecker())
        anyconst(dut.values)
        anyconst(dut.valids)
        assert(dut.outputs_equal)
      })
    }
  }
}