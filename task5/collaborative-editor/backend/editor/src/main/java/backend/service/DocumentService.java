package backend.service;

import backend.model.Document;
import backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    public Document getOrCreateDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseGet(() -> {
                    Document newDoc = new Document(documentId, "");
                    return documentRepository.save(newDoc);
                });
    }
    
    public void saveDocument(String documentId, String content) {
        Document document = getOrCreateDocument(documentId);
        document.setContent(content);
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
    }
    
    public String getDocumentContent(String documentId) {
        Document document = getOrCreateDocument(documentId);
        return document.getContent();
    }
    
    public void updateDocument(String documentId, String content) {
        saveDocument(documentId, content);
    }
}
