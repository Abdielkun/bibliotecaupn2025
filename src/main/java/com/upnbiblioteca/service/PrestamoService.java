package com.upnbiblioteca.service;

import com.upnbiblioteca.model.Prestamo;
import com.upnbiblioteca.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    // Obtener todos los préstamos
    public List<Prestamo> obtenerTodosLosPrestamos() {
        return prestamoRepository.findAll();
    }

    // Obtener préstamos por nombre de usuario
    public List<Prestamo> obtenerPrestamosPorUsuario(String username) {
        return prestamoRepository.findByUsuarioUsername(username);
    }

    // Obtener préstamo por ID
    public Prestamo obtenerPrestamoPorId(Long id) {
        return prestamoRepository.findById(id).orElse(null);
    }

    // Obtener préstamos activos por usuario
    public List<Prestamo> obtenerPrestamosActivosPorUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioIdAndFechaDevolucionAfter(usuarioId, LocalDate.now());
    }

    // Verificar si el usuario puede solicitar un nuevo préstamo
    public boolean puedeSolicitarPrestamo(Long usuarioId) {
        return prestamoRepository.countByUsuarioIdAndFechaDevolucionAfter(usuarioId, LocalDate.now()) < 3;
    }

    // Guardar un nuevo préstamo
    public void guardarPrestamo(Prestamo prestamo) {
        prestamoRepository.save(prestamo);
    }

    // Eliminar un préstamo por ID
    public void eliminarPrestamoPorId(Long id) {
        prestamoRepository.deleteById(id);
    }

    // Solicitar renovación de un préstamo
    public void solicitarRenovacion(Long id) {
        Prestamo prestamo = obtenerPrestamoPorId(id);
        if (prestamo != null && prestamo.getRenovado() < 1) { // Permitir solo una renovación
            prestamo.setFechaDevolucion(prestamo.getFechaDevolucion().plusDays(15)); // Añadir 15 días
            prestamo.setRenovado(prestamo.getRenovado() + 1); // Incrementar contador de renovaciones
            guardarPrestamo(prestamo);
        }
    }

    // Método para verificar si un préstamo puede ser renovado
    public boolean puedeRenovarPrestamo(Long id) {
        Prestamo prestamo = obtenerPrestamoPorId(id);
        return prestamo != null && prestamo.getRenovado() < 1; // Solo permitir una renovación
    }
}
