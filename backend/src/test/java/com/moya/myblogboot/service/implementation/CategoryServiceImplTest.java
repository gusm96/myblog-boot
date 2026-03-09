package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryResDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceImplTest {

    @Autowired private CategoryService categoryService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BoardRepository boardRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member testMember;
    private Category testCategory;

    @BeforeEach
    void before() {
        testMember = memberRepository.save(Member.builder()
                .username("categoryTestUser")
                .password(passwordEncoder.encode("testPw"))
                .nickname("categoryTester")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("테스트카테고리")
                .build());
    }

    @Test
    @DisplayName("카테고리 전체 조회")
    void retrieveAll() {
        List<CategoryResDto> result = categoryService.retrieveAll();

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(c -> c.getName().equals("테스트카테고리"))).isTrue();
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void create() {
        String result = categoryService.create("새카테고리");

        assertThat(result).isEqualTo("카테고리가 정상적으로 등록되었습니다.");
        assertThat(categoryRepository.existsByName("새카테고리")).isTrue();
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 중복된 이름")
    void create_duplicate() {
        assertThrows(DuplicateKeyException.class,
                () -> categoryService.create("테스트카테고리"));
    }

    @Test
    @DisplayName("카테고리 수정 성공")
    void update() {
        String result = categoryService.update(testCategory.getId(), "수정된카테고리");

        assertThat(result).isEqualTo("수정된카테고리");
        assertThat(testCategory.getName()).isEqualTo("수정된카테고리");
    }

    @Test
    @DisplayName("카테고리 조회 실패 - 존재하지 않는 ID")
    void retrieve_notFound() {
        assertThrows(EntityNotFoundException.class,
                () -> categoryService.retrieve(999L));
    }

    @Test
    @DisplayName("카테고리 삭제 성공 - 게시글 없음")
    void delete() {
        Category emptyCategory = categoryRepository.save(Category.builder()
                .name("빈카테고리")
                .build());

        String result = categoryService.delete(emptyCategory.getId());

        assertThat(result).isEqualTo("카테고리가 삭제되었습니다.");
        assertThat(categoryRepository.findById(emptyCategory.getId())).isEmpty();
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 등록된 게시글 존재")
    void delete_withBoards() {
        Board board = boardRepository.save(Board.builder()
                .title("테스트게시글")
                .content("테스트내용")
                .category(testCategory)
                .member(testMember)
                .build());
        testCategory.addBoard(board);

        assertThrows(RuntimeException.class,
                () -> categoryService.delete(testCategory.getId()));
    }
}
