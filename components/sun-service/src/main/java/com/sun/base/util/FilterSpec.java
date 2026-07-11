package com.sun.base.util;

/**
 * A single filter criterion, independent of any GraphQL codegen type.
 *
 * @param field the entity field path (e.g. "title" or "owner.name")
 * @param operator the filter operator name (EQUALS, MATCHES, IN, etc.)
 * @param value the comparison value (comma-separated for IN)
 */
public record FilterSpec(String field, String operator, String value) {
}
