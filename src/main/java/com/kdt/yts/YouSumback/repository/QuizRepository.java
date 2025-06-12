package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    List<Quiz> findByTitleContaining(String keyword);

}
