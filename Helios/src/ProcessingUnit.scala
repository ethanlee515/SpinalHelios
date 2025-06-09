// Ported from "processing_unit_single_FPGA_v2.v"

import spinal.core._
import spinal.lib._

case class InputData(address_width: Int) extends Bundle {
  // unpacking `input_data`
  val neighbor_root = UInt(address_width bits)
  val neighbor_parent_vector = Bool()
  val parent_odd = Bool()
  val child_cluster_parity = Bool()
  val child_touching_boundary = Bool()
  val child_peeling_complete = Bool()
  val child_peeling_m = Bool()
  val parent_peeling_parity_completed = Bool()
}

case class OutputData(address_width: Int) extends Bundle {
  val root = UInt(address_width bits)
  val parent_vector = Bool()
  val odd_to_children = Bool()
  val cluster_parity = Bool()
  val cluster_touching_boundary = Bool()
  val peeling_complete = Bool()
  val peeling_m = Bool()
  val peeling_parity_completed = Bool()
}

class ProcessingUnit(
  address_width: Int = 6,
  neighbor_count: Int = 6,
  address: Int = 0
) extends Component {
  /* IO */
  val measurement = in Bool()
  val measurement_out = out Bool()
  val global_stage = in port Stage()
  val neighbor_fully_grown = in Bits(neighbor_count bits)
  val neighbor_increase = out Bool()
  val neighbor_is_boundary = in Bits(neighbor_count bits)
  val neighbor_is_error = out Bits(neighbor_count bits)
  val input_data = in port Vec.fill(neighbor_count)(InputData(address_width))
  val output_data = out port Vec.fill(neighbor_count)(OutputData(address_width))
  val root = out port Reg(UInt(address_width bits))
  val odd = out port Reg(Bool())
  val busy = out port Reg(Bool()) init(False)
  /* logic */
  // unpacking `input_data`
  val neighbor_root = Vec(input_data.map(_.neighbor_root))
  val neighbor_parent_vector = Vec(input_data.map(
    _.neighbor_parent_vector)).asBits
  val parent_odd = Vec(input_data.map(_.parent_odd)).asBits
  val child_cluster_parity = Vec(input_data.map(_.child_cluster_parity)).asBits
  val child_touching_boundary = Vec(input_data.map(
    _.child_touching_boundary)).asBits
  val child_peeling_complete = Vec(input_data.map(
    _.child_peeling_complete)).asBits
  val child_peeling_m = Vec(input_data.map(_.child_peeling_m)).asBits
  val parent_peeling_parity_completed = Vec(input_data.map(
    _.parent_peeling_parity_completed)).asBits
  // unpacking `output_data`
  // output looks like a wire, but it's in fact backed by these registers.
  val parent_vector = Reg(Bits(neighbor_count bits))
  val odd_to_children = Reg(Bits(neighbor_count bits))
  val cluster_parity = Reg(Bool())
  val cluster_touching_boundary = Reg(Bool())
  val peeling_complete = Reg(Bool()) init(False)
  val peeling_m = Reg(Bool()) init(False)
  val peeling_parity_completed = Reg(Bool()) init(False)
  for(i <- 0 until neighbor_count) {
    output_data(i).root := root
    output_data(i).parent_vector := parent_vector(i)
    output_data(i).odd_to_children := odd_to_children(i)
    output_data(i).cluster_parity := cluster_parity
    output_data(i).cluster_touching_boundary := cluster_touching_boundary
    output_data(i).peeling_complete := peeling_complete
    output_data(i).peeling_m := peeling_m
    output_data(i).peeling_parity_completed := peeling_parity_completed
  }
  // book-keeping
  val stage = Reg(Stage()) init(Stage.idle)
  val last_stage = Reg(Stage()) init(Stage.idle)
  stage := global_stage
  last_stage := stage
  // `measurement_out` is also backed by register
  val m = Reg(Bool()) init(False)
  when(stage === Stage.measurement_loading) {
    m := measurement
  }
  measurement_out := m
  neighbor_increase := odd && (stage === Stage.grow) && (last_stage =/= Stage.grow)
  // solver stuff
  val solver = new MinValLess8xWithIndex(address_width, neighbor_count)
  for(i <- 0 until neighbor_count) {
    solver.values(i).payload := neighbor_root(i)
    solver.values(i).valid := neighbor_fully_grown(i) &&
      !neighbor_is_boundary(i)
  }
  switch(stage) {
    is(Stage.measurement_loading) {
      root := address
      parent_vector := B(0)
    }
    is(Stage.merge) {
      when(solver.valids.orR && (solver.result < root)) {
        root := solver.result
        parent_vector := solver.valids
      }
    }
  }
  // "Calculate the sub-tree parity and sub_tree touching boundary"
  val v = neighbor_parent_vector & child_cluster_parity
  val next_cluster_parity = v.xorR ^ m
  val next_cluster_touching_boundary = v.orR || neighbor_is_boundary.orR
  switch(stage) {
    is(Stage.measurement_loading) {
      cluster_parity := measurement
      cluster_touching_boundary := False
    }
    is(Stage.merge) {
      cluster_parity := next_cluster_parity
      cluster_touching_boundary := next_cluster_touching_boundary
    }
  }
  // odd stuff
  switch(stage) {
    is(Stage.measurement_loading) {
      odd := measurement
      odd_to_children.setAllTo(measurement)
    }
    is(Stage.merge) {
      when(parent_vector.orR) {
        val o = (parent_vector & parent_odd).orR
        odd := o
        odd_to_children.setAllTo(o)
      } otherwise {
        val o = next_cluster_parity && !next_cluster_touching_boundary
        odd := o
        odd_to_children.setAllTo(o)
      }
    }
    is(Stage.peeling) {
      when(!parent_vector.orR) {
        when(!next_cluster_parity) {
          odd := False
          odd_to_children := B(0)
        } otherwise {
          odd := True
          // TODO double check if `last` is right.
          // Think `Bits` are little endian?
          odd_to_children := neighbor_is_boundary.orR ? B(0) |
            OHMasking.last(neighbor_parent_vector & child_touching_boundary)
        }
      } otherwise {
        odd := False
        when((parent_vector & parent_odd).orR) {
          when(neighbor_is_boundary.orR) {
            odd := True
            odd_to_children := B(0)
          } otherwise {
            odd := False
            // TODO same as above
            odd_to_children := OHMasking.last(
              neighbor_parent_vector & child_touching_boundary)
          }
        } otherwise {
          odd_to_children := B(0)
        }
      }
    }
  }
  // compute `peeling_parity_completed`
  switch(stage) {
    is(Stage.measurement_loading) {
      peeling_parity_completed := False
    }
    is(Stage.peeling) {
      peeling_parity_completed := (!parent_vector.orR) ? True |
        (parent_vector & parent_peeling_parity_completed).orR
    }
  }
  // "Calculate peeling complete"
  val child_incomplete = (neighbor_parent_vector &
    ~child_peeling_complete).orR || (!peeling_parity_completed)
  switch(stage) {
    is(Stage.measurement_loading) {
      peeling_complete := False
    }
    is(Stage.peeling) {
      peeling_complete := !child_incomplete
    }
  }
  // "If peeling complete, absorb children's m while marking the errors"
  when(stage === Stage.peeling && last_stage =/= Stage.peeling) {
    peeling_m := False
  } otherwise {
    when(stage === Stage.peeling && !child_incomplete) {
      peeling_m := m ^ ((neighbor_parent_vector & child_peeling_m).xorR) ^ odd
    }
  }

  // Calculate `neighbor_is_error`
  val neighbor_is_error_internal = Reg(Bits(neighbor_count bits))
  neighbor_is_error_internal := (
    stage === Stage.peeling && !child_incomplete) ?
    (neighbor_parent_vector & child_peeling_m) | B(0)
  val neighbor_is_error_border = Reg(Bits(neighbor_count bits))
  neighbor_is_error_border := (
    stage === Stage.peeling && !child_incomplete && odd) ?
    // TODO double check bit order; `last` or `first`
    OHMasking.last(neighbor_is_boundary) | B(0)
  neighbor_is_error := neighbor_is_error_internal | neighbor_is_error_border

  // Calculate `busy`
  switch(stage) {
    is(Stage.merge) {
      busy := (solver.valids.orR && solver.result < root) ||
        next_cluster_parity =/= cluster_parity ||
        next_cluster_touching_boundary =/= cluster_touching_boundary ||
        (parent_vector.orR && ((parent_vector & parent_odd).orR =/= odd)) ||
        (!(parent_vector.orR) && ((next_cluster_parity & !next_cluster_touching_boundary) =/= odd))
    }
    is(Stage.peeling) {
      busy := child_incomplete
    }
  }
}

object ProcUnitVerilog extends App {
  SpinalVerilog(new ProcessingUnit())
}
