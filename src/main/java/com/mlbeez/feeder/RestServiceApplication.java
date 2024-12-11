package com.mlbeez.feeder;

import com.mlbeez.framework.config.MelbeezContextInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class RestServiceApplication {

	public static void main(String[] args) {
			//SpringApplication.run(RestServiceApplication.class, args
		new SpringApplicationBuilder(RestServiceApplication.class)
				.initializers(new MelbeezContextInitializer())  // <---- here
				.run(args);
	}
}
