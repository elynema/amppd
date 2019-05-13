package edu.indiana.dlib.amppd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("edu.indiana.dlib.amppd.repository")
public class AmppdApplication {
	
    private static ApplicationContext applicationContext;

	public static void main(String[] args) {
		SpringApplication.run(AmppdApplication.class, args);
	}
	
    public static void displayAllBeans() {
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        for(String beanName : allBeanNames) {
            System.out.println(beanName);
        }
    }	

}