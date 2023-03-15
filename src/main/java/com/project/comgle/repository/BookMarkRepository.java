package com.project.comgle.repository;

import com.project.comgle.entity.BookMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookMarkRepository extends JpaRepository<BookMark, Long> {
    Optional<BookMark> findByBookMarkFolderIdAndPostId(Long folderId, Long postId);
}
