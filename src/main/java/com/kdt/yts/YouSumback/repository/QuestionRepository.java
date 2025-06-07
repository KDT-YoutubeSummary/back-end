package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Question;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findById(Long id);
    List<Question> findAll();
}
