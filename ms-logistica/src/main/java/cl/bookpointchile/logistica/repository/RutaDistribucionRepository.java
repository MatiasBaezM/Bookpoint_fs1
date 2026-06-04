package cl.bookpointchile.logistica.repository;

import cl.bookpointchile.logistica.model.RutaDistribucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutaDistribucionRepository extends JpaRepository<RutaDistribucion, Long> {
    List<RutaDistribucion> findByDestinoIgnoreCase(String destino);
}
