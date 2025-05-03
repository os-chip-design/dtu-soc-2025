    .global _start

_start:
    li t1, 0x00            # Start address
    li t2, 0x1F            # End address (exclusive)

poll_addresses:
    lw t3, 0(t1)           # Load word from current address
    bnez t3, handle_found  # If found something, go handle it

next_address:
    addi t1, t1, 4         # Increment address by 4 bytes
    blt t1, t2, poll_addresses  # Loop if not at end
    j print_hello               # Otherwise, done polling — print message

handle_found:
    # Example of handling found data (here we just nop)
    nop                    # Placeholder for processing the detected data
    j next_address         # Continue with the next address

print_hello:
    li t1, 0x04            # UART base address (assuming 0x04)

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

    j print_hello          # Loop forever
