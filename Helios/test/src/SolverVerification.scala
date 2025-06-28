import utest._
import HeliosParams._
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import spinal.core.formal._
import spinal.core.assert

object SolverParams {
  val data_width = 8
  val channel_count = 6
}

import SolverParams._

class BoxedSolver extends BlackBox {
  val io = new Bundle {
    val values = in port Bits(data_width * channel_count bits)
    val valids = in port Bits(channel_count bits)
    val result = out port Bits(data_width bits)
    val output_valids = out port Bits(channel_count bits)
  }
  noIoPrefix()
  addRTLPath("./ext/Helios_scalable_QEC/design/tree_compare_solver.sv")
  setBlackBoxName("min_val_less_8x_with_index")
}

class SolverChecker extends Component {
  val values = in port Vec.fill(channel_count)(UInt(data_width bits))
  val valids = in port Bits(channel_count bits)
  val outputs_equal = out port Bool()
  val solver = new MinValLess8xWithIndex()
  for(i <- 0 until channel_count) {
    solver.values(i).valid := valids(i)
    solver.values(i).payload := values(i)
  }
  val reference = new BoxedSolver()
  reference.io.values := values.asBits
  reference.io.valids := valids
  val valids_all_eq = solver.valids === reference.io.output_valids
  val res_eq = solver.result === reference.io.result.asUInt
  outputs_equal := valids_all_eq & ((!solver.valids.orR) || res_eq)
}

object SolverTest extends TestSuite {
  def tests = Tests {
    test("Checking tree solver using SymbiYosys") {
      FormalConfig.withBMC(15).doVerify(new Component {
        val dut = FormalDut(new SolverChecker())
        anyconst(dut.values)
        anyconst(dut.valids)
        assert(dut.outputs_equal)
      })
    }
  }
}