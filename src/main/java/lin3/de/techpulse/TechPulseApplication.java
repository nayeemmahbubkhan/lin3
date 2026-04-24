package lin3.de.techpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechPulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechPulseApplication.class, args);
	}

}
