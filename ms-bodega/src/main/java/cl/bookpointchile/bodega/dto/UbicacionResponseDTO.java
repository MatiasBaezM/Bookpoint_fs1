package cl.bookpointchile.bodega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionResponseDTO {
    private Long id;
    private String pasillo;
    private String estante;
    private String nivel;
    private String codigoBarras;
}
