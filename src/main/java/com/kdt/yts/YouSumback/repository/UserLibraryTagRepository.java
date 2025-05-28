package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLibraryTagRepository {
    List findByUserLibrary(UserLibrary userLibrary);
}
