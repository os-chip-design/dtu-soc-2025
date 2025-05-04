#define UART_ADDR   ((volatile char*)0x08)

void _start() {
    volatile int* addr;
    int found = 0;

    // Poll memory addresses 0x00 to 0x0A (inclusive), word-aligned
    for (int i = 0x00; i <= 0x0A; i += 4) {
        addr = (volatile int*)i;
        if (*addr != 0) {
            found = 1;
            break;
        }
    }

    // Exit if nothing found (simulate with infinite loop)
    if (!found) {
        while (1) {}
    }

    // Send "HelloWorld" repeatedly
    while (1) {
        *UART_ADDR = 'H';
        *UART_ADDR = 'e';
        *UART_ADDR = 'l';
        *UART_ADDR = 'l';
        *UART_ADDR = 'o';
        *UART_ADDR = 'W';
        *UART_ADDR = 'o';
        *UART_ADDR = 'r';
        *UART_ADDR = 'l';
        *UART_ADDR = 'd';
    }
}