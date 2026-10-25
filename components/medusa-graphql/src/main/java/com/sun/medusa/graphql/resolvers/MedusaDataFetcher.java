package com.sun.medusa.graphql.resolvers;

import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.medusa.codegen.types.AuditLog;
import com.sun.medusa.codegen.types.AuditLogPage;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.codegen.types.BoardLayout;
import com.sun.medusa.codegen.types.BoardLayoutInput;
import com.sun.medusa.codegen.types.BoardLayoutResponse;
import com.sun.medusa.codegen.types.DeleteResponse;
import com.sun.medusa.codegen.types.MedusaMutations;
import com.sun.medusa.codegen.types.MedusaQueries;
import com.sun.medusa.codegen.types.Piece;
import com.sun.medusa.codegen.types.PieceInput;
import com.sun.medusa.codegen.types.PieceResponse;
import com.sun.medusa.codegen.types.PieceSet;
import com.sun.medusa.codegen.types.PieceSetInput;
import com.sun.medusa.codegen.types.PieceSetResponse;
import com.sun.medusa.graphql.mappers.AuditLogMapper;
import com.sun.medusa.graphql.mappers.BoardLayoutMapper;
import com.sun.medusa.graphql.mappers.PieceMapper;
import com.sun.medusa.graphql.mappers.PieceSetMapper;
import com.sun.medusa.model.BoardLayoutEntity;
import com.sun.medusa.model.PieceDefinitionEntity;
import com.sun.medusa.model.PieceSetEntity;
import com.sun.medusa.service.AuditService;
import com.sun.medusa.service.BoardLayoutService;
import com.sun.medusa.service.PieceService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.medusa.service.PieceSetService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;

@DgsComponent
public class MedusaDataFetcher {

  private final PieceService pieceService;
  private final PieceSetService pieceSetService;
  private final BoardLayoutService boardLayoutService;
  private final AuditService auditService;
  private final PieceMapper pieceMapper;
  private final PieceSetMapper pieceSetMapper;
  private final BoardLayoutMapper boardLayoutMapper;
  private final AuditLogMapper auditLogMapper;

  public MedusaDataFetcher(PieceService pieceService, PieceSetService pieceSetService,
      BoardLayoutService boardLayoutService, AuditService auditService, PieceMapper pieceMapper,
      PieceSetMapper pieceSetMapper, BoardLayoutMapper boardLayoutMapper,
      AuditLogMapper auditLogMapper) {
    this.pieceService = pieceService;
    this.pieceSetService = pieceSetService;
    this.boardLayoutService = boardLayoutService;
    this.auditService = auditService;
    this.pieceMapper = pieceMapper;
    this.pieceSetMapper = pieceSetMapper;
    this.boardLayoutMapper = boardLayoutMapper;
    this.auditLogMapper = auditLogMapper;
  }

  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "medusaQueries")
  public MedusaQueries getMedusaQueries() {
    return MedusaQueries.newBuilder().build();
  }

  @Audited(event = "LIST", entity = "Piece")
  @DgsData(parentType = "MedusaQueries", field = "pieces")
  @PreAuthorize("@permissions.has('graphql.medusa.listPieces')")
  @Transactional(readOnly = true)
  public List<Piece> pieces() {
    return pieceService.list(UserContextHolder.getUserId()).stream()
        .map(e -> pieceMapper.map(e, pieceService.findSprite(e.getId())))
        .collect(Collectors.toList());
  }

  @Audited(event = "LOCATE", entity = "Piece")
  @DgsData(parentType = "MedusaQueries", field = "piece")
  @PreAuthorize("@permissions.has('graphql.medusa.getPiece')")
  @Transactional(readOnly = true)
  public Piece piece(String id) {
    return pieceService.findById(UUID.fromString(id))
        .map(e -> pieceMapper.map(e, pieceService.findSprite(e.getId())))
        .orElseThrow(() -> new RuntimeException("Piece not found"));
  }

  @Audited(event = "LIST", entity = "PieceSet")
  @DgsData(parentType = "MedusaQueries", field = "pieceSets")
  @PreAuthorize("@permissions.has('graphql.medusa.listPieceSets')")
  @Transactional(readOnly = true)
  public List<PieceSet> pieceSets() {
    return pieceSetService.list(UserContextHolder.getUserId()).stream()
        .map(e -> pieceSetMapper.map(e, pieceSetService))
        .collect(Collectors.toList());
  }

  @Audited(event = "LOCATE", entity = "PieceSet")
  @DgsData(parentType = "MedusaQueries", field = "pieceSet")
  @PreAuthorize("@permissions.has('graphql.medusa.getPieceSet')")
  @Transactional(readOnly = true)
  public PieceSet pieceSet(String id) {
    return pieceSetService.findById(UUID.fromString(id))
        .map(e -> pieceSetMapper.map(e, pieceSetService))
        .orElseThrow(() -> new RuntimeException("Piece set not found"));
  }

  @Audited(event = "LIST", entity = "BoardLayout")
  @DgsData(parentType = "MedusaQueries", field = "boardLayouts")
  @PreAuthorize("@permissions.has('graphql.medusa.listBoardLayouts')")
  @Transactional(readOnly = true)
  public List<BoardLayout> boardLayouts() {
    return boardLayoutService.list(UserContextHolder.getUserId()).stream()
        .map(boardLayoutMapper::map)
        .collect(Collectors.toList());
  }

  @Audited(event = "LOCATE", entity = "BoardLayout")
  @DgsData(parentType = "MedusaQueries", field = "boardLayout")
  @PreAuthorize("@permissions.has('graphql.medusa.getBoardLayout')")
  @Transactional(readOnly = true)
  public BoardLayout boardLayout(String id) {
    return boardLayoutService.findById(UUID.fromString(id))
        .map(boardLayoutMapper::map)
        .orElseThrow(() -> new RuntimeException("Board layout not found"));
  }

  @Audited(event = "LOCATE", entity = "BoardLayout")
  @DgsData(parentType = "MedusaQueries", field = "boardLayoutBySlug")
  @PreAuthorize("@permissions.has('graphql.medusa.getBoardLayout')")
  @Transactional(readOnly = true)
  public BoardLayout boardLayoutBySlug(String slug) {
    return boardLayoutService.findBySlug(slug)
        .map(boardLayoutMapper::map)
        .orElseThrow(() -> new RuntimeException("Board layout not found"));
  }

  @Audited(event = "LIST", entity = "AuditLog")
  @DgsData(parentType = "MedusaQueries", field = "auditLogs")
  @PreAuthorize("@permissions.has('graphql.medusa.auditLogs')")
  @Transactional(readOnly = true)
  public AuditLogPage auditLogs(PaginationInput pagination) {
    int p = (pagination == null || pagination.getPage() == null) ? 0 : pagination.getPage();
    int s = (pagination == null || pagination.getSize() == null) ? 50 : pagination.getSize();
    var result = auditService.list(p, s);
    return AuditLogPage.newBuilder()
        .items(result.getContent().stream().map(auditLogMapper::map).collect(Collectors.toList()))
        .total((int) result.getTotalElements())
        .build();
  }

  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Mutation", field = "medusaMutations")
  public MedusaMutations getMedusaMutations() {
    return MedusaMutations.newBuilder().build();
  }

  @Audited(event = "SAVE", entity = "Piece")
  @DgsData(parentType = "MedusaMutations", field = "savePiece")
  @PreAuthorize("@permissions.has('graphql.medusa.savePiece')")
  @Transactional
  public PieceResponse savePiece(PieceInput input) {
    PieceDefinitionEntity entity = pieceMapper.mapInput(input);
    if (entity.getCreatedBy() == null) {
      entity.setCreatedBy(UserContextHolder.getUserId());
    }
    PieceDefinitionEntity saved = pieceService.save(entity,
        input.getSprite().getHeadKey(), input.getSprite().getBodyKey(),
        input.getSprite().getFootKey());
    return PieceResponse.newBuilder()
        .message("Piece saved")
        .piece(pieceMapper.map(saved, pieceService.findSprite(saved.getId())))
        .build();
  }

  @Audited(event = "DELETE", entity = "Piece")
  @DgsData(parentType = "MedusaMutations", field = "deletePiece")
  @PreAuthorize("@permissions.has('graphql.medusa.deletePiece')")
  @Transactional
  public DeleteResponse deletePiece(String id) {
    boolean ok = pieceService.delete(UUID.fromString(id));
    return DeleteResponse.newBuilder().ok(ok).build();
  }

  @Audited(event = "SAVE", entity = "PieceSet")
  @DgsData(parentType = "MedusaMutations", field = "savePieceSet")
  @PreAuthorize("@permissions.has('graphql.medusa.savePieceSet')")
  @Transactional
  public PieceSetResponse savePieceSet(PieceSetInput input) {
    PieceSetEntity entity = pieceSetMapper.mapInput(input);
    if (entity.getCreatedBy() == null) {
      entity.setCreatedBy(UserContextHolder.getUserId());
    }
    var members = pieceSetMapper.mapMembers(input, entity.getId() == null ? null : entity.getId());
    PieceSetEntity saved = pieceSetService.save(entity, members);
    return PieceSetResponse.newBuilder()
        .message("Piece set saved")
        .pieceSet(pieceSetMapper.map(saved, pieceSetService))
        .build();
  }

  @Audited(event = "DELETE", entity = "PieceSet")
  @DgsData(parentType = "MedusaMutations", field = "deletePieceSet")
  @PreAuthorize("@permissions.has('graphql.medusa.deletePieceSet')")
  @Transactional
  public DeleteResponse deletePieceSet(String id) {
    boolean ok = pieceSetService.delete(UUID.fromString(id));
    return DeleteResponse.newBuilder().ok(ok).build();
  }

  @Audited(event = "SAVE", entity = "BoardLayout")
  @DgsData(parentType = "MedusaMutations", field = "saveBoardLayout")
  @PreAuthorize("@permissions.has('graphql.medusa.saveBoardLayout')")
  @Transactional
  public BoardLayoutResponse saveBoardLayout(BoardLayoutInput input) {
    BoardLayoutEntity entity = boardLayoutMapper.mapInput(input);
    if (entity.getCreatedBy() == null) {
      entity.setCreatedBy(UserContextHolder.getUserId());
    }
    BoardLayoutEntity saved = boardLayoutService.save(entity);
    return BoardLayoutResponse.newBuilder()
        .message("Board layout saved")
        .boardLayout(boardLayoutMapper.map(saved))
        .build();
  }

  @Audited(event = "DELETE", entity = "BoardLayout")
  @DgsData(parentType = "MedusaMutations", field = "deleteBoardLayout")
  @PreAuthorize("@permissions.has('graphql.medusa.deleteBoardLayout')")
  @Transactional
  public DeleteResponse deleteBoardLayout(String id) {
    boolean ok = boardLayoutService.delete(UUID.fromString(id));
    return DeleteResponse.newBuilder().ok(ok).build();
  }
}
