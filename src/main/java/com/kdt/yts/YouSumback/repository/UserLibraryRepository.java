package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
}