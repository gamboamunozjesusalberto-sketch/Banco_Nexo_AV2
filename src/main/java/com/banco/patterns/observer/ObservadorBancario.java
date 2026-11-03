package com.banco.patterns.observer;

/**
 * PATRON OBSERVER (interfaz Observador / Suscriptor).
 * Toda clase que quiera enterarse de lo que pasa en el banco implementa este metodo.
 */
public interface ObservadorBancario {

    void actualizar(EventoBancario evento);
}
