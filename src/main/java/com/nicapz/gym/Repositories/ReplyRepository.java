package com.nicapz.gym.Repositories;

import com.nicapz.gym.Model.ChatGPTReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplyRepository extends JpaRepository<ChatGPTReply, Long> {
}
