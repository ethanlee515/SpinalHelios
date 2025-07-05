package helios
import spinal.core._
import spinal.lib._

class MinValWithIndex(
  width: Int = 8,
  num_channels: Int = 1
) extends Component {  
  // IO
  val val0 = in UInt(width bits)
  val val1 = in UInt(width bits)
  val valid0 = in Bits(num_channels bits)
  val valid1 = in Bits(num_channels bits)
  val valid = out Bits(2 * num_channels bits)
  val min_val = out UInt(width bits)
  // logic
  val anyValid0 = valid0.orR
  val anyValid1 = valid1.orR
  val valid0_out = valid(num_channels - 1 downto 0)
  val valid1_out = valid(2 * num_channels - 1 downto num_channels)
  when(anyValid0 && anyValid1) {
    when(val0 < val1) {
      min_val := val0
      valid0_out := valid0
      valid1_out := B(0)
    } otherwise {
      min_val := val1
      valid0_out := B(0)
      valid1_out := valid1
    }
  } otherwise {
    when(anyValid0) {
      min_val := val0
      valid0_out := valid0
      valid1_out := B(0)
    } otherwise {
      min_val := val1
      valid0_out := B(0)
      valid1_out := valid1
    }
  }
}

class MinVal4xWithIndex(width: Int = 8) extends Component {
  // IO
  val vals = in port Vec.fill(4)(Flow(UInt(width bits)))
  val min_val = out UInt(width bits)
  val min_valid = out Bits(4 bits)
  // logic
  val left = new MinValWithIndex(width)
  left.val0 := vals(0).payload
  left.val1 := vals(1).payload
  left.valid0(0) := vals(0).valid
  left.valid1(0) := vals(1).valid
  val right = new MinValWithIndex(width)
  right.val0 := vals(2).payload
  right.val1 := vals(3).payload
  right.valid0(0) := vals(2).valid
  right.valid1(0) := vals(3).valid
  val minAll = new MinValWithIndex(width, 2)
  minAll.val0 := left.min_val
  minAll.val1 := right.min_val
  minAll.valid0 := left.valid
  minAll.valid1 := right.valid
  min_val := minAll.min_val
  min_valid := minAll.valid
}

class MinVal8xWithIndex(width: Int = 8) extends Component {
  // IO
  val vals = in port Vec.fill(8)(Flow(UInt(width bits)))
  val min_val = out UInt(width bits)
  val min_valid = out Bits(8 bits)
  /* -- logic -- */
  val left = new MinVal4xWithIndex(width)
  val right = new MinVal4xWithIndex(width)
  // can't slice vectors...?
  for(i <- 0 until 4) {
    left.vals(i) := vals(i)
    right.vals(i) := vals(i + 4)
  }
  val minAll = new MinValWithIndex(width, 4)
  minAll.val0 := left.min_val
  minAll.val1 := right.min_val
  minAll.valid0 := left.min_valid
  minAll.valid1 := right.min_valid
  min_val := minAll.min_val
  min_valid := minAll.valid
}

class MinValLess8xWithIndex(
  width: Int = 8,
  num_channels: Int = 6
) extends Component {
  assert(num_channels <= 8)
  /* IO */
  val values = in port Vec.fill(num_channels)(Flow(UInt(width bits)))
  val result = out UInt(width bits)
  val valids = out Bits(num_channels bits)
  /* logic */
  val comp = new MinVal8xWithIndex(width)
  for(i <- 0 until 8) {
    if(i < num_channels) {
      comp.vals(i) := values(i)
    } else {
      comp.vals(i).setIdle()
    }
  }
  result := comp.min_val
  for(i <- 0 until num_channels) {
    valids(i) := comp.min_valid(i)
  }
}
