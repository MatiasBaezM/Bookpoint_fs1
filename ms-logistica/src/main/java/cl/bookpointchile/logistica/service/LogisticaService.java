package cl.bookpointchile.logistica.service;

import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;

public interface LogisticaService {
    EnvioResponseDTO crearEnvio(CrearEnvioRequestDTO request);
    EnvioResponseDTO actualizarEstado(Long id, ActualizarEstadoRequestDTO request);
    EnvioResponseDTO obtenerEnvioPorVentaId(Long ventaId);
}
