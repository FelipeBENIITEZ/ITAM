package com.sistema.iTsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "garantias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Garantia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garant_id")
    private Long garantId;

    @Column(name = "garant_fecha_inicio", nullable = false)
    private LocalDate garantInicioFec;

    @Column(name = "garant_fecha_fin", nullable = false)
    private LocalDate garantFechaFin;

    @Column(name = "garant_vigencia", length = 50)
    private String garantVigencia;

    // Relacion con HardwareInfo (agregare despues)
    @OneToOne
    @JoinColumn(name = "hardware_info_hw_id")
    private HardwareInfo hardwareInfo;
}