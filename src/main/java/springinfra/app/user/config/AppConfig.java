package springinfra.app.user.config;

import springinfra.app.user.DefaultBean;
import springinfra.infra.annotation.bean.Bean;
import springinfra.infra.annotation.bean.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public DefaultBean defaultBean() {
        return new DefaultBean();
    }
}
