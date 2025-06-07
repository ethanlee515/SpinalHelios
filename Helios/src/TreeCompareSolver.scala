import spinal.core._
import spinal.lib._

class MinValWithIndex(
  width: Int = 8,
  num_channels: Int = 8
) extends Component {  
  // IO
  val val1 = in UInt(width bits)
  val val2 = in UInt(width bits)
  val valid1 = in Bits(num_channels bits)
  val valid2 = in Bits(num_channels bits)
  val valid = out Bits(2 * num_channels bits)
  val min_val = out UInt(width bits)
  // logic
  val anyValid1 = valid1.orR
  val anyValid2 = valid2.orR
  val valid1_out = valid(num_channels - 1 downto 0)
  val valid2_out = valid(2 * num_channels - 1 downto num_channels)
  val invalid_out = B(0, num_channels bits)
  when(anyValid1 && anyValid2) {
    when(val1 < val2) {
      min_val := val1
      valid1_out := valid1
      valid2_out := invalid_out
    } otherwise {
      min_val := val2
      valid1_out := invalid_out
      valid2_out := valid2
    }
  } otherwise {
    when(anyValid1) {
      min_val := val1
      valid1_out := valid1
      valid2_out := invalid_out
    } otherwise {
      min_val := val2
      valid1_out := invalid_out
      valid2_out := valid2
    }
  }
}

class MinVal4xWithIndex(
  width: Int = 8,
  num_channels: Int = 8
) extends Component {
  // IO
  val vals = in UInt(4 * width bits)
  val valid = in Bits(4 * num_channels bits)
  val min_val = out UInt(width bits)
  val min_valid = out Bits(4 * num_channels bits)
  // logic
  val left = new MinValWithIndex(width, num_channels)
  left.val1 := vals(width - 1 downto 0)
  left.val2 := vals(2 * width - 1 downto width)
  left.valid1 := valid(num_channels - 1 downto 0)
  left.valid2 := valid(2 * num_channels - 1 downto num_channels)
  val right = new MinValWithIndex(width, num_channels)
  right.val1 := vals(3 * width - 1 downto 2 * width)
  right.val2 := vals(4 * width - 1 downto 3 * width)
  right.valid1 := valid(3 * num_channels - 1 downto 2 * num_channels)
  right.valid2 := valid(4 * num_channels - 1 downto 3 * num_channels)
  val minAll = new MinValWithIndex(width, 2 * num_channels)
  minAll.val1 := left.min_val
  minAll.val2 := right.min_val
  minAll.valid1 := left.valid
  minAll.valid2 := right.valid
  min_val := minAll.min_val
  min_valid := minAll.valid
}

class MinVal8xWithIndex(
  width: Int = 8,
  num_channels: Int = 8
) extends Component {
  // IO
  val vals = in UInt(8 * width bits)
  val valid = in Bits(8 * num_channels bits)
  val min_val = out UInt(width bits)
  val min_valid = out Bits(8 * num_channels bits)
  // logic
  val left = new MinVal4xWithIndex(width, num_channels)
  left.vals := vals(4 * width - 1 downto 0)
  left.valid := valid(4 * num_channels - 1 downto 0)
  val right = new MinVal4xWithIndex(width, num_channels)
  right.vals := vals(8 * width - 1 downto 4 * width)
  right.valid := valid(8 * num_channels - 1 downto 4 * num_channels)
  val minAll = new MinValWithIndex(width, 4 * num_channels)
  minAll.val1 := left.min_val
  minAll.val2 := right.min_val
  minAll.valid1 := left.valid
  minAll.valid2 := right.valid
  min_val := minAll.min_val
  min_valid := minAll.valid
}

object VerilogTest extends App {
  SpinalVerilog { new MinVal8xWithIndex() }
}

