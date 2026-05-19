package br.com.fiap.dimdimapp.controller;

import br.com.fiap.dimdimapp.model.Cliente;
import br.com.fiap.dimdimapp.repository.ClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST do recurso Cliente.
 * Expoe o CRUD completo (Create, Read, Update, Delete)
 * sobre a tabela "clientes" - requisito obrigatorio do CP3.
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository repository;

    public ClienteController(ClienteRepository repository) {
        this.repository = repository;
    }

    // CREATE - cadastra um novo cliente
    @PostMapping
    public ResponseEntity<Cliente> criar(@RequestBody Cliente cliente) {
        Cliente salvo = repository.save(cliente);
        return ResponseEntity.status(201).body(salvo);
    }

    // READ - lista todos os clientes
    @GetMapping
    public List<Cliente> listar() {
        return repository.findAll();
    }

    // READ - busca um cliente pelo id
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE - atualiza um cliente existente
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(@PathVariable Long id,
                                             @RequestBody Cliente dados) {
        return repository.findById(id).map(cliente -> {
            if (dados.getNome() != null) {
                cliente.setNome(dados.getNome());
            }
            if (dados.getEmail() != null) {
                cliente.setEmail(dados.getEmail());
            }
            if (dados.getSaldo() != null) {
                cliente.setSaldo(dados.getSaldo());
            }
            return ResponseEntity.ok(repository.save(cliente));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE - remove um cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
