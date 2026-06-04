package cl.bookpointchile.sucursales.service;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;
import cl.bookpointchile.sucursales.exception.NombreSucursalDuplicadoException;
import cl.bookpointchile.sucursales.exception.SucursalNoEncontradaException;
import cl.bookpointchile.sucursales.model.Sucursal;
import cl.bookpointchile.sucursales.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository repository;

    @Override
    @Transactional
    public SucursalResponseDTO crearSucursal(SucursalRequestDTO request) {
        String nombreUpper = request.getNombre().trim();
        log.info("Iniciando la inauguración de una nueva sucursal: '{}'", nombreUpper);

        // 1. Validar nombre duplicado
        if (repository.existsByNombreIgnoreCase(nombreUpper)) {
            log.warn("Registro rechazado: Ya existe una sucursal con el nombre '{}'", nombreUpper);
            throw new NombreSucursalDuplicadoException("La sucursal con el nombre '" + request.getNombre() + "' ya existe.");
        }

        // 2. Lógica del Service: Validación y formateo de campos
        String regionFormated = request.getRegion() != null && !request.getRegion().trim().isEmpty()
                ? request.getRegion().trim()
                : "Región Metropolitana"; // Valor por defecto

        String horarioFormated = request.getHorarioAtencion() != null && !request.getHorarioAtencion().trim().isEmpty()
                ? request.getHorarioAtencion().trim()
                : "Lunes a Viernes 09:00 a 19:00"; // Horario corporativo por defecto

        Sucursal nueva = Sucursal.builder()
                .nombre(nombreUpper)
                .direccion(request.getDireccion().trim())
                .comuna(request.getComuna().trim())
                .region(regionFormated)
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .horarioAtencion(horarioFormated)
                .estadoOperativo(request.getEstadoOperativo() != null ? request.getEstadoOperativo().trim().toUpperCase() : "ACTIVO")
                .build();

        Sucursal guardada = repository.save(nueva);
        log.info("Sucursal '{}' inaugurada de manera exitosa. ID asignado: {}", guardada.getNombre(), guardada.getId());

        return mapToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public SucursalResponseDTO obtenerPorId(Long id) {
        log.info("Buscando sucursal por ID: {}", id);
        Sucursal sucursal = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Sucursal con ID {} no encontrada.", id);
                    return new SucursalNoEncontradaException("La sucursal con ID " + id + " no existe.");
                });
        return mapToResponse(sucursal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalResponseDTO> obtenerTodas() {
        log.info("Obteniendo listado completo de sedes operativas.");
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SucursalResponseDTO actualizarSucursal(Long id, SucursalRequestDTO request) {
        log.info("Solicitud de actualización para la sucursal ID: {}", id);

        Sucursal sucursal = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: Sucursal ID {} no existe.", id);
                    return new SucursalNoEncontradaException("La sucursal con ID " + id + " no existe.");
                });

        // Validar cambio de nombre único
        if (!sucursal.getNombre().equalsIgnoreCase(request.getNombre().trim())) {
            if (repository.existsByNombreIgnoreCase(request.getNombre().trim())) {
                log.warn("Actualización fallida: Nombre '{}' ya está en uso por otra sucursal.", request.getNombre());
                throw new NombreSucursalDuplicadoException("Ya existe una sucursal registrada con el nombre '" + request.getNombre() + "'.");
            }
            sucursal.setNombre(request.getNombre().trim());
        }

        // Formateo e inyección lógica en Service
        String regionFormated = request.getRegion() != null && !request.getRegion().trim().isEmpty()
                ? request.getRegion().trim()
                : sucursal.getRegion();

        String horarioFormated = request.getHorarioAtencion() != null && !request.getHorarioAtencion().trim().isEmpty()
                ? request.getHorarioAtencion().trim()
                : sucursal.getHorarioAtencion();

        sucursal.setDireccion(request.getDireccion().trim());
        sucursal.setComuna(request.getComuna().trim());
        sucursal.setRegion(regionFormated);
        sucursal.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : sucursal.getTelefono());
        sucursal.setHorarioAtencion(horarioFormated);
        if (request.getEstadoOperativo() != null) {
            sucursal.setEstadoOperativo(request.getEstadoOperativo().trim().toUpperCase());
        }

        Sucursal actualizada = repository.save(sucursal);
        log.info("Sucursal ID {} actualizada con éxito. Horario: '{}'", id, actualizada.getHorarioAtencion());

        return mapToResponse(actualizada);
    }

    private SucursalResponseDTO mapToResponse(Sucursal s) {
        return SucursalResponseDTO.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .direccion(s.getDireccion())
                .comuna(s.getComuna())
                .region(s.getRegion())
                .telefono(s.getTelefono())
                .horarioAtencion(s.getHorarioAtencion())
                .estadoOperativo(s.getEstadoOperativo())
                .build();
    }
}
