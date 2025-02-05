package com.nicapz.gym.Service;

import com.nicapz.gym.Model.Interaction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class RAG {


    private final JdbcClient jdbcClient;
    private final EmbeddingModel embeddingModel;

    private final ChatClient chatCLient;

    @Autowired
    public RAG (JdbcClient jdbcClient, EmbeddingModel embeddingModel, ChatClient.Builder chatCLientBuilder) {
        this.jdbcClient = jdbcClient;
        this.embeddingModel = embeddingModel;
        this.chatCLient = chatCLientBuilder.build();
    }

    public List<Interaction> hybridSearch (String prompt, int kMaxSemantic, float maxCosineDistance, int kMaxKeyword) {
        float[] embedding = embedPrompt(prompt);
        String keywords = extractKeywords(prompt);
        List<Interaction> results = new ArrayList<>(semanticSearch(embedding, kMaxSemantic, maxCosineDistance));
        List<Interaction> keywordResults = keyWordSearch(keywords, kMaxKeyword);
        keywordResults.removeIf(results::contains);
        results.addAll(keywordResults);
        return results;
    }

    public List<Interaction> semanticSearch(float[] embedding, int kMax, float maxCosineDistance) {
        return jdbcClient.sql("SELECT id, conversation_id, user_request, ai_reply " +
                        "FROM interactions WHERE (vector <=> :queryEmbedding::vector < :maxCosineDistance)" +
                        " ORDER BY vector <=> :queryEmbedding::vector LIMIT :limit")
                .param("queryEmbedding", embedding)
                .param("limit", kMax)
                .param("maxCosineDistance", maxCosineDistance)
                .query(Interaction.class)
                .list();
    }

    public List<Interaction> keyWordSearch(String keywords, int kMax) {
        return jdbcClient.sql("SELECT id, conversation_id, user_request, ai_reply " +
                        "FROM interactions WHERE tsvector @@ to_tsquery('english', :keywords) LIMIT :limit")
                .param("keywords", keywords)
                .param("limit", kMax)
                .query(Interaction.class)
                .list();
    }

    public float[] embedPrompt(String prompt) {
        return this.embeddingModel.embed(prompt);
    }

    public String extractKeywords(String input) {

        String keywords = this.chatCLient.prompt()
                .user("Extract the most important keywords from the following text. If you cannot discern any Keywords, simply return null." +
                        "Put a | between all extracted keywords, even if they belong together, as in \"Magnolia | flowers\" instead of \"Magnolia Flowers\". Text: " + input)
                .call()
                .content();
        System.out.println(keywords);

        return keywords;
    }
 }