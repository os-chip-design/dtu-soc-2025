// This is used to generate waveforrms for the vgacontroller, do not run unless you want to turn your pc into a space heater
//
//
// import chisel3._
// import chiseltest._
// import org.scalatest.flatspec.AnyFlatSpec
//
// // Assuming VgaConfig object is defined and accessible
// import VgaConfig._
//
// // Ensure class name matches the file name if required by testing framework
// class VgaControllerTest extends AnyFlatSpec with ChiselScalatestTester {
//
//   behavior of "Synchronous VgaController VCD Generation"
//
//   it should "write 'A's and generate VCD waveform" in {
//     // Test VgaController directly
//     test(new VgaController).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
//       // --- Setup ---
//       c.clock.setTimeout(0) // Disable timeout
//
//       val charToDisplay = 0xdb // ASCII 65 (0x41)
//       val charToDisplayAlt = 0x41 // ASCII 65 (0x41)
//       val attribute = 0x07 // White on black
//       val writeDataValue = (attribute << 8) | charToDisplay
//       val writeDataAlt = (attribute << 8) | charToDisplayAlt
//
//       // --- Initial State & Reset ---
//       println("Applying Reset...")
//       c.io.wrEnable.poke(false.B)
//       c.io.wrAddr.poke(0.U)
//       c.io.wrData.poke(0.U)
//       // Reset DUT
//       c.reset.poke(true.B)
//       c.clock.step(5) // Hold reset
//       c.reset.poke(false.B)
//       println("Reset Released.")
//       c.clock.step(2) // Step a bit after reset release
//
//       println(
//         s"--- Writing character ${charToDisplay.toChar} (0x${writeDataValue.toHexString}) to first row (0 to ${CHAR_COLS - 1}) ---"
//       )
//
//       for (col <- 0 until CHAR_COLS) {
//         // Poke address and data
//         c.io.wrAddr.poke(col.U)
//         if (col % 10 == 5) {
//           c.io.wrData.poke(writeDataAlt.U)
//         } else {
//           c.io.wrData.poke(writeDataValue.U)
//         }
//         c.io.wrEnable.poke(true.B)
//         c.clock.step(1)
//         c.io.wrEnable.poke(false.B)
//       }
//
//       c.io.wrEnable.poke(false.B)
//       c.io.wrAddr.poke(0.U) // Clear data ports
//       c.io.wrData.poke(0.U)
//
//       println(
//         "--- Finished writing all columns. Running simulation for ~1 frame ---"
//       )
//
//       val simulationCycles =
//         H_TOTAL * 3
//
//       // val simulationCycles = H_TOTAL * V_TOTAL + (H_TOTAL * 3)
//
//       var simulationCyclesAcc = 0
//       // Run the simulation
//       while (simulationCyclesAcc < simulationCycles) {
//         c.clock.step(simulationCycles / 100)
//         simulationCyclesAcc += (simulationCycles / 100)
//         println(
//           s"{$simulationCyclesAcc} / {$simulationCycles} Cycles completed"
//         )
//       }
//     }
//   }
// }
