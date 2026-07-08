package com.sun.fates.service;

import com.sun.base.service.BaseService;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.repository.PersonRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonService extends BaseService<PersonEntity> {

  private final PersonRepository personRepository;

  public PersonService(PersonRepository repository) {
    super(repository);
    this.personRepository = repository;
  }

  public Optional<PersonEntity> locate(UUID id) {
    return findById(id);
  }

  public Optional<PersonEntity> findByEmail(String email) {
    return personRepository.findByEmail(email);
  }
}
