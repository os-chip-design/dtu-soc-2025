void _start() {
  // Load UART base address (0x08) into t1 register
  asm volatile (
      "li t1, 0x08\n"   // Load UART base address into t1
  );

  while (1) {
      asm volatile (
          "li t0, 'H'\n"   // Load 'H' into t0
          "sw t0, 0(t1)\n"  // Write 'H' to UART address
      );

      asm volatile (
          "li t0, 'e'\n"    // Load 'e' into t0
          "sw t0, 0(t1)\n"  // Write 'e' to UART address
      );

      asm volatile (
          "li t0, 'l'\n"    // Load 'l' into t0
          "sw t0, 0(t1)\n"  // Write 'l' to UART address
      );

      asm volatile (
          "li t0, 'l'\n"    // Load second 'l' into t0
          "sw t0, 0(t1)\n"  // Write 'l' to UART address
      );

      asm volatile (
          "li t0, 'o'\n"    // Load 'o' into t0
          "sw t0, 0(t1)\n"  // Write 'o' to UART address
      );

      asm volatile (
          "li t0, 'W'\n"    // Load 'W' into t0
          "sw t0, 0(t1)\n"  // Write 'W' to UART address
      );

      asm volatile (
          "li t0, 'o'\n"    // Load 'o' into t0
          "sw t0, 0(t1)\n"  // Write 'o' to UART address
      );

      asm volatile (
          "li t0, 'r'\n"    // Load 'r' into t0
          "sw t0, 0(t1)\n"  // Write 'r' to UART address
      );

      asm volatile (
          "li t0, 'l'\n"    // Load 'l' into t0
          "sw t0, 0(t1)\n"  // Write 'l' to UART address
      );

      asm volatile (
          "li t0, 'd'\n"    // Load 'd' into t0
          "sw t0, 0(t1)\n"  // Write 'd' to UART address
      );
  }
}