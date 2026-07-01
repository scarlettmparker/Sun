package com.sun.fates.service;

import com.sun.base.service.BaseService;
import com.sun.fates.model.PlaceEntity;
import com.sun.fates.repository.PlaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("fatesTransactionManager")
public class PlaceService extends BaseService<PlaceEntity> {

  public PlaceService(PlaceRepository repository) {
    super(repository);
  }

  public Optional<PlaceEntity> locate(UUID id) {
    return findById(id);
  }
}
