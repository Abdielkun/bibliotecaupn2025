package com.upnbiblioteca.controller;

import com.upnbiblioteca.model.*;
import com.upnbiblioteca.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/prestamos")
public class PrestamoController {

    private static final Logger logger = LoggerFactory.getLogger(PrestamoController.class);

    @Autowired private PrestamoService prestamoService;
    @Autowired private LibroService libroService;
    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public String listarPrestamos(Model model) {
        try {
            List<Prestamo> prestamos = prestamoService.obtenerTodosLosPrestamos();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            prestamos.forEach(prestamo -> {
                if (prestamo.getFechaDevolucion() != null) {
                    prestamo.setFechaDevolucionStr(prestamo.getFechaDevolucion().format(formatter));
                }
                if (prestamo.getFechaPrestamo() != null) {
                    prestamo.setFechaPrestamoStr(prestamo.getFechaPrestamo().format(formatter));
                }
            });
            
            model.addAttribute("prestamos", prestamos);
            model.addAttribute("libros", libroService.obtenerTodosLosLibros());
            
            List<Usuario> usuarios = usuarioService.obtenerTodosLosUsuarios().stream()
                    .filter(usuario -> !usuario.getRol().equals("ROLE_ADMIN"))
                    .collect(Collectors.toList());
            model.addAttribute("usuarios", usuarios);
            
            return "misPrestamos";
            
        } catch (Exception e) {
            logger.error("Error al cargar lista de préstamos", e);
            throw e;
        }
    }

    @PostMapping("/guardar")
    public String guardarPrestamo(
            @RequestParam("libroId") Long libroId,
            @RequestParam("usuarioId") Long usuarioId,
            @RequestParam("fechaPrestamo") String fechaPrestamoStr,
            @RequestParam("fechaDevolucion") String fechaDevolucionStr,
            RedirectAttributes redirectAttributes) {
        
        try {
            LocalDate fechaPrestamo = LocalDate.parse(fechaPrestamoStr);
            LocalDate fechaDevolucion = LocalDate.parse(fechaDevolucionStr);

            Libro libro = libroService.obtenerLibroPorId(libroId);
            Usuario usuario = usuarioService.obtenerUsuarioPorId(usuarioId);

            if (libro == null || usuario == null) {
                redirectAttributes.addFlashAttribute("error", "El libro o usuario no existen");
                return "redirect:/prestamos";
            }

            // Validar si el usuario ya tiene préstamos activos
            List<Prestamo> prestamosActivos = prestamoService.obtenerPrestamosActivosPorUsuario(usuarioId);
            if (!prestamosActivos.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Usuario ya tiene préstamos activos. No puede solicitar más.");
                return "redirect:/prestamos";
            }

            if (!libro.isDisponible()) {
                redirectAttributes.addFlashAttribute("error", "El libro no está disponible");
                return "redirect:/prestamos";
            }

            Prestamo prestamo = new Prestamo();
            prestamo.setLibro(libro);
            prestamo.setUsuario(usuario);
            prestamo.setFechaPrestamo(fechaPrestamo);
            prestamo.setFechaDevolucion(fechaDevolucion);
            prestamo.setRenovado(0);
            
            prestamoService.guardarPrestamo(prestamo);
            
            // Marcar libro como no disponible
            libro.setDisponible(false);
            libroService.guardarLibro(libro);

            String mensajeExito = String.format("Préstamo creado: %s para %s (Devolución: %s)", 
                libro.getTitulo(), usuario.getNombre(), fechaDevolucionStr);
                
            redirectAttributes.addFlashAttribute("success", mensajeExito);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad al crear préstamo", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el préstamo");
        } catch (Exception e) {
            logger.error("Error al crear préstamo", e);
            redirectAttributes.addFlashAttribute("error", "Error en formato de fecha o datos inválidos");
        }
        
        return "redirect:/prestamos";
    }

    @GetMapping("/editar/{id}")
    public String editarPrestamo(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.obtenerPrestamoPorId(id);
            if (prestamo == null) {
                redirectAttributes.addFlashAttribute("error", "Préstamo no encontrado");
                return "redirect:/prestamos";
            }
            
            model.addAttribute("prestamo", prestamo);
            model.addAttribute("libros", libroService.obtenerTodosLosLibros());

            List<Usuario> usuarios = usuarioService.obtenerTodosLosUsuarios().stream()
                    .filter(usuario -> !usuario.getRol().equals("ADMIN"))
                    .collect(Collectors.toList());
            model.addAttribute("usuarios", usuarios);

            return "editarPrestamo";
            
        } catch (Exception e) {
            logger.error("Error al cargar formulario de edición", e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar el préstamo");
            return "redirect:/prestamos";
        }
    }

    @PostMapping("/actualizar")
    public String actualizarPrestamo(
            @ModelAttribute Prestamo prestamoForm,
            RedirectAttributes redirectAttributes) {
        
        try {
            Prestamo prestamoExistente = prestamoService.obtenerPrestamoPorId(prestamoForm.getId());
            if (prestamoExistente == null) {
                redirectAttributes.addFlashAttribute("error", "Préstamo no encontrado");
                return "redirect:/prestamos";
            }

            prestamoExistente.setFechaPrestamo(prestamoForm.getFechaPrestamo());
            prestamoExistente.setFechaDevolucion(prestamoForm.getFechaDevolucion());

            prestamoService.guardarPrestamo(prestamoExistente);
            
            String mensajeExito = String.format("Préstamo actualizado: %s (Devolución: %s)", 
                prestamoExistente.getLibro().getTitulo(), 
                prestamoForm.getFechaDevolucion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                
            redirectAttributes.addFlashAttribute("success", mensajeExito);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad al actualizar préstamo ID: " + prestamoForm.getId(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el préstamo");
        } catch (Exception e) {
            logger.error("Error al actualizar préstamo ID: " + prestamoForm.getId(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el préstamo");
        }
        
        return "redirect:/prestamos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarPrestamo(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.obtenerPrestamoPorId(id);
            if (prestamo == null) {
                logger.warn("Intento de eliminar préstamo no existente ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "El préstamo no existe");
                return "redirect:/prestamos";
            }

            String libroTitulo = prestamo.getLibro().getTitulo();
            String usuarioNombre = prestamo.getUsuario().getNombre();
            
            // Marcar libro como disponible
            Libro libro = prestamo.getLibro();
            libro.setDisponible(true);
            libroService.guardarLibro(libro);
            
            prestamoService.eliminarPrestamoPorId(id);
            
            logger.info("Préstamo eliminado ID: {}, Libro: {}, Usuario: {}", id, libroTitulo, usuarioNombre);
            
            String mensajeExito = String.format("Préstamo eliminado: %s para %s", libroTitulo, usuarioNombre);
            redirectAttributes.addFlashAttribute("success", mensajeExito);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad al eliminar préstamo ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el préstamo");
        } catch (Exception e) {
            logger.error("Error al eliminar préstamo ID: " + id, e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el préstamo");
        }
        
        return "redirect:/prestamos";
    }
    
    
}
