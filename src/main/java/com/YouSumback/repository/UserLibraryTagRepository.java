package com.YouSumback.repository;

import com.YouSumback.model.entity.Tag;
import com.YouSumback.model.entity.UserLibrary;
import com.YouSumback.model.entity.UserLibraryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLibraryTagRepository extends JpaRepository<UserLibraryTag, Long> {
    Optional<UserLibraryTag> findByUserLibraryAndTag(UserLibrary userLibrary, Tag tag);
}

