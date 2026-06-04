package cl.bookpointchile.bodega.repository;

import cl.bookpointchile.bodega.model.UbicacionFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UbicacionFisicaRepository extends JpaRepository<UbicacionFisica, Long> {
    Optional<UbicacionFisica> findByCodigoBarras(String codigoBarras);
    boolean existsByCodigoBarras(String codigoBarras);
}
