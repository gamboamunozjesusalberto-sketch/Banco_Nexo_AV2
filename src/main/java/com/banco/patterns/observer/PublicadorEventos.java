package com.banco.patterns.observer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PATRON OBSERVER (Sujeto / Publicador).
 * Los servicios (CuentaService, CreditoService, AuditoriaService) solo publican el evento:
 * no saben quien lo escucha. Para agregar una nueva reaccion (por ejemplo, enviar un correo)
 * se crea otro observador y no se toca ningun servicio (principio Abierto/Cerrado).
 */
@Component
public class PublicadorEventos {

    private final List<ObservadorBancario> observadores = new CopyOnWriteArrayList<>();

    /** Spring entrega todas las clases que implementan ObservadorBancario y se suscriben al iniciar. */
    public PublicadorEventos(List<ObservadorBancario> iniciales) {
        iniciales.forEach(this::suscribir);
    }

    public void suscribir(ObservadorBancario observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void desuscribir(ObservadorBancario observador) {
        observadores.remove(observador);
    }

    /** Avisa a todos los observadores suscritos. */
    public void publicar(EventoBancario evento) {
        for (ObservadorBancario observador : observadores) {
            observador.actualizar(evento);
        }
    }

    public int cantidadObservadores() {
        return observadores.size();
    }
}
