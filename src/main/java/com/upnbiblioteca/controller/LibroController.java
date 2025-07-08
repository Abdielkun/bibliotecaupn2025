package com.upnbiblioteca.controller;

import com.upnbiblioteca.model.Libro;
import com.upnbiblioteca.service.LibroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/libros")
public class LibroController {

    @Autowired
    private LibroService libroService;

    @GetMapping("/buscar")
    public String buscarLibros(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Libro> libros;
        if (keyword != null && !keyword.isBlank()) {
            libros = libroService.buscarPorTituloOAutor(keyword);
        } else {
            libros = libroService.obtenerTodosLosLibros();
        }
        model.addAttribute("libros", libros);
        model.addAttribute("keyword", keyword);
        model.addAttribute("libroForm", new Libro());
        return "buscar-libros";
    }

    @PostMapping("/guardar")
    public String guardarLibro(@ModelAttribute("libroForm") Libro libro, RedirectAttributes redirectAttributes) {
        try {
            // Validar que todos los campos requeridos estén completos
            if (libro.getTitulo() == null || libro.getAutor() == null || libro.getIsbn() == null) {
                redirectAttributes.addFlashAttribute("error", "Todos los campos son obligatorios.");
                return "redirect:/libros/buscar";
            }

            // Verificar si el libro es nuevo o existente
            if (libro.getId() == null) {
                // Nuevo libro
                if (libroService.existePorIsbn(libro.getIsbn())) {
                    redirectAttributes.addFlashAttribute("error", "El ISBN '" + libro.getIsbn() + "' ya está registrado.");
                    return "redirect:/libros/buscar";
                }
                if (libroService.existePorTitulo(libro.getTitulo())) {
                    redirectAttributes.addFlashAttribute("error", "El título '" + libro.getTitulo() + "' ya está registrado.");
                    return "redirect:/libros/buscar";
                }
                libroService.guardarLibro(libro);
                redirectAttributes.addFlashAttribute("success", "Libro creado correctamente.");
            } else {
                // Actualizar libro existente
                Libro existente = libroService.obtenerLibroPorId(libro.getId());
                if (existente == null) {
                    redirectAttributes.addFlashAttribute("error", "El libro que intentas editar no existe.");
                    return "redirect:/libros/buscar";
                }
                // Verificar duplicados solo si se está cambiando el título o ISBN
                if (!existente.getIsbn().equals(libro.getIsbn()) && libroService.existePorIsbn(libro.getIsbn())) {
                    redirectAttributes.addFlashAttribute("error", "El ISBN '" + libro.getIsbn() + "' ya está registrado.");
                    return "redirect:/libros/buscar";
                }
                if (!existente.getTitulo().equals(libro.getTitulo()) && libroService.existePorTitulo(libro.getTitulo())) {
                    redirectAttributes.addFlashAttribute("error", "El título '" + libro.getTitulo() + "' ya está registrado.");
                    return "redirect:/libros/buscar";
                }
                // Actualizar los campos del libro existente
                existente.setTitulo(libro.getTitulo());
                existente.setAutor(libro.getAutor());
                existente.setIsbn(libro.getIsbn());
                existente.setDisponible(libro.isDisponible());
                libroService.guardarLibro(existente);
                redirectAttributes.addFlashAttribute("success", "Libro actualizado correctamente.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el libro.");
        }
        return "redirect:/libros/buscar";
    }

    @GetMapping("/editar/{id}")
    public String editarLibro(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Libro libro = libroService.obtenerLibroPorId(id);
        if (libro == null) {
            redirectAttributes.addFlashAttribute("error", "Libro no encontrado");
            return "redirect:/libros/buscar";
        }
        model.addAttribute("libroForm", libro);
        return "buscar-libros"; // Asegúrate de que esta vista esté configurada correctamente
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarLibro(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        libroService.eliminarLibroPorId(id);
        redirectAttributes.addFlashAttribute("success", "Libro eliminado correctamente");
        return "redirect:/libros/buscar";
    }
}
