package com.project.comgle.service;

import com.project.comgle.dto.request.PostRequestDto;
import com.project.comgle.dto.response.MessageResponseDto;
import com.project.comgle.dto.response.PostResponseDto;
import com.project.comgle.entity.*;
import com.project.comgle.entity.enumSet.PositionEnum;
import com.project.comgle.repository.CategoryRepository;
import com.project.comgle.repository.KeywordRepository;
import com.project.comgle.repository.PostCategoryRepository;
import com.project.comgle.repository.PostRepository;
import com.project.comgle.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final KeywordRepository keywordRepository;

    @Transactional
    public ResponseEntity<MessageResponseDto> createPost(PostRequestDto postRequestDto, UserDetailsImpl userDetails) {

        Post newPost = Post.from(postRequestDto, userDetails.getMember());
        for (String k: postRequestDto.getKeywords()) {
            Keyword keyword = Keyword.of(k);
            keyword.addPost(newPost);
        }
        postRepository.save(newPost);

        Optional<Category> findCategory = categoryRepository.findByCategoryNameAndCompany(postRequestDto.getCategory(), userDetails.getMember().getCompany());
        if (findCategory.isEmpty()) {
            throw new IllegalArgumentException("해당 카테고리가 존재하지 않습니다.");
        }

        PostCategory newPostCategory = PostCategory.of(findCategory.get(), newPost);
        postCategoryRepository.save(newPostCategory);

        return ResponseEntity.ok().body(MessageResponseDto.of(HttpStatus.OK.value(), "작성 완료"));
    }


    @Transactional
    public ResponseEntity<MessageResponseDto> deletePost(Long id, Member member) {

        Optional<Post> post = postRepository.findByIdAndMember(id, member);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("해당 게시물이 없습니다.");
        }

        List<Keyword> keywordList = keywordRepository.findAllByPost(post.get());
        for (Keyword k : keywordList) {
            keywordRepository.delete(k);
        }

        Optional<PostCategory> postCategory = postCategoryRepository.findByPostId(post.get().getId());
        if(postCategory.isPresent()){
            postCategoryRepository.delete(postCategory.get());
            categoryRepository.deleteById(postCategory.get().getCategory().getId());
        }
        postRepository.delete(post.get());

        return ResponseEntity.ok().body(MessageResponseDto.of(HttpStatus.OK.value(), "삭제 완료"));
    }

    @Transactional
    public ResponseEntity<MessageResponseDto> updatePost(Long id, PostRequestDto postRequestDto, Member member) {

        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("해당 게시글이 없습니다.");
        }

        // String을 Enum으로 형변환
        // Enum에 값을 부여해서 등급을 정수로 비교
        PositionEnum modifyEnum = PositionEnum.valueOf(post.get().getModifyPermission().trim().toUpperCase());

        Optional<Post> compare = postRepository.findByIdAndMember(id, member);
        if (compare.isEmpty()) {
            throw new IllegalArgumentException("해당 작성자의 게시물이 없습니다.");
        } else if (member.getPosition().getNum() < modifyEnum.getNum() ) {
            throw new IllegalArgumentException("수정 가능한 회원 등급이 아닙니다.");
        } else {
            post = postRepository.findById(id);
        }

        List<Keyword> keywordList = keywordRepository.findAllByPost(post.get());
        List<String> keyWords = new ArrayList<>(Arrays.asList(postRequestDto.getKeywords()));
        List<String> newKeywords = new ArrayList<>();

        for (Keyword k: keywordList) {
            if(!keyWords.contains(k.getKeyword())){
                keywordRepository.delete(k);
            }
            newKeywords.add(k.getKeyword());
        }

        for (String s : keyWords) {
            if(!newKeywords.contains(s)){
                Keyword keyword = Keyword.of(s);
                keyword.addPost(post.get());
                keywordRepository.save(keyword);
            }
        }

        Optional<PostCategory> postCategory = postCategoryRepository.findByPost(post.get());
        Optional<Category> category = categoryRepository.findById(postCategory.get().getId());
        String newCategory = postRequestDto.getCategory();

        if(!newCategory.contains(category.get().getCategoryName()))  {
            postCategoryRepository.delete(postCategory.get());
            categoryRepository.deleteById(postCategory.get().getCategory().getId());
        }

        if (!postCategory.get().getCategory().getCategoryName().contains(newCategory)) {
            Category renewCategory = new Category(postRequestDto.getCategory());
            categoryRepository.save(renewCategory);

            PostCategory renewPostCategory = PostCategory.of(renewCategory, post.get());
            postCategoryRepository.save(renewPostCategory);
        }

        post.get().update(postRequestDto);
        return ResponseEntity.ok().body(MessageResponseDto.of(HttpStatus.OK.value(), "수정 완료"));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PostResponseDto> readPost(Long id, Member member) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("해당 게시글이 없습니다.");
        }

        List<Keyword> keywords = keywordRepository.findAllByPost(post.get());
        String[] keywordList = new String[keywords.size()];
        for (int i=0; i < keywords.size(); i++) {
            keywordList[i] = keywords.get(i).getKeyword();
        }

        Optional<PostCategory> postCategory = postCategoryRepository.findByPost(post.get());
        Optional<Category> category = categoryRepository.findById(postCategory.get().getId());
        String getCategory = category.get().getCategoryName();

        // 댓글은 구현 후 추가 예정

        return ResponseEntity.ok().body(PostResponseDto.of(post.get(), getCategory, keywordList));
    }

}
