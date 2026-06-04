package cl.bookpointchile.proveedores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearProveedorRequestDTO {

    @NotBlank(message = "El RUT del proveedor es obligatorio")
    private String rut;

    @NotBlank(message = "La razón social del proveedor es obligatoria")
    private String razonSocial;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto debe tener un formato válido")
    private String emailContacto;

    private String telefono;
}
