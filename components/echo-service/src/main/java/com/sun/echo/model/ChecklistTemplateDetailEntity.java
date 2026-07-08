package com.sun.echo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Detail object (description and remote-object references) for a checklist
 * template. Linked to its template via the inherited ownerId.
 */
@Entity
@Table(name = "echo_checklist_template_details")
public class ChecklistTemplateDetailEntity extends AbstractDetailEntity {
}
