package com.sun.hades.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.repository.ReaderAnnotationRepository;
import com.sun.hades.repository.ReaderPositionRepository;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.repository.ReaderVoteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderAnnotationServiceTest {

  @Mock private ReaderAnnotationRepository annotationRepository;
  @Mock private ReaderTextRepository textRepository;
  @Mock private ReaderPositionRepository positionRepository;
  @Mock private ReaderVoteRepository voteRepository;

  @InjectMocks private ReaderAnnotationService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID textId = UUID.randomUUID();

  @BeforeEach
  void setUser() {
    UserContextHolder.setUserId(userId);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void createAnnotation_createsPositionForFreshRange() {
    when(textRepository.findByIdForUpdate(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    when(annotationRepository.findByTextId(textId)).thenReturn(List.of());
    when(positionRepository.findByTextId(textId)).thenReturn(List.of());
    when(positionRepository.findByTextIdAndStartOffsetAndEndOffset(textId, 0, 10))
        .thenReturn(Optional.empty());
    ReaderPositionEntity savedPosition = position(textId, 0, 10);
    when(positionRepository.save(any())).thenReturn(savedPosition);
    ReaderAnnotationEntity savedAnnotation = annotation(savedPosition.getId());
    when(annotationRepository.save(any())).thenReturn(savedAnnotation);

    UUID id = service.createAnnotation(textId, 0, 10, "body");

    assertThat(id).isEqualTo(savedAnnotation.getId());
    verify(positionRepository).save(any());
  }

  @Test
  void createAnnotation_rejectsOverlappingActiveRange() {
    ReaderPositionEntity existing = position(textId, 10, 20);
    when(textRepository.findByIdForUpdate(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    when(positionRepository.findByTextId(textId)).thenReturn(List.of(existing));
    ReaderAnnotationEntity active = annotation(existing.getId());
    active.setStatus(ReaderStatus.ACTIVE);
    when(annotationRepository.findByTextId(textId)).thenReturn(List.of(active));

    assertThatThrownBy(() -> service.createAnnotation(textId, 15, 25, "body"))
        .hasMessageContaining("overlaps");

    verify(positionRepository, never()).save(any());
    verify(annotationRepository, never()).save(any());
  }

  @Test
  void createAnnotation_allowsExactRangeCoAnnotation() {
    ReaderPositionEntity existing = position(textId, 10, 20);
    when(textRepository.findByIdForUpdate(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    when(positionRepository.findByTextId(textId)).thenReturn(List.of(existing));
    ReaderAnnotationEntity active = annotation(existing.getId());
    active.setStatus(ReaderStatus.ACTIVE);
    when(annotationRepository.findByTextId(textId)).thenReturn(List.of(active));
    when(positionRepository.findByTextIdAndStartOffsetAndEndOffset(textId, 10, 20))
        .thenReturn(Optional.of(existing));
    when(annotationRepository.save(any())).thenReturn(annotation(existing.getId()));

    UUID id = service.createAnnotation(textId, 10, 20, "body");

    assertThat(id).isNotNull();
    verify(positionRepository, never()).save(any());
    verify(annotationRepository).save(any());
  }

  @Test
  void createAnnotation_reusesExactRangePosition() {
    ReaderPositionEntity existing = position(textId, 10, 20);
    when(textRepository.findByIdForUpdate(textId)).thenReturn(Optional.of(new ReaderTextEntity()));
    when(positionRepository.findByTextId(textId)).thenReturn(List.of(existing));
    when(annotationRepository.findByTextId(textId)).thenReturn(List.of());
    when(positionRepository.findByTextIdAndStartOffsetAndEndOffset(textId, 10, 20))
        .thenReturn(Optional.of(existing));
    when(annotationRepository.save(any())).thenReturn(annotation(existing.getId()));

    service.createAnnotation(textId, 10, 20, "body");

    verify(positionRepository, never()).save(any());
  }

  @Test
  void createAnnotation_requiresAuthentication() {
    UserContextHolder.clear();

    assertThatThrownBy(() -> service.createAnnotation(textId, 0, 10, "body"))
        .hasMessageContaining("Authentication");
  }

  @Test
  void deleteAnnotation_removesPositionWhenLastAnnotationRemoved() {
    UUID annotationId = UUID.randomUUID();
    UUID positionId = UUID.randomUUID();
    ReaderAnnotationEntity annotation = annotation(positionId);
    annotation.setCreatedBy(userId);
    when(annotationRepository.findById(annotationId)).thenReturn(Optional.of(annotation));
    when(annotationRepository.countByPositionId(positionId)).thenReturn(0L);

    service.deleteAnnotation(annotationId);

    verify(voteRepository).deleteByTargetTypeAndTargetId(any(), any());
    verify(annotationRepository).deleteById(annotationId);
    verify(positionRepository).deleteById(positionId);
  }

  private ReaderPositionEntity position(UUID textId, int start, int end) {
    ReaderPositionEntity p = new ReaderPositionEntity();
    p.setId(UUID.randomUUID());
    p.setTextId(textId);
    p.setStartOffset(start);
    p.setEndOffset(end);
    return p;
  }

  private ReaderAnnotationEntity annotation(UUID positionId) {
    ReaderAnnotationEntity a = new ReaderAnnotationEntity();
    a.setId(UUID.randomUUID());
    a.setPositionId(positionId);
    a.setBody("body");
    return a;
  }
}
