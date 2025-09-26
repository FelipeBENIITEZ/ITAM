package com.sistema.iTsystem.repository;

import com.sistema.iTsystem.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {
    List<Marca> findAllByOrderByMarcaNomAsc();
    boolean existsByMarcaNom(String marcaNom);
}