package com.upnbiblioteca.controller;

import com.upnbiblioteca.model.*;
import com.upnbiblioteca.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/usuario")
public class UsuarioInterfaceController {

    @Autowired private LibroService libroService;
    @Autowired private PrestamoService prestamoService;
    @Autowired private UsuarioService usuarioService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) return "redirect:/login";

        List<Prestamo> misPrestamos = prestamoService.obtenerTodosLosPrestamos()
                .stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("misPrestamos", misPrestamos);
        model.addAttribute("prestamosActivos", misPrestamos.size());
        model.addAttribute("librosDisponibles", 
            libroService.obtenerTodosLosLibros().stream().filter(Libro::isDisponible).count());
        
        return "usuario/dashboard";
    }

    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(value = "buscar", required = false) String buscar, Model model) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) return "redirect:/login";

        List<Libro> libros = libroService.obtenerTodosLosLibros();

        if (buscar != null && !buscar.trim().isEmpty()) {
            String buscarLower = buscar.toLowerCase().trim();
            libros = libros.stream()
                    .filter(libro -> libro.getTitulo().toLowerCase().contains(buscarLower) ||
                                   libro.getAutor().toLowerCase().contains(buscarLower))
                    .collect(Collectors.toList());
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("libros", libros);
        model.addAttribute("buscar", buscar);
        model.addAttribute("totalLibros", libros.size());
        model.addAttribute("librosDisponibles", libros.stream().filter(Libro::isDisponible).count());

        return "usuario/catalogo";
    }

    @GetMapping("/mis-prestamos")
    public String misPrestamos(Model model) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) return "redirect:/login";

        List<Prestamo> misPrestamos = prestamoService.obtenerTodosLosPrestamos()
                .stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("prestamos", misPrestamos);
        model.addAttribute("prestamosActivos", misPrestamos.size());

        return "usuario/mis-prestamos";
    }

    @PostMapping("/renovar/{id}")
    public String renovarPrestamo(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) return "redirect:/login";

        try {
            Prestamo prestamo = prestamoService.obtenerPrestamoPorId(id);
            if (prestamo == null || !prestamo.getUsuario().getId().equals(usuario.getId())) {
                redirectAttributes.addFlashAttribute("error", "Préstamo no encontrado");
                return "redirect:/usuario/mis-prestamos";
            }
            
            // Verificar si ya fue renovado antes
            if (prestamo.getRenovado() >= 1) {
                redirectAttributes.addFlashAttribute("error", "Solo puedes renovar un préstamo una vez");
                return "redirect:/usuario/mis-prestamos";
            }

            prestamo.setFechaDevolucion(prestamo.getFechaDevolucion().plusDays(15));
            prestamo.setRenovado(prestamo.getRenovado() + 1);
            prestamoService.guardarPrestamo(prestamo);
            redirectAttributes.addFlashAttribute("success", "Préstamo renovado por 15 días más");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al renovar el préstamo");
        }
        return "redirect:/usuario/mis-prestamos";
    }

    @PostMapping("/solicitar-prestamo/{libroId}")
    public String solicitarPrestamo(@PathVariable Long libroId, RedirectAttributes redirectAttributes) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) return "redirect:/login";

        try {
            // Validar libro disponible
            Libro libro = libroService.obtenerLibroPorId(libroId);
            if (libro == null || !libro.isDisponible()) {
                redirectAttributes.addFlashAttribute("error", "El libro no está disponible");
                return "redirect:/usuario/catalogo";
            }

            // Validar límite de préstamos (máximo 3)
            long prestamosActivos = prestamoService.obtenerTodosLosPrestamos()
                    .stream()
                    .filter(p -> p.getUsuario().getId().equals(usuario.getId()) && 
                               p.getFechaDevolucion().isAfter(LocalDate.now()))
                    .count();

            if (prestamosActivos >= 3) {
                redirectAttributes.addFlashAttribute("error", 
                    "Límite alcanzado: Solo puedes tener 3 préstamos activos");
                return "redirect:/usuario/catalogo";
            }

            // Verificar si el usuario tiene préstamos pendientes
            boolean tienePrestamosPendientes = prestamoService.obtenerTodosLosPrestamos()
                    .stream()
                    .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()) && 
                                   p.getFechaDevolucion().isAfter(LocalDate.now()));

            if (tienePrestamosPendientes) {
                redirectAttributes.addFlashAttribute("error", 
                    "No puedes solicitar un nuevo préstamo mientras tengas libros pendientes de devolución.");
                return "redirect:/usuario/catalogo";
            }

            // Crear préstamo
            Prestamo prestamo = new Prestamo();
            prestamo.setLibro(libro);
            prestamo.setUsuario(usuario);
            prestamo.setFechaPrestamo(LocalDate.now());
            prestamo.setFechaDevolucion(LocalDate.now().plusDays(15)); // 15 días para devolver
            prestamo.setRenovado(0);

            prestamoService.guardarPrestamo(prestamo);
            
            // Actualizar estado del libro
            libro.setDisponible(false);
            libroService.guardarLibro(libro);

            redirectAttributes.addFlashAttribute("success", 
                "Préstamo registrado. Fecha de devolución: " + prestamo.getFechaDevolucion());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la solicitud");
        }

        return "redirect:/usuario/catalogo";
    }

    @PostMapping("/devolver-libro/{id}")
    public String devolverLibro(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para devolver un libro.");
            return "redirect:/login"; // Redirigir si no está autenticado
        }

        try {
            Prestamo prestamo = prestamoService.obtenerPrestamoPorId(id);
            if (prestamo == null || !prestamo.getUsuario().getId().equals(usuario.getId())) {
                redirectAttributes.addFlashAttribute("error", "Préstamo no encontrado o no pertenece a este usuario.");
                return "redirect:/usuario/mis-prestamos";
            }

            // Marcar libro como disponible
            Libro libro = prestamo.getLibro();
            libro.setDisponible(true);
            libroService.guardarLibro(libro);

            // Eliminar el préstamo
            prestamoService.eliminarPrestamoPorId(id);
            redirectAttributes.addFlashAttribute("success", "Libro devuelto exitosamente.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al devolver el libro.");
        }

        return "redirect:/usuario/mis-prestamos";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) {
            return "redirect:/login"; // Redirigir si no está autenticado
        }

        model.addAttribute("usuario", usuario);
        return "usuario/perfil"; // Asegúrate de que esta vista esté configurada correctamente
    }

    @PostMapping("/actualizar-perfil")
    public String actualizarPerfil(@ModelAttribute Usuario usuarioForm, RedirectAttributes redirectAttributes) {
        Usuario usuario = getCurrentUser ();
        if (usuario == null) {
            return "redirect:/login"; // Redirigir si no está autenticado
        }

        try {
            usuario.setNombre(usuarioForm.getNombre());
            usuario.setEmail(usuarioForm.getEmail());
            usuarioService.save(usuario);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el perfil.");
        }

        return "redirect:/usuario/perfil";
    }

    private Usuario getCurrentUser () {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return usuarioService.findByUsername(auth.getName());
        }
        return null;
    }
}
