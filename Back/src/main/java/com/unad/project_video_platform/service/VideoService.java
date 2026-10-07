package com.unad.project_video_platform.service;

import com.unad.project_video_platform.dto.LocalUploadResult;
import com.unad.project_video_platform.entity.ContentMaterial;
import com.unad.project_video_platform.entity.Conversation;
import com.unad.project_video_platform.entity.Video;
import com.unad.project_video_platform.repository.CategoryRepository;
import com.unad.project_video_platform.repository.ContentMaterialRepository;
import com.unad.project_video_platform.repository.ConversationRepository;
import com.unad.project_video_platform.repository.QuestionRepository;
import com.unad.project_video_platform.repository.UserRepository;
import com.unad.project_video_platform.repository.VideoRepository;
import com.unad.project_video_platform.repository.VideoStatsRepository;
import com.unad.project_video_platform.service.impl.IVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class VideoService implements IVideoService {

    private static final Set<String> ALLOWED_MATERIAL_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png", "webp");

    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"));

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private com.unad.project_video_platform.security.CurrentUser currentUser;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentMaterialRepository contentMaterialRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private VideoStatsRepository videoStatsRepository;

    @Autowired
    private LocalContentMaterialStorageService localContentMaterialStorageService;

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public Optional<Video> getVideoById(Integer id) {
        return videoRepository.findById(id);
    }

    @Transactional
    public Video createVideo(Video video) {
        validateVideo(video, false, null);
        hydrateReferences(video);
        return videoRepository.save(video);
    }

    @Transactional
    public Video createVideo(Video video, MultipartFile[] materials) {
        validateVideo(video, hasUploadableMaterials(materials), null);
        hydrateReferences(video);
        Video saved = videoRepository.save(video);
        saveMaterials(saved, materials);
        return videoRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional
    public Video addMaterials(Integer id, MultipartFile[] materials) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + id));

        saveMaterials(video, materials);
        return videoRepository.findById(id).orElse(video);
    }

    @Transactional
    public Video deleteMaterial(Integer id, Integer materialId) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + id));

        ContentMaterial material = contentMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + materialId));

        if (material.getContent() == null || !id.equals(material.getContent().getId())) {
            throw new IllegalArgumentException("El material no pertenece al contenido indicado");
        }

        localContentMaterialStorageService.delete(material.getDriveFileId());
        contentMaterialRepository.delete(material);
        ensureMaterialsList(video).removeIf(current -> materialId.equals(current.getId()));

        return video;
    }

    @Transactional
    public Video updateVideo(Integer id, Video videoDetails) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + id));

        validateVideo(videoDetails, hasSavedMaterials(video), id);
        hydrateReferences(videoDetails);

        video.setUrlVideo(videoDetails.getUrlVideo());
        video.setTitle(videoDetails.getTitle());
        video.setDescription(videoDetails.getDescription());
        video.setCategory(videoDetails.getCategory());

        return videoRepository.save(video);
    }

    @Transactional
    public void deleteVideo(Integer id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado con id: " + id));

        List<ContentMaterial> materials = contentMaterialRepository.findByContentIdOrderByPositionAscIdAsc(id);
        materials.forEach(material -> localContentMaterialStorageService.delete(material.getDriveFileId()));

        List<Integer> conversationIds = conversationRepository.findByContentIdOrderByCreatedAtDesc(id)
                .stream()
                .map(Conversation::getId)
                .toList();

        videoStatsRepository.deleteByContentId(id);
        if (!conversationIds.isEmpty()) {
            questionRepository.deleteByConversationIdIn(conversationIds);
        }
        conversationRepository.deleteByContentId(id);
        contentMaterialRepository.deleteAll(materials);
        ensureMaterialsList(video).clear();
        videoRepository.delete(video);
    }

    private void validateVideo(Video video, boolean hasMaterials, Integer excludeId) {
        if (video == null) {
            throw new IllegalArgumentException("El contenido es obligatorio");
        }
        boolean hasVideoUrl = video.getUrlVideo() != null && !video.getUrlVideo().isBlank();
        if (!hasVideoUrl && !hasMaterials) {
            throw new IllegalArgumentException("Debes agregar una URL de video o al menos un material de apoyo");
        }
        if (video.getTitle() == null || video.getTitle().isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        if (video.getCategory() == null || video.getCategory().getId() == null) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }

        String trimmedTitle = video.getTitle().trim();
        boolean isDuplicate = excludeId == null
                ? videoRepository.existsByTitleIgnoreCase(trimmedTitle)
                : videoRepository.existsByTitleIgnoreCaseAndIdNot(trimmedTitle, excludeId);
        if (isDuplicate) {
            throw new IllegalArgumentException("Ya existe un contenido con el titulo: " + trimmedTitle);
        }

        video.setUrlVideo(hasVideoUrl ? video.getUrlVideo().trim() : null);
        video.setTitle(trimmedTitle);
        if (video.getDescription() != null) {
            video.setDescription(video.getDescription().trim());
        }
    }

    private boolean hasUploadableMaterials(MultipartFile[] materials) {
        if (materials == null || materials.length == 0) {
            return false;
        }

        for (MultipartFile file : materials) {
            if (file != null && !file.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSavedMaterials(Video video) {
        return video.getMaterials() != null && !video.getMaterials().isEmpty();
    }

    private void saveMaterials(Video video, MultipartFile[] materials) {
        if (materials == null || materials.length == 0) {
            return;
        }

        int position = contentMaterialRepository.findByContentIdOrderByPositionAscIdAsc(video.getId())
                .stream()
                .map(ContentMaterial::getPosition)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        for (MultipartFile file : materials) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String extension = resolveMaterialExtension(file);
            String mimeType = normalizeContentType(file, extension);
            LocalUploadResult uploadedFile = localContentMaterialStorageService.saveMaterial(file, extension, mimeType);

            ContentMaterial material = new ContentMaterial();
            material.setContent(video);
            material.setDriveFileId(uploadedFile.storedFileName());
            material.setDriveUrl(uploadedFile.url());
            material.setFileName(resolveFileName(file));
            material.setMimeType(uploadedFile.mimeType());
            material.setSizeBytes(file.getSize());
            material.setPosition(position++);

            ContentMaterial savedMaterial = contentMaterialRepository.save(material);
            ensureMaterialsList(video).add(savedMaterial);
        }
    }

    private List<ContentMaterial> ensureMaterialsList(Video video) {
        if (video.getMaterials() == null) {
            video.setMaterials(new ArrayList<>());
        }
        return video.getMaterials();
    }

    private String resolveFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        return originalName == null || originalName.isBlank() ? "material" : originalName;
    }

    private String resolveMaterialExtension(MultipartFile file) {
        String extension = extensionFromFileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());

        if (!ALLOWED_MATERIAL_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato de material no permitido. Usa PDF, Word, Excel, JPG, PNG o WEBP");
        }

        if (!contentType.isBlank()
                && !"application/octet-stream".equals(contentType)
                && !isCompatibleExtension(extension, contentType)) {
            throw new IllegalArgumentException("El tipo de archivo no coincide con su extension");
        }

        return extension;
    }

    private String extensionFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(MultipartFile file, String extension) {
        String contentType = normalizeContentType(file.getContentType());
        return contentType.isBlank() || "application/octet-stream".equals(contentType)
                ? MIME_BY_EXTENSION.getOrDefault(extension, "application/octet-stream")
                : contentType;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean isCompatibleExtension(String extension, String contentType) {
        String expected = MIME_BY_EXTENSION.get(extension);
        if (expected == null) {
            return false;
        }
        if (expected.equals(contentType)) {
            return true;
        }
        return Set.of("jpg", "jpeg").contains(extension) && "image/jpeg".equals(contentType);
    }

    private void hydrateReferences(Video video) {
        video.setCategory(
                categoryRepository.findById(video.getCategory().getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Categoria no encontrada con id: " + video.getCategory().getId())));

        video.setCreatedBy(currentUser.require());
    }
}
