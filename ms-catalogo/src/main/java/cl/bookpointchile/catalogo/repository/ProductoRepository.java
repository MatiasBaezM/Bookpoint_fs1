package cl.bookpointchile.catalogo.repository;

import cl.bookpointchile.catalogo.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("SELECT DISTINCT p FROM Producto p LEFT JOIN FETCH p.resenas WHERE " +
           "(:autor IS NULL OR LOWER(p.autor) LIKE LOWER(CONCAT('%', :autor, '%'))) AND " +
           "(:editorial IS NULL OR LOWER(p.editorial) LIKE LOWER(CONCAT('%', :editorial, '%'))) AND " +
           "(:categoria IS NULL OR LOWER(p.categoria) = LOWER(:categoria)) AND " +
           "(:precioMin IS NULL OR p.precio >= :precioMin) AND " +
           "(:precioMax IS NULL OR p.precio <= :precioMax)")
    Page<Producto> buscarConFiltros(
            @Param("autor") String autor,
            @Param("editorial") String editorial,
            @Param("categoria") String categoria,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            Pageable pageable);
}
