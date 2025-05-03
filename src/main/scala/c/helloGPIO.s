    .global _start

_start:
    li t1, 0x28            # UART base address

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