package com.fitnesslife.gym.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class ChatbotDocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(ChatbotDocumentLoader.class);

    private final VectorStore vectorStore;
    private final boolean ingestOnStartup;

    public ChatbotDocumentLoader(
            VectorStore vectorStore,
            @Value("${chatbot.ingest-on-startup:true}") boolean ingestOnStartup) {
        this.vectorStore = vectorStore;
        this.ingestOnStartup = ingestOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadDocuments() {
        if (!ingestOnStartup) {
            log.info("Chatbot RAG: ingesta al arrancar desactivada (chatbot.ingest-on-startup=false)");
            return;
        }

        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:chatbot-docs/*.txt");

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(300)
                    .build();

            List<Document> chunks = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                List<Document> fileChunks = splitter.apply(new TextReader(resource).get());

                for (int i = 0; i < fileChunks.size(); i++) {
                    Document chunk = fileChunks.get(i);
                    String id = UUID.nameUUIDFromBytes(
                            (filename + "#" + i).getBytes(StandardCharsets.UTF_8)).toString();

                    chunks.add(Document.builder()
                            .id(id)
                            .text(chunk.getText())
                            .metadata(chunk.getMetadata())
                            .build());
                }
                log.info("Chatbot RAG: {} -> {} chunks", filename, fileChunks.size());
            }

            if (chunks.isEmpty()) {
                log.warn("Chatbot RAG: no se encontraron documentos en classpath:chatbot-docs/*.txt");
                return;
            }

            vectorStore.add(chunks);
            log.info("Chatbot RAG: {} chunks almacenados en el vector store", chunks.size());
        } catch (IOException | RuntimeException e) {
            log.error("Chatbot RAG: no se pudieron cargar los documentos", e);
        }
    }
}
