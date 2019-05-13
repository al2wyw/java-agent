package unsafe;

import java.lang.instrument.Instrumentation;


public class InstrumentationSteal {

    public static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent.premain");
        instrumentation =inst;
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent.agentmain");
        instrumentation =inst;
    }
}
