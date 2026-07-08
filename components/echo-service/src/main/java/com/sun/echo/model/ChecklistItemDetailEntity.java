package com.sun.echo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Detail object (description and remote-object references) for a checklist item,
 * for example an attached image. Linked to its item via the inherited ownerId.
 */
@Entity
@Table(name = "echo_checklist_item_details")
public class ChecklistItemDetailEntity extends AbstractDetailEntity {
}
