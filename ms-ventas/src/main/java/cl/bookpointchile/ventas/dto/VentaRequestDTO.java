package cl.bookpointchile.ventas.dto;

import cl.bookpointchile.ventas.model.TipoVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequestDTO {

    @NotNull(message = "El tipo de venta (PRESENCIAL o ONLINE) es obligatorio")
    private TipoVenta tipoVenta;

    @NotNull(message = "La sucursal de la venta es obligatoria")
    private Long sucursalId; // Sucursal desde la que se vende y de la que se descuenta el stock

    private Long usuarioId; // ID del usuario registrado (opcional; si se provee, se obtienen nombre y RUT automáticamente)
    private String clienteNombre;
    private String clienteRut;
    private String asistenteNombre; // Obligatorio para ventas presenciales en caja
    private String codigoDescuento; // Cupones o convenios (ej: 'DESCUENTO10', 'CONVENIO_ESTUDIANTIL')
    private String tipoDocumento; // BOLETA o FACTURA
    private String razonSocial;
    private String giro;

    @NotEmpty(message = "La venta debe incluir al menos un detalle de producto")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;
}
