package com.upnbiblioteca.repository;

import com.upnbiblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    // Método para encontrar préstamos por el nombre de usuario
    List<Prestamo> findByUsuarioUsername(String username);

    // Método para encontrar préstamos activos por usuario
    List<Prestamo> findByUsuarioIdAndFechaDevolucionAfter(Long usuarioId, LocalDate now);

    // Método para contar préstamos activos por usuario
    int countByUsuarioIdAndFechaDevolucionAfter(Long usuarioId, LocalDate now);
}
