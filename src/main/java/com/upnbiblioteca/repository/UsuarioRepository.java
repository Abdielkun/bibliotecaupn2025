package com.upnbiblioteca.repository;

import com.upnbiblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.username) = LOWER(:username)")
    Usuario findByUsernameIgnoreCase(@Param("username") String username);
    boolean existsByUsername(String username);
}
