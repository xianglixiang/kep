package com.kep.document;

import com.kep.document.api.Converter;
import com.kep.document.converter.DefaultTitleExtractor;
import com.kep.document.dto.EditLockView;
import com.kep.document.dto.KnowledgeVersionView;
import com.kep.document.dto.KnowledgeView;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.storage.ObjectStorage;
import com.kep.shared.tenant.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 仅生产剖面启用——local-mock 排除 JpaRepositoriesAutoConfiguration，无 KnowledgeRepository
 * 可注入。与 ReviewService / JpaCatalogNodeStore 同模式。
 */
@Service
@Profile("!local-mock")
public class DocumentService {

    private final KnowledgeRepository knowledgeRepo;
    private final KnowledgeVersionRepository versionRepo;
    private final ObjectStorage objectStorage;
    private final Converter converter;
    private final DefaultTitleExtractor titleExtractor;
    private final PermissionChecker permissionChecker;
    private final EditLockRepository editLockRepo;

    public DocumentService(KnowledgeRepository knowledgeRepo,
                           KnowledgeVersionRepository versionRepo,
                           @Lazy ObjectStorage objectStorage,
                           Converter converter,
                           DefaultTitleExtractor titleExtractor,
                           PermissionChecker permissionChecker,
                           EditLockRepository editLockRepo) {
        this.knowledgeRepo = knowledgeRepo;
        this.versionRepo = versionRepo;
        this.objectStorage = objectStorage;
        this.converter = converter;
        this.titleExtractor = titleExtractor;
        this.permissionChecker = permissionChecker;
        this.editLockRepo = editLockRepo;
    }

    @Transactional
    public KnowledgeView upload(long userId, long catalogNodeId,
                                 MultipartFile file, String title) throws Exception {
        permissionChecker.check(userId, catalogNodeId, Permission.WRITE);

        // 1. 内存中转（POI 需要流；避免 OSS 往返）
        byte[] docxBytes = file.getBytes();

        // 2. POI 转换
        String html = converter.convert(new ByteArrayInputStream(docxBytes));

        // 3. 解析 title
        String finalTitle = (title == null || title.isBlank())
            ? titleExtractor.from(html)
            : title;

        // 4. 上传 docx 到 OSS（key 用 pre-known uuid）
        String key = "tenant-" + TenantContext.get() + "/knowledge/" + UUID.randomUUID() + "/v1/original.docx";
        objectStorage.put(key, new ByteArrayInputStream(docxBytes), docxBytes.length,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        // 5. INSERT knowledge（先不设 current_version_id，FK 互指，DEFERRABLE 让事务末尾检查）
        Knowledge k = knowledgeRepo.save(Knowledge.newInstance(catalogNodeId, finalTitle, userId));

        // 6. INSERT knowledge_version v1
        KnowledgeVersion v = versionRepo.save(KnowledgeVersion.create(
            k.getId(), 1, html, key, "docx", userId));

        // 7. UPDATE knowledge.current_version_id
        k.setCurrentVersionId(v.getId());
        knowledgeRepo.save(k);

        return KnowledgeView.from(k, KnowledgeVersionView.from(v));
    }

    @Transactional
    public EditLockView acquireLock(long userId, long knowledgeId) {
        Knowledge k = knowledgeRepo.findById(knowledgeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识不存在"));
        permissionChecker.check(userId, k.getCatalogNodeId(), Permission.WRITE);

        return editLockRepo.findByKnowledgeId(knowledgeId)
            .map(existing -> {
                if (existing.isExpired()) {
                    EditLock refreshed = EditLock.refresh(existing, userId);
                    return EditLockView.from(editLockRepo.save(refreshed));
                }
                if (existing.isHeldBy(userId)) {
                    // 自己的锁, idempotent 刷新 TTL
                    EditLock refreshed = EditLock.refresh(existing, userId);
                    return EditLockView.from(editLockRepo.save(refreshed));
                }
                // 他人持锁
                throw new BusinessException(ErrorCode.CONFLICT,
                    "知识已被其他用户锁定，过期时间：" + existing.getExpiresAt());
            })
            .orElseGet(() -> {
                EditLock lock = EditLock.acquire(knowledgeId, userId);
                return EditLockView.from(editLockRepo.save(lock));
            });
    }

    @Transactional
    public void releaseLock(long userId, long knowledgeId) {
        EditLock lock = editLockRepo.findByKnowledgeId(knowledgeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "锁不存在"));
        if (!lock.isHeldBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅持锁者可释放");
        }
        editLockRepo.delete(lock);
    }

    @Transactional(readOnly = true)
    public KnowledgeView getCurrent(long userId, long knowledgeId) {
        Knowledge k = knowledgeRepo.findById(knowledgeId)
            .orElseThrow(() -> new com.kep.shared.error.BusinessException(
                com.kep.shared.error.ErrorCode.NOT_FOUND, "知识不存在"));
        permissionChecker.check(userId, k.getCatalogNodeId(), Permission.READ);

        KnowledgeVersion v = versionRepo.findById(k.getCurrentVersionId())
            .orElseThrow(() -> new IllegalStateException("current_version 缺失"));
        return KnowledgeView.from(k, KnowledgeVersionView.from(v));
    }

    @Transactional(readOnly = true)
    public KnowledgeVersionView getVersion(long userId, long knowledgeId, int versionNo) {
        Knowledge k = knowledgeRepo.findById(knowledgeId)
            .orElseThrow(() -> new com.kep.shared.error.BusinessException(
                com.kep.shared.error.ErrorCode.NOT_FOUND, "知识不存在"));
        permissionChecker.check(userId, k.getCatalogNodeId(), Permission.READ);

        KnowledgeVersion v = versionRepo.findByKnowledgeIdAndVersionNo(knowledgeId, versionNo)
            .orElseThrow(() -> new com.kep.shared.error.BusinessException(
                com.kep.shared.error.ErrorCode.NOT_FOUND, "版本不存在"));
        return KnowledgeVersionView.from(v);
    }

    @Transactional(readOnly = true)
    public byte[] getOriginalFile(long userId, long knowledgeId) {
        Knowledge k = knowledgeRepo.findById(knowledgeId)
            .orElseThrow(() -> new com.kep.shared.error.BusinessException(
                com.kep.shared.error.ErrorCode.NOT_FOUND, "知识不存在"));
        permissionChecker.check(userId, k.getCatalogNodeId(), Permission.READ);

        KnowledgeVersion v = versionRepo.findById(k.getCurrentVersionId())
            .orElseThrow(() -> new IllegalStateException("current_version 缺失"));
        if (v.getOriginalFileKey() == null) {
            throw new com.kep.shared.error.BusinessException(
                com.kep.shared.error.ErrorCode.NOT_FOUND, "原始文件未保存");
        }
        return objectStorage.get(v.getOriginalFileKey());
    }
}
