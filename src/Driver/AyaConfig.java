package Driver;

public class AyaConfig {
    public static boolean PRINT_IR_BB_INFO = false;
    public static boolean USE_REG_NUMBER = true;
    public static int STACK_SIZE = 4 * 1024;

    public static double Spill_PARAM = 5.14;

    public static boolean OPT = true; // 优化开关

    public static int MAX_INLINE_LENGTH = 64;

    public static boolean AGGRESSIVE_INLINE = true;

    public static boolean AGGRESSIVE_DIVCNST = true; //激进优化除法，不管溢出行为

    public static boolean CALLER_SAVED = true;

    public static boolean NO_IMM16_CHECK = true;

    public static String builtinFunc = "int __OPT_fib(int i) {\n" +
            "    int a = 1;\n" +
            "    int b = 2;\n" +
            "    int j = 0;\n" +
            "    while (j < (i-1) / 2) {\n" +
            "        a = a + b;\n" +
            "        b = a + b;\n" +
            "        j = j + 1;\n" +
            "    }\n" +
            "    if (i % 2 == 1) {\n" +
            "        return a;\n" +
            "    } else {\n" +
            "        return b;\n" +
            "    }\n" +
            "    return 0;\n" +
            "}";
}
