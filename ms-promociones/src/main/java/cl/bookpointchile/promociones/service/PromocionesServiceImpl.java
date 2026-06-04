package cl.bookpointchile.promociones.service;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;
import cl.bookpointchile.promociones.exception.PromocionCaducadaException;
import cl.bookpointchile.promociones.exception.PromocionNoEncontradaException;
import cl.bookpointchile.promociones.model.Promocion;
import cl.bookpointchile.promociones.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromocionesServiceImpl implements PromocionesService {

    private final PromocionRepository promocionRepository;

    @Override
    @Transactional
    public PromocionResponseDTO registrarPromocion(CrearPromocionRequestDTO request) {
        String codigoUpper = request.getCodigo().trim().toUpperCase();
        log.info("Registrando nueva promoción: '{}' con descuento del {}%", codigoUpper, request.getPorcentajeDescuento());

        if (promocionRepository.existsByCodigoIgnoreCase(codigoUpper)) {
            log.warn("Registro rechazado: Promoción con código '{}' ya existe.", codigoUpper);
            throw new PromocionCaducadaException("La promoción con el código '" + request.getCodigo() + "' ya se encuentra registrada.");
        }

        Promocion promocion = Promocion.builder()
                .codigo(codigoUpper)
                .porcentajeDescuento(request.getPorcentajeDescuento())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .estado(request.getEstado() != null ? request.getEstado().trim().toUpperCase() : "ACTIVO")
                .build();

        Promocion saved = promocionRepository.save(promocion);
        log.info("Promoción registrada exitosamente. ID: {}, Código: '{}'", saved.getId(), saved.getCodigo());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> obtenerTodas() {
        log.info("Obteniendo listado completo de promociones.");
        return promocionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionResponseDTO validarPromocion(String codigo) {
        String codigoUpper = codigo.trim().toUpperCase();
        log.info("Validando código promocional: '{}'", codigoUpper);

        Promocion prom = promocionRepository.findByCodigoIgnoreCase(codigoUpper)
                .orElseThrow(() -> {
                    log.warn("Validación fallida: Código '{}' no encontrado en la base de datos.", codigoUpper);
                    return new PromocionNoEncontradaException("La promoción con el código '" + codigo + "' no fue encontrada.");
                });

        LocalDate hoy = LocalDate.now();

        // 1. Validar Estado ACTIVO/INACTIVO
        if (!"ACTIVO".equalsIgnoreCase(prom.getEstado())) {
            log.warn("Validación fallida: Código '{}' está INACTIVO.", codigoUpper);
            throw new PromocionCaducadaException("La promoción con el código '" + codigo + "' está inactiva.");
        }

        // 2. Validar que la promoción ya haya comenzado
        if (hoy.isBefore(prom.getFechaInicio())) {
            log.warn("Validación fallida: Código '{}' aún no entra en vigencia (Inicia el {}).", codigoUpper, prom.getFechaInicio());
            throw new PromocionCaducadaException("La promoción con el código '" + codigo + "' aún no ha comenzado. Válida desde: " + prom.getFechaInicio());
        }

        // 3. Validar fecha de expiración
        if (hoy.isAfter(prom.getFechaFin())) {
            log.warn("Validación fallida: Código '{}' ha caducado (Expiró el {}).", codigoUpper, prom.getFechaFin());
            throw new PromocionCaducadaException("La promoción con el código '" + codigo + "' ha caducado. Expiró el: " + prom.getFechaFin());
        }

        log.info("Código promocional '{}' validado con éxito. Porcentaje descuento: {}%", codigoUpper, prom.getPorcentajeDescuento());
        return mapToResponse(prom);
    }

    // Helper manual de mapeo
    private PromocionResponseDTO mapToResponse(Promocion p) {
        LocalDate hoy = LocalDate.now();
        boolean vigente = "ACTIVO".equalsIgnoreCase(p.getEstado()) &&
                !hoy.isBefore(p.getFechaInicio()) &&
                !hoy.isAfter(p.getFechaFin());

        return PromocionResponseDTO.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .porcentajeDescuento(p.getPorcentajeDescuento())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .estado(p.getEstado())
                .vigente(vigente)
                .build();
    }
}
