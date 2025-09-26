package com.sistema.iTsystem.repository;

import com.sistema.iTsystem.model.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModeloRepository extends JpaRepository<Modelo, Long> {
    List<Modelo> findByMarca_MarcaIdOrderByModelNomAsc(Long marcaId);
    List<Modelo> findAllByOrderByModelNomAsc();
}