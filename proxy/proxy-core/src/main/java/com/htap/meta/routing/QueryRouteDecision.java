package com.htap.meta.routing;

/**
 * Type de décision de routage pour une requête SQL.
 *
 * - OLTP_ONLY : exécution uniquement sur la base transactionnelle (PostgreSQL).
 * - OLAP_ONLY : exécution uniquement sur la base analytique (ClickHouse).
 * - HYBRID    : exécution combinée sur les deux ("OLTP" + "OLAP").
 */
public enum QueryRouteDecision {
    OLTP_ONLY,
    OLAP_ONLY,
    HYBRID
}
