package br.com.fiap.dimdimapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicacao DimDimApp.
 * Ponto de entrada da API RESTful do Projeto DimDim (CP3).
 */
@SpringBootApplication
public class DimDimAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DimDimAppApplication.class, args);
    }
}
