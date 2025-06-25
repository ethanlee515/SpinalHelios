import spinal.core._
import spinal.lib._

class SolverBox extends BlackBox {
  val data_width = 8
  val channel_count = 6
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
  val data_width = 8
  val channel_count = 6
  val values = in port Vec.fill(channel_count)(UInt(data_width bits))
  val valids = in port Bits(channel_count bits)
  val result = out port Bits(data_width bits)
  val r0 = out port Bool()
  r0 := result(0)
  val output_valids = out port Bits(channel_count bits)
  val solver = new SolverBox()
  for(i <- 0 until channel_count) {
    solver.io.values(((i + 1) * data_width - 1) downto (i * data_width)) := values(i).asBits
  }
  solver.io.valids := valids
  result := solver.io.result
  output_valids := solver.io.output_valids
}
