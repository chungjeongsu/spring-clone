package springinfra;

import springinfra.context.di.DIContext;

public class SpringApplication {

    public static void run(Class<?> appClass) {
        DIContext diContext = new DIContext(appClass);
    }
}
