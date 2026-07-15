package cl.bookpointchile.usuarios.controller;

import cl.bookpointchile.usuarios.dto.ActualizarRolRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioRegistroRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioResponseDTO;
import cl.bookpointchile.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la integración segura Frontend-Backend en patrón CSR
@Tag(name = "Usuarios", description = "Registro, consulta y administración de roles de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Registrar un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario duplicado", content = @Content)
    })
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(
            @Valid @RequestBody UsuarioRegistroRequestDTO request) {
        UsuarioResponseDTO response = usuarioService.registrarUsuario(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe usuario con ese ID", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorId(
            @Parameter(description = "ID del usuario") @PathVariable Long id) {
        UsuarioResponseDTO response = usuarioService.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener usuario por RUT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe usuario con ese RUT", content = @Content)
    })
    @GetMapping("/rut/{rut}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorRut(
            @Parameter(description = "RUT del usuario (ej: 12345678-9)") @PathVariable String rut) {
        UsuarioResponseDTO response = usuarioService.obtenerUsuarioPorRut(rut);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar rol de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe usuario con ese ID", content = @Content)
    })
    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponseDTO> actualizarRol(
            @Parameter(description = "ID del usuario") @PathVariable Long id,
            @Valid @RequestBody ActualizarRolRequestDTO request) {
        UsuarioResponseDTO response = usuarioService.actualizarRol(id, request);
        return ResponseEntity.ok(response);
    }
}
