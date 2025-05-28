package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLibraryTagRepository extends JpaRepository<UserLibraryTag, Long> {
    List findByUserLibrary(UserLibrary userLibrary);

}
