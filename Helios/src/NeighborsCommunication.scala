import spinal.core._
import spinal.lib._
import HeliosParams._

case class NeighborsCommunication() extends Bundle {
  val root = UInt(address_width bits)
  val parent_vector = Bool()
  val odd = Bool()
  val cluster_parity = Bool()
  val touching_boundary = Bool()
  val peeling_complete = Bool()
  val peeling_m = Bool()
  val peeling_parity_completed = Bool()
}
