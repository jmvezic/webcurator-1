package org.webcurator.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@SpringBootApplication
@ComponentScan(basePackages = {
        "org.webcurator.app",
        "org.webcurator.core.coordinator",
//        "org.webcurator.core.harvester.coordinator",
//        "org.webcurator.core.store.coordinator",
//        "org.webcurator.core.rest",
//        "org.webcurator.core.reader",
//        "org.webcurator.core.harvester.agent.HarvestAgentClient",
//        "org.webcurator.core.visualization.networkmap.service",
//        "org.webcurator.domain.VisualizationImportedFileDAOImpl"
})
//        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Controller.class)
//@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class TestApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(TestApplication.class, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // Note that this is just here for debugging purposes. It can be deleted at any time.
            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }
        };
    }
}
