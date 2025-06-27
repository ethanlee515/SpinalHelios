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
  addRTLPath("./ext/Helios_scalable_QEC/design/generics/tree_compare_solver.sv")
  setBlackBoxName("min_val_less_8x_with_index")
}

class WrappedSolver extends Component {
  val values = in port Vec.fill(channel_count)(UInt(data_width bits))
  val valids = in port Bits(channel_count bits)
  val result = out port UInt(data_width bits)
  val output_valids = out port Bits(channel_count bits)
  val solver = new BoxedSolver()
  for(i <- 0 until channel_count) {
    solver.io.values(((i + 1) * data_width - 1) downto (i * data_width)) := values(i).asBits
  }
  solver.io.valids := valids
  result := solver.io.result.asUInt
  output_valids := solver.io.output_valids
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
  val reference = new WrappedSolver()
  reference.values := values
  reference.valids := valids
  val valids_eqs = Vec.tabulate(channel_count) { i =>
    solver.valids(i) === reference.output_valids(i)
  }
  val valids_all_eq = valids_eqs.asBits.andR
  val res_eq = solver.result === reference.result
  outputs_equal := valids_all_eq & ((!reference.output_valids.orR) || res_eq)
}

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