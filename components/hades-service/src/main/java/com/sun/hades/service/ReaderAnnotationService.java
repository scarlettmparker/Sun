package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.repository.ReaderAnnotationRepository;
import com.sun.hades.repository.ReaderPositionRepository;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.repository.ReaderVoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for annotations and the character-range positions they share.
 */
@Service
@Transactional
public class ReaderAnnotationService extends BaseService<ReaderAnnotationEntity> {

  private final ReaderTextRepository textRepository;
  private final ReaderPositionRepository positionRepository;
  private final ReaderAnnotationRepository annotationRepository;
  private final ReaderVoteRepository voteRepository;

  public ReaderAnnotationService(ReaderAnnotationRepository repository,
      ReaderTextRepository textRepository,
      ReaderPositionRepository positionRepository,
      ReaderVoteRepository voteRepository) {
    super(repository);
    this.annotationRepository = repository;
    this.textRepository = textRepository;
    this.positionRepository = positionRepository;
    this.voteRepository = voteRepository;
  }

  /**
   * Creates an annotation on a range. Rejects ranges that overlap an active
   * annotation; reuses the exact-match position when its annotations are all
   * hidden (override), otherwise creates a new position.
   *
   * @param textId the text id
   * @param startOffset the range start
   * @param endOffset the range end
   * @param body the markdown body
   * @return the new annotation id
   */
  public UUID createAnnotation(UUID textId, int startOffset, int endOffset, String body) {
    requireUser();
    if (startOffset < 0 || endOffset <= startOffset || body == null || body.isBlank()) {
      throw new IllegalArgumentException("Invalid annotation");
    }
    textRepository.findByIdForUpdate(textId)
        .orElseThrow(() -> new IllegalArgumentException("Text not found: " + textId));

    Map<UUID, List<ReaderAnnotationEntity>> byPosition =
        annotationRepository.findByTextId(textId).stream()
            .collect(Collectors.groupingBy(ReaderAnnotationEntity::getPositionId));

    for (ReaderPositionEntity p : positionRepository.findByTextId(textId)) {
      boolean active = byPosition.getOrDefault(p.getId(), List.of()).stream()
          .anyMatch(a -> a.getStatus() == ReaderStatus.ACTIVE);
      if (!active) {
        continue;
      }
      boolean overlaps = startOffset < p.getEndOffset() && p.getStartOffset() < endOffset;
      boolean exact = startOffset == p.getStartOffset() && endOffset == p.getEndOffset();
      if (overlaps) {
        throw new IllegalArgumentException(
            exact ? "Range already has an active annotation" : "Range overlaps an active annotation");
      }
    }

    ReaderPositionEntity position = positionRepository
        .findByTextIdAndStartOffsetAndEndOffset(textId, startOffset, endOffset)
        .orElseGet(() -> {
          ReaderPositionEntity np = new ReaderPositionEntity();
          np.setTextId(textId);
          np.setStartOffset(startOffset);
          np.setEndOffset(endOffset);
          return positionRepository.save(np);
        });

    ReaderAnnotationEntity annotation = new ReaderAnnotationEntity();
    annotation.setPositionId(position.getId());
    annotation.setBody(body);
    annotation.setStatus(ReaderStatus.ACTIVE);
    return annotationRepository.save(annotation).getId();
  }

  /**
   * Updates an annotation's body (author only).
   *
   * @param id the annotation id
   * @param body the new markdown body
   * @return the annotation id
   */
  public UUID editAnnotation(UUID id, String body) {
    UUID userId = requireUser();
    ReaderAnnotationEntity annotation = annotationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + id));
    if (!annotation.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    annotation.setBody(body);
    return annotationRepository.save(annotation).getId();
  }

  /**
   * Deletes an annotation (author only), removing its position when no
   * annotations remain on it.
   *
   * @param id the annotation id
   */
  public void deleteAnnotation(UUID id) {
    UUID userId = requireUser();
    ReaderAnnotationEntity annotation = annotationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + id));
    if (!annotation.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    UUID positionId = annotation.getPositionId();
    voteRepository.deleteByTargetTypeAndTargetId(ReaderVoteTarget.ANNOTATION, id);
    annotationRepository.deleteById(id);
    if (annotationRepository.countByPositionId(positionId) == 0) {
      positionRepository.deleteById(positionId);
    }
  }

  /**
   * Lists annotations for a text, optionally including hidden ones.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @return the annotations
   */
  public List<ReaderAnnotationEntity> listForText(UUID textId, boolean includeHidden) {
    List<ReaderAnnotationEntity> all = annotationRepository.findByTextId(textId);
    return includeHidden
        ? all
        : all.stream().filter(a -> a.getStatus() == ReaderStatus.ACTIVE).toList();
  }

  /**
   * Attaches a remote object id to an annotation if not already present.
   *
   * @param annotationId the annotation id
   * @param target the remote object id
   * @return the annotation id
   */
  public UUID attach(UUID annotationId, String target) {
    ReaderAnnotationEntity annotation = annotationRepository.findById(annotationId)
        .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + annotationId));
    List<String> remoteObject =
        annotation.getRemoteObject() == null ? new ArrayList<>() : new ArrayList<>(annotation.getRemoteObject());
    if (!remoteObject.contains(target)) {
      remoteObject.add(target);
    }
    annotation.setRemoteObject(remoteObject);
    return annotationRepository.save(annotation).getId();
  }

  /**
   * Finds annotations that reference any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    String[] arr = ids.toArray(new String[0]);
    List<RemoteObjectReference> out = new ArrayList<>();
    annotationRepository.findByRemoteObjectsIn(arr).forEach(a ->
        out.add(new RemoteObjectReference(a.getId(), "ANNOTATION", a.getId(), null)));
    return out;
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}
