package com.sun.icarus.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.model.enums.ThreadStatus;
import com.sun.icarus.repository.ForumThreadRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for discussion threads.
 */
@Service
@Transactional("icarusTransactionManager")
public class ForumThreadService extends BaseService<ForumThreadEntity> {

  private final ForumThreadRepository threadRepository;

  public ForumThreadService(ForumThreadRepository repository) {
    super(repository);
    this.threadRepository = repository;
  }

  /**
   * Lists threads attached to a remote object.
   *
   * @param remoteObject the remote object id
   * @return the threads
   */
  public List<ForumThreadEntity> listForRemoteObject(String remoteObject) {
    return threadRepository.findByRemoteObjectsIn(new String[] {remoteObject});
  }

  /**
   * Creates a thread attached to a remote object.
   *
   * @param title the thread title
   * @param remoteObject the remote object id
   * @return the thread id
   */
  public UUID create(String title, String remoteObject) {
    requireUser();
    ForumThreadEntity thread = new ForumThreadEntity();
    thread.setTitle(title);
    thread.setStatus(ThreadStatus.ACTIVE);
    thread.setRemoteObject(new ArrayList<>(List.of(remoteObject)));
    return threadRepository.save(thread).getId();
  }

  /**
   * Sets a thread's status.
   *
   * @param id the thread id
   * @param status the new status
   * @return the thread id
   */
  public UUID setStatus(UUID id, ThreadStatus status) {
    requireUser();
    ForumThreadEntity thread = threadRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + id));
    thread.setStatus(status);
    return threadRepository.save(thread).getId();
  }

  /**
   * Attaches an additional remote object id to a thread.
   *
   * @param threadId the thread id
   * @param target the remote object id
   * @return the thread id
   */
  public UUID attach(UUID threadId, String target) {
    ForumThreadEntity thread = threadRepository.findById(threadId)
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
    List<String> remoteObject =
        thread.getRemoteObject() == null ? new ArrayList<>() : new ArrayList<>(thread.getRemoteObject());
    if (!remoteObject.contains(target)) {
      remoteObject.add(target);
    }
    thread.setRemoteObject(remoteObject);
    return threadRepository.save(thread).getId();
  }

  /**
   * Finds threads that reference any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    String[] arr = ids.toArray(new String[0]);
    List<RemoteObjectReference> out = new ArrayList<>();
    threadRepository.findByRemoteObjectsIn(arr).forEach(t ->
        out.add(new RemoteObjectReference(t.getId(), "THREAD", t.getId())));
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
