package cl.bookpointchile.bodega.repository;

import cl.bookpointchile.bodega.model.OrdenPicking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdenPickingRepository extends JpaRepository<OrdenPicking, Long> {
    Optional<OrdenPicking> findByVentaId(Long ventaId);
    boolean existsByVentaId(Long ventaId);
}
