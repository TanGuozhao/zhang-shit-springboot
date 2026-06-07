package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.Department;
import com.example.platform.user.domain.DepartmentAttributeDefinition;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.AdminDepartmentCreateRequest;
import com.example.platform.user.dto.AdminDepartmentSummaryResponse;
import com.example.platform.user.dto.AdminDepartmentUpdateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionCreateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionResponse;
import com.example.platform.user.dto.DepartmentAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchItemRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchResultItemResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchResultResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberRelationRequest;
import com.example.platform.user.dto.DepartmentMembershipResponse;
import com.example.platform.user.dto.DepartmentTransferRequest;
import com.example.platform.user.dto.DepartmentUserSummaryResponse;
import com.example.platform.user.dto.OrganizationTreeNodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.repository.DepartmentAttributeDefinitionRepository;
import com.example.platform.user.repository.DepartmentMemberAttributeRepository;
import com.example.platform.user.repository.DepartmentRepository;
import com.example.platform.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DepartmentAdminService {

    private static final Set<String> SUPPORTED_ATTRIBUTE_TYPES = Set.of("TEXT", "NUMBER", "BOOLEAN", "DATE", "DATETIME");

    private final DepartmentRepository departmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final DepartmentAttributeDefinitionRepository departmentAttributeDefinitionRepository;
    private final DepartmentMemberAttributeRepository departmentMemberAttributeRepository;
    private final UserViewAssembler userViewAssembler;
    private final UserAccessSupport userAccessSupport;

    public DepartmentAdminService(DepartmentRepository departmentRepository,
                                  UserAccountRepository userAccountRepository,
                                  DepartmentAttributeDefinitionRepository departmentAttributeDefinitionRepository,
                                  DepartmentMemberAttributeRepository departmentMemberAttributeRepository,
                                  UserViewAssembler userViewAssembler,
                                  UserAccessSupport userAccessSupport) {
        this.departmentRepository = departmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.departmentAttributeDefinitionRepository = departmentAttributeDefinitionRepository;
        this.departmentMemberAttributeRepository = departmentMemberAttributeRepository;
        this.userViewAssembler = userViewAssembler;
        this.userAccessSupport = userAccessSupport;
    }

    public PagedResult<AdminDepartmentSummaryResponse> listDepartments(Long operatorUserId,
                                                                       String sessionKey,
                                                                       String deptName,
                                                                       Long parentDepartmentId,
                                                                       Integer pageNum,
                                                                       Integer pageSize) {
        UserAccount operator = userAccessSupport.requireAdmin(operatorUserId, sessionKey, "department:read");
        List<AdminDepartmentSummaryResponse> items = scopedDepartments(operator).stream()
                .filter(department -> matchesDepartment(department, deptName))
                .filter(department -> parentDepartmentId == null || Objects.equals(department.parentDepartmentId(), parentDepartmentId))
                .map(userViewAssembler::toDepartmentSummary)
                .toList();
        return page(items, pageNum, pageSize);
    }

    public List<OrganizationTreeNodeResponse> getOrganizationTree(Long operatorUserId, String sessionKey) {
        UserAccount operator = userAccessSupport.requireAdmin(operatorUserId, sessionKey, "department:tree:read");
        if (userAccessSupport.isDepartmentScopedOperator(operator)) {
            Department root = requireDepartment(operator.departmentId());
            return List.of(buildTree(root));
        }
        return departmentRepository.findAll().stream()
                .filter(department -> department.parentDepartmentId().equals(0L))
                .map(this::buildTree)
                .toList();
    }

    public AdminDepartmentSummaryResponse createDepartment(Long operatorUserId,
                                                           String sessionKey,
                                                           AdminDepartmentCreateRequest request) {
        UserAccount operator = userAccessSupport.requireScopedDepartmentAccess(
                operatorUserId,
                sessionKey,
                "department:write",
                request.parentDepartmentId()
        );
        if (departmentRepository.existsByCode(request.departmentCode())) {
            throw new BusinessException("DEPARTMENT_CODE_EXISTS", "department code already exists");
        }
        validateParentDepartment(request.parentDepartmentId());
        Department created = departmentRepository.create(
                request.departmentCode(),
                request.departmentName(),
                request.parentDepartmentId(),
                request.description(),
                request.attributes()
        );
        ensureDefinitionsForAttributes(created.departmentId(), request.attributes());
        return userViewAssembler.toDepartmentSummary(created);
    }

    public AdminDepartmentSummaryResponse updateDepartment(Long operatorUserId,
                                                           String sessionKey,
                                                           Long departmentId,
                                                           AdminDepartmentUpdateRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:write", departmentId);
        Department existing = requireDepartment(departmentId);
        if (departmentId.equals(request.parentDepartmentId())) {
            throw new BusinessException("INVALID_PARENT_DEPARTMENT", "department cannot be its own parent");
        }
        validateParentDepartment(request.parentDepartmentId());
        if (request.parentDepartmentId() != null
                && request.parentDepartmentId() != 0L
                && departmentRepository.isDescendantOrSelf(departmentId, request.parentDepartmentId())) {
            throw new BusinessException("INVALID_PARENT_DEPARTMENT", "department cannot move under its own subtree");
        }
        Map<String, String> attributes = request.attributes() == null ? existing.attributes() : request.attributes();
        ensureDefinitionsForAttributes(departmentId, attributes);
        Department updated = departmentRepository.update(
                departmentId,
                request.departmentName(),
                request.parentDepartmentId(),
                request.description(),
                attributes
        ).orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toDepartmentSummary(updated);
    }

    public void deleteDepartment(Long operatorUserId, String sessionKey, Long departmentId) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:write", departmentId);
        requireDepartment(departmentId);
        long boundUsers = userAccountRepository.countByDepartmentId(departmentId);
        if (boundUsers > 0) {
            throw new BusinessException("DEPARTMENT_IN_USE", "department is bound by " + boundUsers + " users");
        }
        if (departmentRepository.hasChildren(departmentId)) {
            throw new BusinessException("DEPARTMENT_HAS_CHILDREN", "department has child departments");
        }
        departmentAttributeDefinitionRepository.deleteByDepartmentId(departmentId);
        departmentMemberAttributeRepository.deleteByDepartmentId(departmentId);
        departmentRepository.delete(departmentId);
    }

    public PagedResult<DepartmentUserSummaryResponse> listDepartmentUsers(Long operatorUserId,
                                                                          String sessionKey,
                                                                          Long departmentId,
                                                                          String account,
                                                                          String userName,
                                                                          Integer pageNum,
                                                                          Integer pageSize) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:read", departmentId);
        requireDepartment(departmentId);
        List<DepartmentUserSummaryResponse> users = userAccountRepository.findByDepartmentId(departmentId).stream()
                .filter(user -> matches(user.account(), account))
                .filter(user -> matches(user.userName(), userName))
                .map(userViewAssembler::toDepartmentUserSummary)
                .toList();
        return page(users, pageNum, pageSize);
    }

    public List<DepartmentAttributeDefinitionResponse> listAttributeDefinitions(Long operatorUserId,
                                                                                String sessionKey,
                                                                                Long departmentId) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:attribute:read", departmentId);
        requireDepartment(departmentId);
        return departmentAttributeDefinitionRepository.findByDepartmentId(departmentId).stream()
                .map(userViewAssembler::toDepartmentAttributeDefinition)
                .toList();
    }

    public DepartmentAttributeDefinitionResponse createAttributeDefinition(Long operatorUserId,
                                                                          String sessionKey,
                                                                          Long departmentId,
                                                                          DepartmentAttributeDefinitionCreateRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:attribute:write", departmentId);
        Department department = requireDepartment(departmentId);
        String normalizedType = normalizeAttributeType(request.dataType());
        if (departmentAttributeDefinitionRepository.exists(departmentId, request.attributeKey())) {
            throw new BusinessException("ATTRIBUTE_KEY_EXISTS", "attribute key already exists");
        }
        validateAttributeValue(request.attributeKey(), normalizedType, request.defaultValue());
        DepartmentAttributeDefinition definition = departmentAttributeDefinitionRepository.create(
                departmentId,
                request.attributeKey(),
                request.attributeName(),
                normalizedType,
                request.defaultValue(),
                Boolean.TRUE.equals(request.required()),
                request.rules(),
                request.displayOrder()
        );
        if (request.defaultValue() != null && !request.defaultValue().isBlank() && !department.attributes().containsKey(request.attributeKey())) {
            Map<String, String> updatedAttributes = new LinkedHashMap<>(department.attributes());
            updatedAttributes.put(request.attributeKey(), request.defaultValue().trim());
            departmentRepository.updateAttributes(departmentId, Map.copyOf(updatedAttributes));
        }
        return userViewAssembler.toDepartmentAttributeDefinition(definition);
    }

    public void deleteAttributeDefinition(Long operatorUserId,
                                          String sessionKey,
                                          Long departmentId,
                                          String attributeKey) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:attribute:write", departmentId);
        Department department = requireDepartment(departmentId);
        departmentAttributeDefinitionRepository.findByDepartmentIdAndKey(departmentId, attributeKey)
                .orElseThrow(() -> new BusinessException("ATTRIBUTE_NOT_FOUND", "attribute definition not found", HttpStatus.NOT_FOUND));
        if (department.attributes().containsKey(attributeKey)) {
            Map<String, String> updatedAttributes = new LinkedHashMap<>(department.attributes());
            updatedAttributes.remove(attributeKey);
            departmentRepository.updateAttributes(departmentId, Map.copyOf(updatedAttributes));
        }
        departmentMemberAttributeRepository.deleteByDepartmentIdAndAttributeKey(departmentId, attributeKey);
        departmentAttributeDefinitionRepository.delete(departmentId, attributeKey);
    }

    public AdminDepartmentSummaryResponse updateDepartmentAttributes(Long operatorUserId,
                                                                    String sessionKey,
                                                                    Long departmentId,
                                                                    DepartmentAttributesUpdateRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:attribute:write", departmentId);
        Department department = requireDepartment(departmentId);
        validateDefinedAttributes(departmentId, request.attributes().keySet());
        validateAttributeValues(departmentId, request.attributes());
        Map<String, String> merged = new LinkedHashMap<>(department.attributes());
        request.attributes().forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                merged.remove(key);
            } else {
                merged.put(key, value.trim());
            }
        });
        Department updated = departmentRepository.updateAttributes(departmentId, Map.copyOf(merged))
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toDepartmentSummary(updated);
    }

    public PagedResult<DepartmentUserSummaryResponse> addMembers(Long operatorUserId,
                                                                 String sessionKey,
                                                                 Long departmentId,
                                                                 DepartmentMemberRelationRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", departmentId);
        requireDepartment(departmentId);
        for (Long userId : request.userIds()) {
            UserAccount user = userAccessSupport.requireUser(userId);
            if (Objects.equals(user.departmentId(), departmentId)) {
                throw new BusinessException("USER_ALREADY_IN_DEPARTMENT", "user " + userId + " is already in department");
            }
            userAccessSupport.ensureUserAccessible(userAccessSupport.requireAdmin(operatorUserId, sessionKey, "department:member:write"), user);
            userAccountRepository.updateDepartmentMembership(userId, departmentId)
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        }
        return listDepartmentUsers(operatorUserId, sessionKey, departmentId, null, null, 1, request.userIds().size());
    }

    public DepartmentMembershipResponse removeMember(Long operatorUserId,
                                                     String sessionKey,
                                                     Long departmentId,
                                                     Long userId) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", departmentId);
        requireDepartment(departmentId);
        UserAccount user = userAccessSupport.requireUser(userId);
        userAccessSupport.ensureUserAccessible(userAccessSupport.requireAdmin(operatorUserId, sessionKey, "department:member:write"), user);
        if (!Objects.equals(user.departmentId(), departmentId)) {
            throw new BusinessException("USER_NOT_IN_DEPARTMENT", "user is not in target department");
        }
        userAccountRepository.updateDepartmentMembership(userId, null)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        departmentMemberAttributeRepository.deleteByDepartmentIdAndUserId(departmentId, userId);
        return new DepartmentMembershipResponse(userId, null, null, null, null, Map.of());
    }

    public List<DepartmentMembershipResponse> transferMembers(Long operatorUserId,
                                                              String sessionKey,
                                                              DepartmentTransferRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", request.fromDepartmentId());
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", request.toDepartmentId());
        if (Objects.equals(request.fromDepartmentId(), request.toDepartmentId())) {
            throw new BusinessException("INVALID_TRANSFER_TARGET", "source and target departments must differ");
        }
        requireDepartment(request.fromDepartmentId());
        requireDepartment(request.toDepartmentId());
        List<DepartmentMembershipResponse> results = new ArrayList<>();
        for (Long userId : request.userIds()) {
            UserAccount user = userAccessSupport.requireUser(userId);
            if (!Objects.equals(user.departmentId(), request.fromDepartmentId())) {
                throw new BusinessException("USER_NOT_IN_DEPARTMENT", "user " + userId + " is not in source department");
            }
            userAccountRepository.updateDepartmentMembership(userId, request.toDepartmentId())
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
            departmentMemberAttributeRepository.deleteByDepartmentIdAndUserId(request.fromDepartmentId(), userId);
            results.add(getUserMembership(operatorUserId, sessionKey, userId));
        }
        return results;
    }

    public DepartmentMembershipResponse getUserMembership(Long operatorUserId,
                                                          String sessionKey,
                                                          Long userId) {
        UserAccount operator = userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "department:member:read", userId);
        UserAccount user = userAccessSupport.requireUser(userId);
        if (user.departmentId() == null) {
            return new DepartmentMembershipResponse(user.userId(), null, null, null, null, Map.of());
        }
        userAccessSupport.ensureDepartmentAccessible(operator, user.departmentId());
        Department department = requireDepartment(user.departmentId());
        return new DepartmentMembershipResponse(
                user.userId(),
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                buildDepartmentPath(department.departmentId()),
                departmentMemberAttributeRepository.findByDepartmentIdAndUserId(department.departmentId(), user.userId())
        );
    }

    public DepartmentMembershipResponse updateMemberAttributes(Long operatorUserId,
                                                               String sessionKey,
                                                               Long departmentId,
                                                               Long userId,
                                                               DepartmentMemberAttributesUpdateRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", departmentId);
        requireDepartment(departmentId);
        UserAccount user = userAccessSupport.requireUser(userId);
        if (!Objects.equals(user.departmentId(), departmentId)) {
            throw new BusinessException("USER_NOT_IN_DEPARTMENT", "user is not in target department");
        }
        validateDefinedAttributes(departmentId, request.attributes().keySet());
        validateAttributeValues(departmentId, request.attributes());
        Map<String, String> attributes = departmentMemberAttributeRepository.mergeAttributes(departmentId, userId, request.attributes());
        Department department = requireDepartment(departmentId);
        return new DepartmentMembershipResponse(
                userId,
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                buildDepartmentPath(department.departmentId()),
                attributes
        );
    }

    public DepartmentMemberAttributesBatchResultResponse batchUpdateMemberAttributes(Long operatorUserId,
                                                                                     String sessionKey,
                                                                                     Long departmentId,
                                                                                     DepartmentMemberAttributesBatchRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "department:member:write", departmentId);
        requireDepartment(departmentId);
        List<DepartmentMemberAttributesBatchResultItemResponse> results = new ArrayList<>();
        boolean allSucceeded = true;
        for (DepartmentMemberAttributesBatchItemRequest item : request.operations()) {
            try {
                DepartmentMembershipResponse response = updateMemberAttributes(
                        operatorUserId,
                        sessionKey,
                        departmentId,
                        item.userId(),
                        new DepartmentMemberAttributesUpdateRequest(item.attributes())
                );
                results.add(new DepartmentMemberAttributesBatchResultItemResponse(
                        item.userId(),
                        true,
                        "updated",
                        response.memberAttributes()
                ));
            } catch (BusinessException ex) {
                allSucceeded = false;
                results.add(new DepartmentMemberAttributesBatchResultItemResponse(
                        item.userId(),
                        false,
                        ex.getMessage(),
                        Map.of()
                ));
            }
        }
        return new DepartmentMemberAttributesBatchResultResponse(allSucceeded, results);
    }

    private List<Department> scopedDepartments(UserAccount operator) {
        if (!userAccessSupport.isDepartmentScopedOperator(operator) || operator.departmentId() == null) {
            return departmentRepository.findAll();
        }
        List<Long> visibleDepartmentIds = departmentRepository.subtreeIds(operator.departmentId());
        return departmentRepository.findAll().stream()
                .filter(department -> visibleDepartmentIds.contains(department.departmentId()))
                .toList();
    }

    private OrganizationTreeNodeResponse buildTree(Department department) {
        List<DepartmentAttributeDefinitionResponse> definitions = departmentAttributeDefinitionRepository.findByDepartmentId(department.departmentId()).stream()
                .map(userViewAssembler::toDepartmentAttributeDefinition)
                .toList();
        List<OrganizationTreeNodeResponse> children = departmentRepository.findAll().stream()
                .filter(child -> Objects.equals(child.parentDepartmentId(), department.departmentId()))
                .map(this::buildTree)
                .toList();
        return userViewAssembler.toOrganizationTreeNode(
                department,
                (int) userAccountRepository.countByDepartmentId(department.departmentId()),
                definitions,
                children
        );
    }

    private Department requireDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found", HttpStatus.NOT_FOUND));
    }

    private void validateParentDepartment(Long parentDepartmentId) {
        if (parentDepartmentId == null || parentDepartmentId == 0L) {
            return;
        }
        departmentRepository.findById(parentDepartmentId)
                .orElseThrow(() -> new BusinessException("PARENT_DEPARTMENT_NOT_FOUND", "parent department not found"));
    }

    private boolean matchesDepartment(Department department, String deptName) {
        if (deptName == null || deptName.isBlank()) {
            return true;
        }
        String keyword = deptName.trim().toLowerCase(Locale.ROOT);
        return department.departmentName().toLowerCase(Locale.ROOT).contains(keyword)
                || department.departmentCode().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matches(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private void ensureDefinitionsForAttributes(Long departmentId, Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        attributes.forEach((key, value) -> {
            if (!departmentAttributeDefinitionRepository.exists(departmentId, key)) {
                departmentAttributeDefinitionRepository.createInferred(departmentId, key, value);
            }
        });
    }

    private void validateDefinedAttributes(Long departmentId, Set<String> attributeKeys) {
        if (!departmentAttributeDefinitionRepository.existsAll(departmentId, List.copyOf(attributeKeys))) {
            throw new BusinessException("ATTRIBUTE_NOT_DEFINED", "one or more attributes are not defined");
        }
    }

    private void validateAttributeValues(Long departmentId, Map<String, String> attributes) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            DepartmentAttributeDefinition definition = departmentAttributeDefinitionRepository
                    .findByDepartmentIdAndKey(departmentId, entry.getKey())
                    .orElseThrow(() -> new BusinessException("ATTRIBUTE_NOT_DEFINED", "attribute " + entry.getKey() + " is not defined"));
            if (definition.required() && (entry.getValue() == null || entry.getValue().isBlank())) {
                throw new BusinessException("ATTRIBUTE_REQUIRED", "attribute " + entry.getKey() + " is required");
            }
            validateAttributeValue(entry.getKey(), definition.dataType(), entry.getValue());
        }
    }

    private void validateAttributeValue(String attributeKey, String dataType, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            switch (normalizeAttributeType(dataType)) {
                case "TEXT" -> {
                }
                case "NUMBER" -> new java.math.BigDecimal(value);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException("boolean");
                    }
                }
                case "DATE" -> java.time.LocalDate.parse(value);
                case "DATETIME" -> java.time.Instant.parse(value);
                default -> throw new IllegalArgumentException("unsupported type");
            }
        } catch (Exception ex) {
            throw new BusinessException("ATTRIBUTE_VALUE_INVALID", "attribute " + attributeKey + " is not valid for type " + dataType);
        }
    }

    private String normalizeAttributeType(String dataType) {
        String normalized = dataType == null ? "" : dataType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ATTRIBUTE_TYPES.contains(normalized)) {
            throw new BusinessException("ATTRIBUTE_TYPE_INVALID", "dataType must be one of " + SUPPORTED_ATTRIBUTE_TYPES);
        }
        return normalized;
    }

    private String buildDepartmentPath(Long departmentId) {
        List<String> names = new ArrayList<>();
        Long cursor = departmentId;
        while (cursor != null && cursor != 0L) {
            Department department = requireDepartment(cursor);
            names.add(0, department.departmentName());
            cursor = department.parentDepartmentId();
        }
        return names.stream().collect(Collectors.joining(" / "));
    }

    private <T> PagedResult<T> page(List<T> items, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());
        return new PagedResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
