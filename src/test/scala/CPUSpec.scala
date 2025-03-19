import chisel3._
import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec

class CPUPeekPokeTester(dut: CPU) extends PeekPokeTester(dut) {
  step(10)

  // Dump entire memory as hex
  val mems = dut.mem.mems
  val data: Array[Int] = Array.fill(1024)(0)

  for (i <- 0 until 4) {
    for (j <- 0 until mems(i).length.toInt) {
      val atIdx = peekAt(mems(i), j).toInt
      // set 2 bytes at i position
      data(j) = data(j) | (atIdx << (i * 8))
    }
  }

  println(data.map(_.toHexString).mkString(" "))

  poke(dut.debugMem_rdAddress, 0)
  step(1)
  // expect(dut.debugMem_rdData, 42)
}

class CPUSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Hello"
  it should "do something" in {
    test(new CPU(
      RISCVCompiler.inlineASM(
        """
          |.text
          |addi x1, x0, 42
          |sw x1, 0(x0)
        """.stripMargin)
    )).runPeekPoke(new CPUPeekPokeTester(_))
  }
}
