package com.sun.echo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Detail object (description and remote-object references) for a checklist
 * entry. Linked to its entry via the inherited ownerId.
 */
@Entity
@Table(name = "checklist_entry_details")
public class ChecklistEntryDetailEntity extends AbstractDetailEntity {
}
