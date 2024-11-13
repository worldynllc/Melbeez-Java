package com.mlbeez.feeder;

import com.mlbeez.framework.config.MelbeezContextInitializer;
import com.stripe.Stripe;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RestServiceApplication {

	public static void main(String[] args) {
			//SpringApplication.run(RestServiceApplication.class, args
		new SpringApplicationBuilder(RestServiceApplication.class)
				.initializers(new MelbeezContextInitializer())  // <---- here
				.run(args);
	}

}
