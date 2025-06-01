import utest._

object TestX extends TestSuite {
  def tests = Tests {
    test("hello") {
      assert(X.x == 100)
    }
  }
}
