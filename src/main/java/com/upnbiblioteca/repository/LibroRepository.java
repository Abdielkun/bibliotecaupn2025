package com.upnbiblioteca.repository;

import com.upnbiblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {
    List<Libro> findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCase(String titulo, String autor);
    
    Libro findByIsbn(String isbn);
    
    // Nuevo método para verificar si un libro con el mismo título ya existe
    Libro findByTitulo(String titulo);
}
