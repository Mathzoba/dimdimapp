package br.com.fiap.dimdimapp.repository;

import br.com.fiap.dimdimapp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio do Cliente.
 * O Spring Data JPA fornece automaticamente os metodos de CRUD
 * (save, findAll, findById, deleteById, etc).
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
