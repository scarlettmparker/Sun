package com.sun.echo.service;

import com.sun.echo.model.AbstractDetailEntity;
import com.sun.echo.model.ChecklistEntryDetailEntity;
import com.sun.echo.model.ChecklistItemDetailEntity;
import com.sun.echo.model.ChecklistTemplateDetailEntity;
import com.sun.echo.repository.ChecklistEntryDetailRepository;
import com.sun.echo.repository.ChecklistItemDetailRepository;
import com.sun.echo.repository.ChecklistTemplateDetailRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing checklist detail records across entries, templates, and
 * items. A detail is a sidecar holding a description and a list of cross-
 * component remote-object references.
 */
@Service
@Transactional
public class ChecklistDetailService {

  private final ChecklistEntryDetailRepository entryDetailRepository;
  private final ChecklistTemplateDetailRepository templateDetailRepository;
  private final ChecklistItemDetailRepository itemDetailRepository;

  public ChecklistDetailService(ChecklistEntryDetailRepository entryDetailRepository,
      ChecklistTemplateDetailRepository templateDetailRepository,
      ChecklistItemDetailRepository itemDetailRepository) {
    this.entryDetailRepository = entryDetailRepository;
    this.templateDetailRepository = templateDetailRepository;
    this.itemDetailRepository = itemDetailRepository;
  }

  /**
   * Finds the detail for an entry.
   *
   * @param ownerId the entry id
   * @return the detail, or empty if none exists
   */
  public Optional<ChecklistEntryDetailEntity> findEntryDetail(UUID ownerId) {
    return entryDetailRepository.findByOwnerId(ownerId);
  }

  /**
   * Finds the detail for a template.
   *
   * @param ownerId the template id
   * @return the detail, or empty if none exists
   */
  public Optional<ChecklistTemplateDetailEntity> findTemplateDetail(UUID ownerId) {
    return templateDetailRepository.findByOwnerId(ownerId);
  }

  /**
   * Finds the detail for an item.
   *
   * @param ownerId the item id
   * @return the detail, or empty if none exists
   */
  public Optional<ChecklistItemDetailEntity> findItemDetail(UUID ownerId) {
    return itemDetailRepository.findByOwnerId(ownerId);
  }

  /**
   * Finds every detail that references any of the given object ids, tagged with
   * its owner type so callers can resolve the owning entity.
   *
   * @param ids the object ids to resolve
   * @return the matching references
   */
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    String[] arr = ids.toArray(new String[0]);
    List<RemoteObjectReference> out = new ArrayList<>();
    entryDetailRepository.findByRemoteObjectsIn(arr).forEach(d ->
        out.add(new RemoteObjectReference(d.getId(), "ENTRY", d.getOwnerId(), d.getDescription())));
    templateDetailRepository.findByRemoteObjectsIn(arr).forEach(d ->
        out.add(new RemoteObjectReference(d.getId(), "TEMPLATE", d.getOwnerId(), d.getDescription())));
    itemDetailRepository.findByRemoteObjectsIn(arr).forEach(d ->
        out.add(new RemoteObjectReference(d.getId(), "ITEM", d.getOwnerId(), d.getDescription())));
    return out;
  }

  /**
   * Attaches an object to a detail. Targets already attached are not duplicated.
   * When no detail exists, an ownerType (ENTRY, TEMPLATE, or ITEM) must be given
   * so one can be created.
   *
   * @param source the owning entity id
   * @param target the object id to attach
   * @param ownerType an optional owner type, used to create a new detail
   * @return the detail id
   */
  public UUID attach(UUID source, String target, String ownerType) {
    if (ownerType != null) {
      return attachTyped(source, target, ownerType);
    }

    Optional<ChecklistEntryDetailEntity> entry = entryDetailRepository.findByOwnerId(source);
    if (entry.isPresent()) {
      appendIfAbsent(entry.get(), target);
      return entryDetailRepository.save(entry.get()).getId();
    }
    Optional<ChecklistTemplateDetailEntity> template = templateDetailRepository.findByOwnerId(source);
    if (template.isPresent()) {
      appendIfAbsent(template.get(), target);
      return templateDetailRepository.save(template.get()).getId();
    }
    Optional<ChecklistItemDetailEntity> item = itemDetailRepository.findByOwnerId(source);
    if (item.isPresent()) {
      appendIfAbsent(item.get(), target);
      return itemDetailRepository.save(item.get()).getId();
    }
    throw new IllegalArgumentException(
        "No detail found for owner " + source + "; provide ownerType to create one");
  }

  /**
   * Attaches an object to a detail of a known type, creating it if missing.
   *
   * @param source the owning entity id
   * @param target the object id to attach
   * @param ownerType the owner type (ENTRY, TEMPLATE, or ITEM)
   * @return the detail id
   */
  private UUID attachTyped(UUID source, String target, String ownerType) {
    switch (ownerType) {
      case "ENTRY": {
        ChecklistEntryDetailEntity d = entryDetailRepository.findByOwnerId(source)
            .orElseGet(() -> {
              ChecklistEntryDetailEntity n = new ChecklistEntryDetailEntity();
              n.setOwnerId(source);
              return n;
            });
        appendIfAbsent(d, target);
        return entryDetailRepository.save(d).getId();
      }
      case "TEMPLATE": {
        ChecklistTemplateDetailEntity d = templateDetailRepository.findByOwnerId(source)
            .orElseGet(() -> {
              ChecklistTemplateDetailEntity n = new ChecklistTemplateDetailEntity();
              n.setOwnerId(source);
              return n;
            });
        appendIfAbsent(d, target);
        return templateDetailRepository.save(d).getId();
      }
      case "ITEM": {
        ChecklistItemDetailEntity d = itemDetailRepository.findByOwnerId(source)
            .orElseGet(() -> {
              ChecklistItemDetailEntity n = new ChecklistItemDetailEntity();
              n.setOwnerId(source);
              return n;
            });
        appendIfAbsent(d, target);
        return itemDetailRepository.save(d).getId();
      }
      default:
        throw new IllegalArgumentException("Unknown ownerType: " + ownerType);
    }
  }

  /**
   * Appends a target to a detail's remote objects if it is not already present.
   *
   * @param detail the detail to update
   * @param target the object id to append
   */
  private void appendIfAbsent(AbstractDetailEntity detail, String target) {
    List<String> remoteObject = detail.getRemoteObject();
    if (remoteObject == null) {
      remoteObject = new ArrayList<>();
    } else {
      remoteObject = new ArrayList<>(remoteObject);
    }
    if (!remoteObject.contains(target)) {
      remoteObject.add(target);
    }
    detail.setRemoteObject(remoteObject);
  }
}
