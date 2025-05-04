// This is used to generate waveforrms for the VgaTop, do not run unless you want to turn your pc into a
// space heater it takes around 10 hours to run a full frame on my quite beefy system
//
//
// import chisel3._
// import chisel3.util._
// import chiseltest._
// import org.scalatest.flatspec.AnyFlatSpec
//
// import VgaConfig._
//
// class VgaTopTest extends AnyFlatSpec with ChiselScalatestTester {
//
//   behavior of "VgaTop"
//
//   // Test parameters
//   val fifoDepth = 8
//   val testFifoDepth = fifoDepth // Use the same depth as the DUT
//
//   it should "initialize, handle writes, and generate VCD" in {
//     test(new VgaTop(fifoDepth = testFifoDepth))
//       .withAnnotations(Seq(WriteVcdAnnotation)) { dut => // Enable VCD dumping
//
//         println("Starting VgaTop Testbench...")
//         dut.clock.setTimeout(
//           0
//         )
//
//         dut.io.write.initSource().setSourceClock(dut.clock)
//         dut.io.write.valid.poke(false.B)
//
//         println("Applying Reset...")
//         dut.reset.poke(true.B)
//         dut.clock.step(5)
//         dut.reset.poke(false.B)
//         println("Reset Released.")
//         dut.clock.step(2)
//
//         val charsToWrite = Seq(
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07),
//           (0xdb, 0x07)
//         )
//         var writeAddress = 0
//
//         for ((charInt, attr) <- charsToWrite) {
//           val writeDataValue = (attr << 8) | charInt
//
//           dut.io.write.bits.addr.poke(writeAddress.U)
//           dut.io.write.bits.data.poke(writeDataValue.U)
//           // Assert valid
//           dut.io.write.valid.poke(true.B)
//
//           while (!dut.io.write.ready.peekBoolean()) {
//             println(
//               s"Waiting for ready... (Addr=$writeAddress, Data=0x${writeDataValue}%04X)"
//             )
//             dut.clock.step(1)
//           }
//           // DUT is ready, transaction happens on this clock edge
//           println(
//             f"DUT ready, sending: Addr=$writeAddress%d, Data=0x${writeDataValue}%04X (Char='${charInt.toChar}', Attr=0x${attr}%X)"
//           )
//           dut.clock.step(1) // Step clock for the write to be consumed
//
//           // Deassert valid for next cycle (unless sending immediately)
//           dut.io.write.valid.poke(false.B)
//           writeAddress += 1
//         }
//         println("Finished writing initial characters.")
//
//         // Ensure valid is low after the loop
//         dut.io.write.valid.poke(false.B)
//
//         // --- Simulation Run ---
//         val clockDivRatio = 4
//         val numLinesToSimulate = 3
//         val simulationCycles =
//           VgaConfig.H_TOTAL * clockDivRatio * numLinesToSimulate
//
//         println(
//           s"Running simulation for $simulationCycles system clock cycles (~$numLinesToSimulate VGA lines)..."
//         )
//         dut.clock.step(simulationCycles)
//
//         println(
//           s"--- Simulation complete. Check VCD file (test_run_dir/VgaTopTest/VgaTop.vcd) ---"
//         )
//       }
//   }
// }
