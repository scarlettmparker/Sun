package com.sun.gaia.repository;

import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.enums.EntryStatus;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertySetEntryRepository extends BaseRepository<PropertySetEntryEntity> {

  @Query("select e from PropertySetEntryEntity e where e.ownerKey = :ownerKey "
      + "and e.propertySet = :propertySet and e.entryName = :entryName")
  Optional<PropertySetEntryEntity> findEntry(
      @Param("ownerKey") String ownerKey,
      @Param("propertySet") String propertySet,
      @Param("entryName") String entryName);

  @Query("select e from PropertySetEntryEntity e where e.ownerKey = :ownerKey "
      + "and e.propertySet = :propertySet and e.status = :status")
  List<PropertySetEntryEntity> findByStatus(
      @Param("ownerKey") String ownerKey,
      @Param("propertySet") String propertySet,
      @Param("status") EntryStatus status);

  @Query("select e from PropertySetEntryEntity e where e.ownerKey = :ownerKey "
      + "and e.propertySet = :propertySet and e.configurable = :configurable")
  List<PropertySetEntryEntity> findByConfigurable(
      @Param("ownerKey") String ownerKey,
      @Param("propertySet") String propertySet,
      @Param("configurable") boolean configurable);

  /**
   * Active entries whose permission is NULL or matches a caller pattern.
   *
   * @param remoteUserId the Discord snowflake
   * @param ownerKey the property-set owner
   * @param propertySet the property-set name
   * @return the accessible entries
   */
  @Query(
      value =
          "WITH remote_account AS ("
              + "  SELECT id, person_id FROM gaia_accounts"
              + "  WHERE provider = 'discord' AND provider_id = :remoteUserId AND status = 'ACTIVE'"
              + "),"
              + "effective_perms AS ("
              + "  SELECT permission FROM gaia_account_permissions"
              + "  WHERE account_id IN ("
              + "    SELECT id FROM gaia_accounts WHERE person_id = (SELECT person_id FROM remote_account)"
              + "  )"
              + "  UNION"
              + "  SELECT rp.permission FROM gaia_role_permissions rp"
              + "  JOIN gaia_account_roles ar ON ar.role_id = rp.role_id"
              + "  WHERE ar.account_id IN ("
              + "    SELECT id FROM gaia_accounts WHERE person_id = (SELECT person_id FROM remote_account)"
              + "  )"
              + ")"
              + "SELECT e.* FROM gaia_property_set_entries e"
              + " WHERE e.owner_key = :ownerKey"
              + "   AND e.property_set = :propertySet"
              + "   AND e.status = 'ACTIVE'"
              + "   AND ("
              + "     e.values->>'permission' IS NULL"
              + "     OR EXISTS ("
              + "       SELECT 1 FROM effective_perms p"
              + "       WHERE e.values->>'permission' ~ ("
              + "         '^' || replace(replace(p.permission, '.', '\\\\.'), '*', '.*') || '$'"
              + "       )"
              + "     )"
              + "   )"
              + " ORDER BY e.entry_name",
      nativeQuery = true)
  List<PropertySetEntryEntity> findAccessibleEntries(
      @Param("remoteUserId") String remoteUserId,
      @Param("ownerKey") String ownerKey,
      @Param("propertySet") String propertySet);
}
