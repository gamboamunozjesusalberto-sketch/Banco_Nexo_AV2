package com.banco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada. Spring Boot solo levanta el servidor web (Tomcat) y las vistas. */
@SpringBootApplication
public class BancaDigitalApplication {
    public static void main(String[] args) {
        SpringApplication.run(BancaDigitalApplication.class, args);
    }
}
