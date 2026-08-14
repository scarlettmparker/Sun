package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.repository.IpWhitelistEntryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class IpWhitelistServiceTest {

  @Mock
  private IpWhitelistEntryRepository repository;

  private IpWhitelistService service;

  @BeforeEach
  void setUp() {
    service = new IpWhitelistService(repository, false);
  }

  @Test
  void addEntry_createsNewEntryWhenPatternDoesNotExist() {
    when(repository.findByPattern("192.168.0.1")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    IpWhitelistEntryEntity result = service.addEntry("192.168.0.1", "home", false);

    assertThat(result.getPattern()).isEqualTo("192.168.0.1");
    assertThat(result.getDescription()).isEqualTo("home");
    assertThat(result.isEnabled()).isTrue();
    assertThat(result.isImmutable()).isFalse();
  }

  @Test
  void addEntry_rethrowsEnabledEntryIfExists() {
    IpWhitelistEntryEntity existing = new IpWhitelistEntryEntity();
    existing.setPattern("10.0.0.1");
    existing.setEnabled(true);

    when(repository.findByPattern("10.0.0.1")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.addEntry("10.0.0.1", "duplicate", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void addEntry_reEnablesSuspendedEntry() {
    IpWhitelistEntryEntity existing = new IpWhitelistEntryEntity();
    existing.setPattern("10.0.0.2");
    existing.setEnabled(false);
    existing.setDescription("old");

    when(repository.findByPattern("10.0.0.2")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    IpWhitelistEntryEntity result = service.addEntry("10.0.0.2", "new desc", true);

    assertThat(result.isEnabled()).isTrue();
    assertThat(result.getDescription()).isEqualTo("new desc");
    assertThat(result.isImmutable()).isTrue();
    verify(repository).save(existing);
  }

  @Test
  void updateEntry_throwsWhenImmutable() {
    IpWhitelistEntryEntity entry = new IpWhitelistEntryEntity();
    entry.setPattern("192.168.0.1");
    entry.setImmutable(true);

    when(repository.findById(any())).thenReturn(Optional.of(entry));

    assertThatThrownBy(() -> service.updateEntry(UUID.randomUUID(), "10.0.0.1", null, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("immutable");
  }

  @Test
  void updateEntry_togglesEnabled() {
    IpWhitelistEntryEntity entry = new IpWhitelistEntryEntity();
    entry.setPattern("192.168.0.1");
    entry.setEnabled(true);

    when(repository.findById(any())).thenReturn(Optional.of(entry));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    IpWhitelistEntryEntity result = service.updateEntry(UUID.randomUUID(), null, null, false);

    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  void isAllowed_returnsFalseForMismatch() {
    IpWhitelistEntryEntity entry = new IpWhitelistEntryEntity();
    entry.setPattern("10.0.0.0/24");
    entry.setEnabled(true);

    when(repository.findByEnabledTrue()).thenReturn(List.of(entry));

    assertThat(service.isAllowed("10.0.1.1")).isFalse();
  }

  @Test
  void isAllowed_returnsTrueForCidrMatch() {
    IpWhitelistEntryEntity entry = new IpWhitelistEntryEntity();
    entry.setPattern("10.0.0.0/24");
    entry.setEnabled(true);

    when(repository.findByEnabledTrue()).thenReturn(List.of(entry));

    assertThat(service.isAllowed("10.0.0.50")).isTrue();
  }

  @Test
  void isAllowed_returnsTrueForGlobMatch() {
    IpWhitelistEntryEntity entry = new IpWhitelistEntryEntity();
    entry.setPattern("192.168.0.*");
    entry.setEnabled(true);

    when(repository.findByEnabledTrue()).thenReturn(List.of(entry));

    assertThat(service.isAllowed("192.168.0.5")).isTrue();
  }

  @Test
  void isAllowed_skipsDisabledEntries() {
    IpWhitelistEntryEntity enabled = new IpWhitelistEntryEntity();
    enabled.setPattern("10.0.0.0/24");
    enabled.setEnabled(true);

    IpWhitelistEntryEntity disabled = new IpWhitelistEntryEntity();
    disabled.setPattern("192.168.0.1");
    disabled.setEnabled(false);

    when(repository.findByEnabledTrue()).thenReturn(List.of(enabled));

    assertThat(service.isAllowed("192.168.0.1")).isFalse();
    assertThat(service.isAllowed("10.0.0.5")).isTrue();
  }
}
