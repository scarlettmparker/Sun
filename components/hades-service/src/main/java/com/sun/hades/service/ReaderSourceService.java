package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.repository.ReaderSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for reader text sources.
 */
@Service
@Transactional("hadesTransactionManager")
public class ReaderSourceService extends BaseService<ReaderSourceEntity> {

  public ReaderSourceService(ReaderSourceRepository repository) {
    super(repository);
  }
}
