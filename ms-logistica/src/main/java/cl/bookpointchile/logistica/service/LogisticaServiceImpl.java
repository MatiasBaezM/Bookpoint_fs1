package cl.bookpointchile.logistica.service;

import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;
import cl.bookpointchile.logistica.exception.ResourceNotFoundException;
import cl.bookpointchile.logistica.exception.TransicionEstadoInvalidaException;
import cl.bookpointchile.logistica.model.Envio;
import cl.bookpointchile.logistica.model.EstadoEnvio;
import cl.bookpointchile.logistica.model.RutaDistribucion;
import cl.bookpointchile.logistica.repository.EnvioRepository;
import cl.bookpointchile.logistica.repository.RutaDistribucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticaServiceImpl implements LogisticaService {

    private final EnvioRepository envioRepository;
    private final RutaDistribucionRepository rutaDistribucionRepository;

    @Override
    @Transactional
    public EnvioResponseDTO crearEnvio(CrearEnvioRequestDTO request) {
        log.info("Iniciando creación de envío para Venta ID: {}, Destino: '{}'", request.getVentaId(), request.getDireccionDestino());

        // 1. Validar que no exista un despacho previo para esta venta
        if (envioRepository.findByVentaId(request.getVentaId()).isPresent()) {
            log.warn("Intento de envío duplicado. Venta ID {} ya tiene despacho registrado.", request.getVentaId());
            throw new TransicionEstadoInvalidaException("Ya existe un registro de despacho asociado a la venta ID " + request.getVentaId());
        }

        // 2. Resolver o asignar la ruta óptima de distribución de forma dinámica
        RutaDistribucion rutaSelected = null;
        if (request.getRutaId() != null) {
            rutaSelected = rutaDistribucionRepository.findById(request.getRutaId())
                    .orElseThrow(() -> new ResourceNotFoundException("La ruta especificada con ID " + request.getRutaId() + " no fue encontrada."));
        } else {
            // Logística inteligente: Evalúa la dirección del cliente y asocia transportista
            String direccionUpper = request.getDireccionDestino().toUpperCase();
            if (direccionUpper.contains("HUALPEN") || direccionUpper.contains("HUALPÉN")) {
                List<RutaDistribucion> rutas = rutaDistribucionRepository.findByDestinoIgnoreCase("Hualpén");
                if (!rutas.isEmpty()) rutaSelected = rutas.get(0);
            } else if (direccionUpper.contains("TALCAHUANO")) {
                List<RutaDistribucion> rutas = rutaDistribucionRepository.findByDestinoIgnoreCase("Talcahuano");
                if (!rutas.isEmpty()) rutaSelected = rutas.get(0);
            }

            // Ruta de respaldo (Bío Bío General) si la comuna no coincide con las específicas
            if (rutaSelected == null) {
                log.info("Comuna específica no detectada en la dirección. Asignando ruta general de distribución.");
                List<RutaDistribucion> rutasGenerales = rutaDistribucionRepository.findByDestinoIgnoreCase("Región del Bío Bío");
                if (!rutasGenerales.isEmpty()) {
                    rutaSelected = rutasGenerales.get(0);
                } else {
                    // Fallback en caso de base de datos vacía
                    rutaSelected = rutaDistribucionRepository.findAll().stream().findFirst().orElse(null);
                }
            }
        }

        Envio envio = Envio.builder()
                .ventaId(request.getVentaId())
                .direccionDestino(request.getDireccionDestino())
                .estado(EstadoEnvio.PENDIENTE)
                .fechaEstimada(LocalDate.now().plusDays(3)) // 3 días hábiles estándar
                .rutaDistribucion(rutaSelected)
                .build();

        Envio saved = envioRepository.save(envio);
        log.info("Despacho registrado con éxito. ID Asignado: {}, Estado inicial: {}, Ruta: '{}'", 
                saved.getId(), saved.getEstado(), saved.getRutaDistribucion() != null ? saved.getRutaDistribucion().getDestino() : "Sin ruta");

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public EnvioResponseDTO actualizarEstado(Long id, ActualizarEstadoRequestDTO request) {
        log.info("Solicitud de cambio de estado para Envío ID: {}. Estado objetivo: {}", id, request.getEstado());

        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Actualización fallida: Envío ID {} no existe.", id);
                    return new ResourceNotFoundException("El registro de envío con ID " + id + " no existe.");
                });

        EstadoEnvio actual = envio.getEstado();
        EstadoEnvio nuevo = request.getEstado();

        // Máquina de estados logística estricta (Evita transiciones inválidas)
        if (actual == nuevo) {
            log.info("El envío ID {} ya se encuentra en estado {}. Sin cambios.", id, nuevo);
            return mapToResponse(envio);
        }

        switch (actual) {
            case PENDIENTE:
                if (nuevo != EstadoEnvio.EN_RUTA) {
                    log.warn("Transición denegada para envío ID {}: de PENDIENTE a {}", id, nuevo);
                    throw new TransicionEstadoInvalidaException("Transición inválida. Un envío PENDIENTE solo puede pasar a estado EN_RUTA.");
                }
                break;
            case EN_RUTA:
                if (nuevo != EstadoEnvio.ENTREGADO && nuevo != EstadoEnvio.DEVUELTO) {
                    log.warn("Transición denegada para envío ID {}: de EN_RUTA a {}", id, nuevo);
                    throw new TransicionEstadoInvalidaException("Transición inválida. Un envío EN_RUTA solo puede pasar a ENTREGADO o DEVUELTO.");
                }
                break;
            case ENTREGADO:
                log.warn("Transición denegada: Envío ID {} ya está en estado final ENTREGADO.", id);
                throw new TransicionEstadoInvalidaException("No se puede modificar el estado de un envío que ya fue ENTREGADO.");
            case DEVUELTO:
                log.warn("Transición denegada: Envío ID {} ya está en estado final DEVUELTO.", id);
                throw new TransicionEstadoInvalidaException("No se puede modificar el estado de un envío que ya fue DEVUELTO.");
        }

        envio.setEstado(nuevo);
        Envio saved = envioRepository.save(envio);
        log.info("Estado de despacho ID {} actualizado con éxito: {} -> {}", id, actual, saved.getEstado());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponseDTO obtenerEnvioPorVentaId(Long ventaId) {
        log.info("Buscando despacho asociado a la Venta ID: {}", ventaId);
        Envio envio = envioRepository.findByVentaId(ventaId)
                .orElseThrow(() -> {
                    log.error("No se encontró envío para la Venta ID: {}", ventaId);
                    return new ResourceNotFoundException("No se encontró ningún registro de despacho para la Venta ID " + ventaId);
                });
        return mapToResponse(envio);
    }

    // Helper de mapeo
    private EnvioResponseDTO mapToResponse(Envio e) {
        EnvioResponseDTO.EnvioResponseDTOBuilder builder = EnvioResponseDTO.builder()
                .id(e.getId())
                .ventaId(e.getVentaId())
                .direccionDestino(e.getDireccionDestino())
                .estado(e.getEstado())
                .fechaEstimada(e.getFechaEstimada());

        if (e.getRutaDistribucion() != null) {
            builder.rutaId(e.getRutaDistribucion().getId())
                    .origen(e.getRutaDistribucion().getOrigen())
                    .destino(e.getRutaDistribucion().getDestino())
                    .transportista(e.getRutaDistribucion().getTransportista());
        }

        return builder.build();
    }
}
