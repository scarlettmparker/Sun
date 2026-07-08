package com.sun.hades.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderVoteEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.repository.ReaderAnnotationRepository;
import com.sun.hades.repository.ReaderCommentRepository;
import com.sun.hades.repository.ReaderVoteRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderVoteServiceTest {

  @Mock private ReaderVoteRepository voteRepository;
  @Mock private ReaderAnnotationRepository annotationRepository;
  @Mock private ReaderCommentRepository commentRepository;

  private ReaderVoteService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID targetId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new ReaderVoteService(voteRepository, annotationRepository, commentRepository, -3);
    UserContextHolder.setUserId(userId);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void vote_downvoteToThreshold_hidesAnnotation() {
    ReaderAnnotationEntity annotation = annotation(0, 2);
    when(voteRepository.findByAccountIdAndTargetTypeAndTargetId(
        userId, ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(Optional.empty());
    when(annotationRepository.findById(targetId)).thenReturn(Optional.of(annotation));

    service.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.DOWN);

    assertThat(annotation.getDownvotes()).isEqualTo(3);
    assertThat(annotation.getStatus()).isEqualTo(ReaderStatus.HIDDEN);
  }

  @Test
  void vote_upvoteRecovers_reactivatesAnnotation() {
    ReaderAnnotationEntity annotation = annotation(0, 3);
    annotation.setStatus(ReaderStatus.HIDDEN);
    when(voteRepository.findByAccountIdAndTargetTypeAndTargetId(
        userId, ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(Optional.empty());
    when(annotationRepository.findById(targetId)).thenReturn(Optional.of(annotation));

    service.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP);

    assertThat(annotation.getUpvotes()).isEqualTo(1);
    assertThat(annotation.getStatus()).isEqualTo(ReaderStatus.ACTIVE);
  }

  @Test
  void vote_repeatVote_togglesOff() {
    ReaderAnnotationEntity annotation = annotation(1, 0);
    ReaderVoteEntity existing = new ReaderVoteEntity();
    existing.setValue(VoteValue.UP);
    when(voteRepository.findByAccountIdAndTargetTypeAndTargetId(
        userId, ReaderVoteTarget.ANNOTATION, targetId)).thenReturn(Optional.of(existing));
    when(annotationRepository.findById(targetId)).thenReturn(Optional.of(annotation));

    service.vote(ReaderVoteTarget.ANNOTATION, targetId, VoteValue.UP);

    assertThat(annotation.getUpvotes()).isZero();
    verify(voteRepository).delete(existing);
  }

  private ReaderAnnotationEntity annotation(int up, int down) {
    ReaderAnnotationEntity a = new ReaderAnnotationEntity();
    a.setUpvotes(up);
    a.setDownvotes(down);
    return a;
  }
}
