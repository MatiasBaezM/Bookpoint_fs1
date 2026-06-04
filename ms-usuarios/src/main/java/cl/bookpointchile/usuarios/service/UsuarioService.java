package cl.bookpointchile.usuarios.service;

import cl.bookpointchile.usuarios.dto.ActualizarRolRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioRegistroRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioResponseDTO;

public interface UsuarioService {
    UsuarioResponseDTO registrarUsuario(UsuarioRegistroRequestDTO request);
    UsuarioResponseDTO obtenerUsuarioPorId(Long id);
    UsuarioResponseDTO actualizarRol(Long id, ActualizarRolRequestDTO request);
}
