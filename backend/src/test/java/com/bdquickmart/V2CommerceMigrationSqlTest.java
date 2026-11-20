package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/** Structural checks on the Phase 1 Flyway migration (no database required). */
@DisplayName("V2 commerce migration SQL")
class V2CommerceMigrationSqlTest {

    private String sql() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__commerce_schema.sql");
        return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("1. All commerce tables are created")
    void createsAllTables() throws Exception {
        String sql = sql();
        for (String table : new String[]{"categories", "products", "inventory",
                "inventory_adjustments", "orders", "order_items",
                "order_status_events", "rider_availability"}) {
            assertTrue(sql.contains("CREATE TABLE " + table + " ("), "missing table: " + table);
        }
    }

    @Test
    @DisplayName("2. Stock, money and order constraints are present")
    void hasIntegrityConstraints() throws Exception {
        String sql = sql();
        assertTrue(sql.contains("ck_inventory_qty     CHECK (quantity_on_hand >= 0)"));
        assertTrue(sql.contains("uk_orders_idempotency_key UNIQUE (idempotency_key)"));
        assertTrue(sql.contains("fk_orders_customer FOREIGN KEY (customer_id)"));
        assertTrue(sql.contains("NUMERIC(12,2)"));
        assertTrue(sql.contains("TIMESTAMPTZ"));
        assertFalse(sql.toLowerCase().contains("engine=innodb"));
    }

    @Test
    @DisplayName("3. Canonical order statuses match the client contract")
    void hasFullStatusLifecycle() throws Exception {
        String sql = sql();
        for (String status : new String[]{"PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP",
                "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED",
                "REJECTED", "FAILED_DELIVERY"}) {
            assertTrue(sql.contains("'" + status + "'"), "missing status: " + status);
        }
    }
}
