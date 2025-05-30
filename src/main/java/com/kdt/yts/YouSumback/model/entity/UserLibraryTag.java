package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "UserLibraryTag")
public class UserLibraryTag {

    @EmbeddedId
    private UserLibraryTagId id;

    @ManyToOne
    @MapsId("userLibraryId")
    @JoinColumn(name = "user_library_id")
    private UserLibrary userLibrary; // 라이브러리 식별자

    @ManyToOne
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag; // 태그 식별자

    // 생성자
    public UserLibraryTag(UserLibraryTagId id, UserLibrary userLibrary, Tag tag) {
        this.id = id;
        this.userLibrary = userLibrary;
        this.tag = tag;
    }

    // 기본 생성자
    public UserLibraryTag() {
    }

    // UserLibrary와 Tag를 이용한 생성자
    public UserLibraryTag(UserLibrary library, Tag tag1) {
        this.id = new UserLibraryTagId(library.getUserLibraryId(), tag1.getTagId());
        this.userLibrary = library;
        this.tag = tag1;
    }
}