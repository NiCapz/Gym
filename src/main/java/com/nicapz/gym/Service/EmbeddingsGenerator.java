package com.nicapz.gym.Service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingsGenerator implements CommandLineRunner {

    private final JdbcClient jdbcClient;

    private final EmbeddingModel embeddingModel;

    @Autowired
    public EmbeddingsGenerator(EmbeddingModel embeddingModel, JdbcClient jdbcClient) {
        this.embeddingModel = embeddingModel;
        this.jdbcClient = jdbcClient;
    }

    public void generateAll() {
        System.out.println("Generating embeddings...");

        SqlRowSet result = jdbcClient.sql("SELECT id,  interactions.user_request FROM interactions")
                .query().rowSet();
        int counter = 0;

        while(result.next()) {
            int id = result.getInt("id");
            String userRequest = result.getString("user_request");

            float[] embedding = null;
            if (userRequest != null) {
                embedding = this.embeddingModel.embed(userRequest);
            }


            jdbcClient.sql("UPDATE interactions SET vector = ?::vector WHERE id = ?")
                    .params(embedding, id)
                    .update();

            System.out.println(++counter);
        }
        System.out.println("Embeddings generated.");
    }

    @Override
    public void run(String... args) {
        //generateAll();
    }
}


