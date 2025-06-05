package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_library_tag")
@Getter
@Setter
public class UserLibraryTag {

    @EmbeddedId
    private UserLibraryTagId id;

    @ManyToOne
    @MapsId("userLibraryId")
    @JoinColumn(name = "user_library_id", nullable = false)
    private UserLibrary userLibrary;

    @ManyToOne
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}