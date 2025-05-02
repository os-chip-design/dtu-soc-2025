    .global _start

_start:
    li t1, 0x00            # Start address (0)
    li t2, 0x1F            # End address (10)

poll_addresses:
    # Check if the address has something (just an example, assuming a peripheral at that address)
    lw t3, 0(t1)           # Load the value at the current address (t1)
    bnez t3, detected      # If the value is non-zero, stop (something detected)

    # Increment address and check the next one
    addi t1, t1, 4         # Increment the address by 4 (assuming 32-bit access)
    blt t1, t2, poll_addresses  # If t1 < t2, continue polling

    # If no peripherals detected, print 'Hello World'
    li t1, 0x04            # UART base address (assuming UART at 0x04)

print_hello:
    li t0, 'H'
    sw t0, 0(t1)

    li t0, 'e'
    sw t0, 0(t1)

    li t0, 'l'
    sw t0, 0(t1)

    li t0, 'l'
    sw t0, 0(t1)

    li t0, 'o'
    sw t0, 0(t1)

    li t0, 'W'
    sw t0, 0(t1)

    li t0, 'o'
    sw t0, 0(t1)

    li t0, 'r'
    sw t0, 0(t1)

    li t0, 'l'
    sw t0, 0(t1)

    li t0, 'd'
    sw t0, 0(t1)

    j print_hello          # Repeat forever

detected:
    # If a peripheral was detected, halt the program
    li a7, 10              # Syscall for exit (this can vary based on system)
    ecall                  # Make syscall to stop