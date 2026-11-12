package com.alisonpariela.blogmanager.service;
import com.alisonpariela.blogmanager.model.*;
import com.alisonpariela.blogmanager.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PostServiceTest {
    PostRepository posts = mock(PostRepository.class);
    UserRepository users = mock(UserRepository.class);
    PostService service = new PostService(posts, users);
    User owner;
    @BeforeEach void auth() {
        owner = new User(); owner.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner, null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void createsForAuthenticatedUser() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(posts.save(any())).thenAnswer(i -> i.getArgument(0));
        var post = service.createPost("Title", "Body");
        assertSame(owner, post.getUser()); assertEquals("Body", post.getContents());
    }
    @Test void cannotUpdateAnotherUsersPost() {
        var other = new User(); other.setId(2L);
        var post = new Post(); post.setUser(other);
        when(posts.findById(9L)).thenReturn(Optional.of(post));
        assertThrows(AccessDeniedException.class, () -> service.updatePost(9L, "x", "y"));
        verify(posts, never()).save(any());
    }
    @Test void cannotDeleteAnotherUsersPost() {
        var other = new User(); other.setId(2L);
        var post = new Post(); post.setUser(other);
        when(posts.findById(9L)).thenReturn(Optional.of(post));
        assertThrows(AccessDeniedException.class, () -> service.deletePost(9L));
        verify(posts, never()).delete(any());
    }
    @Test void missingPostFails() { assertThrows(EntityNotFoundException.class, () -> service.deletePost(9L)); }
    @Test void updatesOwnedPost() {
        var post = new Post(); post.setUser(owner);
        when(posts.findById(1L)).thenReturn(Optional.of(post)); when(posts.save(any())).thenReturn(post);
        assertEquals("Changed", service.updatePost(1L,"Changed","Body").getTitle());
    }
    @Test void deletesOwnedPost() {
        var post = new Post(); post.setUser(owner);
        when(posts.findById(1L)).thenReturn(Optional.of(post)); service.deletePost(1L);
        verify(posts).delete(post);
    }
}
