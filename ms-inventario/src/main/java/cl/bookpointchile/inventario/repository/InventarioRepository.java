package cl.bookpointchile.inventario.repository;

import cl.bookpointchile.inventario.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    List<Inventario> findBySucursalId(Long sucursalId);

    @Query("SELECT i FROM Inventario i JOIN FETCH i.sucursal WHERE i.cantidad <= i.stockMinimo")
    List<Inventario> findAlertasStock();
}
