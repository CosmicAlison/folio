package com.alisonpariela.blogmanager.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;
import com.alisonpariela.blogmanager.model.Post;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserUsername(String username);
    List<Post> findByUserId(Long userId);

    long countByUserId(Long userId);

    //paginated
    @Query("SELECT p FROM Post p WHERE p.user.id = :userId")
    Page<Post> findUserPostsOrderedByDate(@Param("userId") Long userId, Pageable pageable);

}
