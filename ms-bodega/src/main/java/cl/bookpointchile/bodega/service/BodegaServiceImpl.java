package cl.bookpointchile.bodega.service;

import cl.bookpointchile.bodega.dto.CrearOrdenPickingRequestDTO;
import cl.bookpointchile.bodega.dto.OrdenPickingResponseDTO;
import cl.bookpointchile.bodega.dto.UbicacionRequestDTO;
import cl.bookpointchile.bodega.dto.UbicacionResponseDTO;
import cl.bookpointchile.bodega.exception.EstadoPickingInvalidoException;
import cl.bookpointchile.bodega.exception.EstadoPickingInvalidoException;
import cl.bookpointchile.bodega.exception.OrdenPickingNoEncontradaException;
import cl.bookpointchile.bodega.exception.UbicacionNoEncontradaException;
import cl.bookpointchile.bodega.model.OrdenPicking;
import cl.bookpointchile.bodega.model.UbicacionFisica;
import cl.bookpointchile.bodega.repository.OrdenPickingRepository;
import cl.bookpointchile.bodega.repository.UbicacionFisicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BodegaServiceImpl implements BodegaService {

    private final UbicacionFisicaRepository ubicacionRepository;
    private final OrdenPickingRepository pickingRepository;

    @Override
    @Transactional
    public UbicacionResponseDTO registrarUbicacion(UbicacionRequestDTO request) {
        String barCodeUpper = request.getCodigoBarras().trim().toUpperCase();
        log.info("Registrando nueva ubicación física: '{}' en Pasillo: {}, Estante: {}, Nivel: {}", 
                barCodeUpper, request.getPasillo(), request.getEstante(), request.getNivel());

        if (ubicacionRepository.existsByCodigoBarras(barCodeUpper)) {
            log.warn("Registro de ubicación fallido: El código de barras '{}' ya existe.", barCodeUpper);
            throw new EstadoPickingInvalidoException("La ubicación con código de barras '" + request.getCodigoBarras() + "' ya existe.");
        }

        UbicacionFisica ubicacion = UbicacionFisica.builder()
                .pasillo(request.getPasillo().trim())
                .estante(request.getEstante().trim())
                .nivel(request.getNivel().trim())
                .codigoBarras(barCodeUpper)
                .build();

        UbicacionFisica saved = ubicacionRepository.save(ubicacion);
        log.info("Ubicación física registrada con éxito. ID: {}, Código: '{}'", saved.getId(), saved.getCodigoBarras());
        return mapToUbicacionResponse(saved);
    }

    @Override
    @Transactional
    public OrdenPickingResponseDTO crearOrdenPicking(CrearOrdenPickingRequestDTO request) {
        log.info("Creando orden de picking para la venta ID: {}, Asignada al operario: '{}'", 
                request.getVentaId(), request.getOperarioAsignado());

        if (pickingRepository.existsByVentaId(request.getVentaId())) {
            log.warn("Creación de Picking fallida: La venta ID: {} ya posee una orden de picking.", request.getVentaId());
            throw new EstadoPickingInvalidoException("La orden de picking para la venta con ID '" + request.getVentaId() + "' ya se encuentra registrada.");
        }

        OrdenPicking orden = OrdenPicking.builder()
                .ventaId(request.getVentaId())
                .operarioAsignado(request.getOperarioAsignado().trim())
                .estado("PENDIENTE")
                .fechaAsignacion(LocalDateTime.now())
                .build();

        OrdenPicking saved = pickingRepository.save(orden);
        log.info("Orden de picking creada con éxito. ID: {}, Venta: {}, Estado: {}", 
                saved.getId(), saved.getVentaId(), saved.getEstado());
        return mapToPickingResponse(saved);
    }

    @Override
    @Transactional
    public OrdenPickingResponseDTO actualizarEstadoPicking(Long id, String nuevoEstado) {
        String estadoUpper = nuevoEstado.trim().toUpperCase();
        log.info("Solicitud de cambio de estado para la Orden ID: {} a '{}'", id, estadoUpper);

        OrdenPicking ord = pickingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización de estado fallida: Orden ID {} no encontrada.", id);
                    return new OrdenPickingNoEncontradaException("La orden de picking con ID " + id + " no existe.");
                });

        String estadoActual = ord.getEstado();

        // 1. Validar si el estado de destino es válido en el sistema
        if (!"PENDIENTE".equals(estadoUpper) && !"EN_PROCESO".equals(estadoUpper) && !"COMPLETADA".equals(estadoUpper)) {
            log.warn("Máquina de estados: Intento de asignar un estado inexistente: '{}' para la Orden ID: {}", estadoUpper, id);
            throw new EstadoPickingInvalidoException("El estado '" + nuevoEstado + "' no es un estado válido para una orden de picking.");
        }

        // 2. Si no hay cambios reales en el estado, retornar inmediatamente sin cambios
        if (estadoActual.equals(estadoUpper)) {
            return mapToPickingResponse(ord);
        }

        // 3. Reglas de Transición de la Máquina de Estados
        if ("PENDIENTE".equals(estadoActual)) {
            if (!"EN_PROCESO".equals(estadoUpper)) {
                log.warn("Máquina de estados: Transición inválida '{}' -> '{}' para la Orden ID: {}.", estadoActual, estadoUpper, id);
                throw new EstadoPickingInvalidoException("Transición de estado inválida. Una orden en estado PENDIENTE sólo puede pasar a EN_PROCESO.");
            }
            log.info("Operario '{}' comenzó a armar el pedido para la venta ID: {}", ord.getOperarioAsignado(), ord.getVentaId());
        } 
        else if ("EN_PROCESO".equals(estadoActual)) {
            if (!"COMPLETADA".equals(estadoUpper)) {
                log.warn("Máquina de estados: Transición inválida '{}' -> '{}' para la Orden ID: {}.", estadoActual, estadoUpper, id);
                throw new EstadoPickingInvalidoException("Transición de estado inválida. Una orden en estado EN_PROCESO sólo puede pasar a COMPLETADA.");
            }
            log.info("Orden de picking ID: {} completada con éxito por el operario '{}'", id, ord.getOperarioAsignado());
        } 
        else if ("COMPLETADA".equals(estadoActual)) {
            log.warn("Máquina de estados: Intento de violar estados. Orden ID: {} ya se encuentra COMPLETADA y se intentó pasar a '{}'", id, estadoUpper);
            throw new EstadoPickingInvalidoException("La orden de picking ya se encuentra en estado COMPLETADA y no admite más cambios de estado.");
        }

        ord.setEstado(estadoUpper);
        OrdenPicking guardada = pickingRepository.save(ord);
        return mapToPickingResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UbicacionResponseDTO> obtenerUbicaciones() {
        log.info("Listando todas las ubicaciones físicas registradas.");
        return ubicacionRepository.findAll().stream()
                .map(this::mapToUbicacionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenPickingResponseDTO> obtenerOrdenesPicking() {
        log.info("Listando todas las órdenes de picking.");
        return pickingRepository.findAll().stream()
                .map(this::mapToPickingResponse)
                .collect(Collectors.toList());
    }

    // Mapeadores manuales
    private UbicacionResponseDTO mapToUbicacionResponse(UbicacionFisica u) {
        return UbicacionResponseDTO.builder()
                .id(u.getId())
                .pasillo(u.getPasillo())
                .estante(u.getEstante())
                .nivel(u.getNivel())
                .codigoBarras(u.getCodigoBarras())
                .build();
    }

    private OrdenPickingResponseDTO mapToPickingResponse(OrdenPicking o) {
        return OrdenPickingResponseDTO.builder()
                .id(o.getId())
                .ventaId(o.getVentaId())
                .operarioAsignado(o.getOperarioAsignado())
                .estado(o.getEstado())
                .fechaAsignacion(o.getFechaAsignacion())
                .build();
    }
}
