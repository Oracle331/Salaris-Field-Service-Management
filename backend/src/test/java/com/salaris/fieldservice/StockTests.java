package com.salaris.fieldservice;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.salaris.fieldservice.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class StockTests {
  @Autowired JdbcTemplate jdbc;
  @PersistenceContext private EntityManager em;

  @Test
  void testStockItemPersistence() {
    StockItem item = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    em.persist(item);
    em.flush();
    StockItem saved = em.find(StockItem.class, item.id);
    assertThat(saved).isNotNull();
    assertThat(saved.name).isEqualTo("Filter 1");
    assertThat(saved.kind).isEqualTo("consumable");
    assertThat(saved.filterSize).isEqualTo("40x20x10");
    assertThat(saved.quantity).isEqualTo(50);
    assertThat(saved.location).isEqualTo("Office A");
  }

  @Test
  void testStockLocationPersistence() {
    StockLocation location = new StockLocation("Office A", "office", "OFFICE-001");
    em.persist(location);
    em.flush();
    StockLocation saved = em.find(StockLocation.class, location.id);
    assertThat(saved).isNotNull();
    assertThat(saved.name).isEqualTo("Office A");
    assertThat(saved.type).isEqualTo("office");
    assertThat(saved.locationCode).isEqualTo("OFFICE-001");
  }

  @Test
  void testStockItemEqualsAndHashCode() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    assertThat(item1).isEqualTo(item2);
    assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
  }

  @Test
  void testStockItemDifferentLocation() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Truck B");
    assertThat(item1).isNotEqualTo(item2);
  }

  @Test
  void testStockItemDifferentFilterSize() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 1", "consumable", "45x25x15", 50, "Office A");
    assertThat(item1).isNotEqualTo(item2);
  }

  @Test
  void testStockItemDifferentKind() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 1", "reusable", "40x20x10", 50, "Office A");
    assertThat(item1).isNotEqualTo(item2);
  }

  @Test
  void testStockItemDifferentQuantity() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 1", "consumable", "40x20x10", 45, "Office A");
    assertThat(item1).isNotEqualTo(item2);
  }

  @Test
  void testStockItemDifferentName() {
    StockItem item1 = new StockItem("Filter 1", "consumable", "40x20x10", 50, "Office A");
    StockItem item2 = new StockItem("Filter 2", "consumable", "40x20x10", 50, "Office A");
    assertThat(item1).isNotEqualTo(item2);
  }
}