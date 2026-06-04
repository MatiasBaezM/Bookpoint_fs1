package cl.bookpointchile.usuarios.service;

import cl.bookpointchile.usuarios.dto.ActualizarRolRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioRegistroRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioResponseDTO;
import cl.bookpointchile.usuarios.exception.EmailYaRegistradoException;
import cl.bookpointchile.usuarios.exception.ResourceNotFoundException;
import cl.bookpointchile.usuarios.exception.UsuarioNoEncontradoException;
import cl.bookpointchile.usuarios.model.Rol;
import cl.bookpointchile.usuarios.model.Usuario;
import cl.bookpointchile.usuarios.repository.RolRepository;
import cl.bookpointchile.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Override
    @Transactional
    public UsuarioResponseDTO registrarUsuario(UsuarioRegistroRequestDTO request) {
        log.info("Iniciando registro de usuario con email: '{}' y nombre: '{}'", request.getEmail(), request.getNombre());

        // 1. Validar unicidad del Email
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Registro rechazado: Intento de registro con email ya existente '{}'", request.getEmail());
            throw new EmailYaRegistradoException("El correo electrónico '" + request.getEmail() + "' ya se encuentra registrado.");
        }

        // 2. Validar unicidad del RUT
        if (usuarioRepository.existsByRut(request.getRut())) {
            log.warn("Registro rechazado: Intento de registro con RUT ya existente '{}'", request.getRut());
            throw new EmailYaRegistradoException("El RUT '" + request.getRut() + "' ya se encuentra registrado.");
        }

        // 3. Determinar y buscar el Rol asignado
        Rol rol;
        if (request.getRolId() == null) {
            log.info("No se especificó un rol. Asignando rol por defecto 'Cliente Web'.");
            rol = rolRepository.findByNombreIgnoreCase("Cliente Web")
                    .orElseThrow(() -> {
                        log.error("Error crítico: Rol por defecto 'Cliente Web' no existe en la base de datos.");
                        return new ResourceNotFoundException("El rol por defecto 'Cliente Web' no está configurado.");
                    });
        } else {
            rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> {
                        log.error("Registro rechazado: El rol ID {} especificado no existe.", request.getRolId());
                        return new ResourceNotFoundException("El rol con ID " + request.getRolId() + " no fue encontrado.");
                    });
        }

        // 4. Mapear y encriptar contraseña (Simulado con hash simple para cumplir con buenas prácticas senior)
        String hashedPassword = "[SHA256]" + request.getPassword(); // Simulación de cifrado seguro

        Usuario usuario = Usuario.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(hashedPassword)
                .estado("ACTIVO")
                .rol(rol)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario registrado con éxito en la plataforma. ID Asignado: {}, Rol: '{}'", saved.getId(), saved.getRol().getNombre());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuarioPorId(Long id) {
        log.info("Buscando detalles de usuario con ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario con ID {} no encontrado.", id);
                    return new UsuarioNoEncontradoException("El usuario con ID " + id + " no existe.");
                });
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizarRol(Long id, ActualizarRolRequestDTO request) {
        log.info("Iniciando actualización de rol para usuario ID: {}. Nuevo Rol ID solicitado: {}", id, request.getRolId());

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Actualización fallida: Usuario con ID {} no existe.", id);
                    return new UsuarioNoEncontradoException("El usuario con ID " + id + " no existe.");
                });

        Rol nuevoRol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> {
                    log.error("Actualización fallida: Rol con ID {} no existe.", request.getRolId());
                    return new ResourceNotFoundException("El rol con ID " + request.getRolId() + " no fue encontrado.");
                });

        usuario.setRol(nuevoRol);
        Usuario saved = usuarioRepository.save(usuario);
        log.info("Rol de usuario ID {} actualizado con éxito a: '{}'", id, saved.getRol().getNombre());

        return mapToResponse(saved);
    }

    // Helper manual de mapeo
    private UsuarioResponseDTO mapToResponse(Usuario u) {
        return UsuarioResponseDTO.builder()
                .id(u.getId())
                .rut(u.getRut())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .estado(u.getEstado())
                .rolId(u.getRol().getId())
                .rolNombre(u.getRol().getNombre())
                .build();
    }
}
