package com.sun.hades.graphql.services;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.model.enums.AccountType;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.graphql.mappers.ReaderObjectReferenceMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.service.DiscordOAuthService;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.RemoteObjectReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderAccountGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderAccountGraphQLService.class);

  private final ReaderAccountService accountService;
  private final ReaderAnnotationService annotationService;
  private final PrivateNoteService privateNoteService;
  private final DiscordOAuthService discordOAuthService;
  private final AccountService gaiaAccountService;
  private final JwtService jwtService;
  private final ReaderAccountMapper accountMapper;
  private final ReaderObjectReferenceMapper objectReferenceMapper;
  private final RemoteUserMapper remoteUserMapper;

  public ReaderAccountGraphQLService(ReaderAccountService accountService,
      ReaderAnnotationService annotationService, PrivateNoteService privateNoteService,
      DiscordOAuthService discordOAuthService, AccountService gaiaAccountService,
      JwtService jwtService, ReaderAccountMapper accountMapper,
      ReaderObjectReferenceMapper objectReferenceMapper, RemoteUserMapper remoteUserMapper) {
    this.accountService = accountService;
    this.annotationService = annotationService;
    this.privateNoteService = privateNoteService;
    this.discordOAuthService = discordOAuthService;
    this.gaiaAccountService = gaiaAccountService;
    this.jwtService = jwtService;
    this.accountMapper = accountMapper;
    this.objectReferenceMapper = objectReferenceMapper;
    this.remoteUserMapper = remoteUserMapper;
  }

  /**
   * Returns the current member's reader account.
   *
   * @return the reader account, or null
   */
  @Transactional(readOnly = true)
  public ReaderAccount readerAccount() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      return null;
    }
    return accountService.findByGaiaAccountId(userId).map(accountMapper::map).orElse(null);
  }

  /**
   * Locates reader accounts for a set of remote users.
   *
   * @param remoteUsers the remote-user references
   * @return the matching reader accounts
   */
  @Transactional(readOnly = true)
  public List<ReaderAccount> readerAccounts(List<RemoteUserInput> remoteUsers) {
    List<String> discordIds = remoteUsers == null ? List.of()
        : remoteUsers.stream()
            .filter(r -> r.getType() == RemoteUserType.DISCORD)
            .map(RemoteUserInput::getId)
            .distinct()
            .toList();
    return accountService.findByDiscordIds(discordIds).stream()
        .map(accountMapper::map)
        .toList();
  }

  /**
   * Searches reader accounts by username.
   *
   * @param query the username fragment
   * @param pagination the pagination input
   * @return the matching reader accounts
   */
  @Transactional(readOnly = true)
  public List<ReaderAccount> searchReaderAccounts(String query, PaginationInput pagination) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    Pageable pageable = pagination == null
        ? PageRequest.of(0, 10)
        : HadesGraphQLSupport.toPageable(pagination, "globalName", Sort.Direction.ASC);
    List<ReaderAccountEntity> entities = accountService.searchByUsername(query, pageable);
    UUID viewer = UserContextHolder.getUserId();
    return entities.stream()
        .filter(e -> {
          UUID gaiaId = e.getGaiaAccountId();
          if (gaiaId == null) {
            return false;
          }
          if (viewer != null && gaiaId.equals(viewer)) {
            return false;
          }
          return gaiaAccountService.findById(gaiaId)
              .map(a -> a.getAccountType() == AccountType.HUMAN)
              .orElse(false);
        })
        .map(e -> accountMapper.map(e))
        .toList();
  }

  /**
   * Finds annotations and private notes that reference any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  @Transactional(readOnly = true)
  public List<ReaderObjectReference> locateRemoteObjects(List<String> ids) {
    List<RemoteObjectReference> out = new ArrayList<>();
    out.addAll(annotationService.locateRemoteObjects(ids));
    out.addAll(privateNoteService.locateRemoteObjects(ids));
    return out.stream().map(objectReferenceMapper::map).toList();
  }

  /**
   * Exchanges a Discord authorization code for a JWT, upserting the gaia account
   * and reader profile.
   *
   * <p>Deactivated accounts get no token; the result flags that a reactivation
   * email is required before the member can sign back in.
   *
   * @param code the authorization code
   * @param state the OAuth state token
   * @return the login result with the JWT
   */
  @Transactional
  public DiscordLoginResult discordLogin(String code, String state) {
    DiscordOAuthService.DiscordProfile profile = discordOAuthService.exchange(code);
    AccountEntity account = gaiaAccountService.upsertProviderAccount(
        "discord", profile.discordId(), profile.username(), profile.globalName(),
        profile.email());
    UUID readerAccountId = accountService.upsertFromDiscord(
        account.getId(), profile.discordId(), profile.username(),
        profile.globalName(), profile.avatar(), profile.cefrLevel(), profile.roles());
    boolean requiresReactivation = account.getStatus() == AccountStatus.DEACTIVATED;
    if (requiresReactivation) {
      logger.info("Discord login for deactivated account {}", account.getId());
      return DiscordLoginResult.newBuilder()
          .token("")
          .accountId(account.getId().toString())
          .readerAccountId(readerAccountId.toString())
          .requiresReactivation(true)
          .build();
    }
    String token = jwtService.generateToken(account.getId(), account.getPersonId());
    return DiscordLoginResult.newBuilder()
        .token(token)
        .accountId(account.getId().toString())
        .readerAccountId(readerAccountId.toString())
        .requiresReactivation(false)
        .build();
  }
}
