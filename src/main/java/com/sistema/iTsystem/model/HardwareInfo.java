package com.sistema.iTsystem.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hardware_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HardwareInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hw_id")
    private Long hwId;

    @Column(name = "hw_serial_num", length = 100)
    private String hwSerialNum;

    // Relación con Activo
    @OneToOne
    @JoinColumn(name = "activo_activo_id", nullable = false)
    private Activo activo;

    // Relación con Modelo
    @ManyToOne
    @JoinColumn(name = "modelo_model_id", nullable = false)
    private Modelo modelo;

    // Relación con Garantía (uno a uno)
    @OneToOne(mappedBy = "hardwareInfo", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Garantia garantia;
}