package com.moya.myblogboot.controller;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardReqDto;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;



@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class BoardControllerTest extends AbstractContainerBaseTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager em;

    @Autowired
    BoardRepository boardRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthService authService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    CategoryRepository categoryRepository;
    private static Long boardId;
    private static Long categoryId;

    private static String accessToken;
    @BeforeEach
    void before () {
        Member member = Member.builder()
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .nickname("testMember")
                .build();
        member.addRoleAdmin();
        Member saveMember = memberRepository.save(member);

        Category category = Category.builder().name("Test").build();
        Category saveCategory = categoryRepository.save(category);
        categoryId = saveCategory.getId();

        for (int i = 0; i < 5; i++) {
            Board newBoard = Board.builder()
                    .member(saveMember)
                    .category(saveCategory)
                    .title("title")
                    .content("content")
                    .build();
            Board result = boardRepository.save(newBoard);
            boardId = result.getId();
        }

        // Login DTO
        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        // Login 후 Token 발급
        accessToken = "bearer " + authService.memberLogin(loginReqDto).getAccess_token();
    }

    @Test
    @DisplayName("모든 게시글 조회")
    void getAllBoards() throws Exception {
        //given
        int page = 1;
        String path = "/api/v1/boards";
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", String.valueOf(page)));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("카테고리별 게시글 조회")
    void requestCategoryOfBoards() throws Exception {
        // given
        int page = 1;
        String category = "Test";
        String path = "/api/v1/boards/" + category;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", String.valueOf(page)));
        //then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("검색 결과별 게시글 조회")
    void searchBoards() throws Exception {
        // given
        SearchType searchType = SearchType.TITLE;
        String contents = "title";
        int page = 1;
        String path = "/api/v1/boards/search";

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", String.valueOf(page))
                .param("type", String.valueOf(searchType))
                .param("contents", contents)
        );

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 조회")
     void getBoardDetail() throws Exception {
        String path = "/api/v2/boards/" + boardId;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(boardId));
    }

    @Test
    @DisplayName("게시글 등록")
    void postBoard() throws Exception {
        // given
        String path = "/api/v1/boards";
        BoardReqDto boardReqDto = BoardReqDto.builder()
                .category(categoryId)
                .title("title")
                .content("content")
                .build();
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(path)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(boardReqDto)));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());

     }

    @Test
    @DisplayName("게시글 수정")
    void editBoard() throws Exception {
        // given
        String path = "/api/v1/boards/" + boardId;
        BoardReqDto modifiedBoardDto = BoardReqDto.builder()
                .title("modifiedTitle")
                .content("modifiedContent")
                .category(categoryId).build();
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.put(path)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(modifiedBoardDto)));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 삭제")
    void deleteBoard() throws Exception {
        // given
        String path = "/api/v1/boards/" + boardId;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 좋아요 요청")
    void requestToAddBoardLike() throws Exception {
        String path = "/api/v1/likes/" + boardId;
        mockMvc.perform(MockMvcRequestBuilders.post(path)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 좋아요 여부 체크")
    void requestToCheckBoardLike() throws Exception{
        // given
        String path = "/api/v1/likes/" + boardId;
        // 먼저 게시글 좋아요 요청
        mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // when
        // 좋아요 여부 확인
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 좋아요 취소")
    void requestToCancelBoardLike()throws Exception {
        // given
        String path = "/api/v1/likes/" + boardId;
        // 먼저 게시글 좋아요 요청
        mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // when
        // 좋아요 취소
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                        .header(HttpHeaders.AUTHORIZATION, accessToken));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}