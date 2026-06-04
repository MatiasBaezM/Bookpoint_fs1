package cl.bookpointchile.facturacion.repository;

import cl.bookpointchile.facturacion.model.DocumentoTributario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentoTributarioRepository extends JpaRepository<DocumentoTributario, Long> {
    Optional<DocumentoTributario> findByFolioVenta(String folioVenta);
    boolean existsByFolioVenta(String folioVenta);
}
