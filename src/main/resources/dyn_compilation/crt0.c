int main();
void _start(void) __attribute__((naked));

void _start(void)
{
    asm("li sp, 0x100");
    main();
    for (;;) ;
}
