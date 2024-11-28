package com.nicapz.gym.Repositories;

import com.nicapz.gym.Model.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<UserRequest, Long> {
}
