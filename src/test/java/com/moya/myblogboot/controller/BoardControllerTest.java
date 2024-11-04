package com.moya.myblogboot.controller;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class BoardControllerTest extends AbstractContainerBaseTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private RestDocumentationResultHandler restDocs;
    private static Long boardId;
    private static Long categoryId;
    private static String accessToken;

    // REST docs setUp
    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentationContextProvider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentationContextProvider))
                .apply(springSecurity())
                .alwaysDo(restDocs)
                .build();
    }

    @BeforeEach
    void before() {
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
    void getCategoryBoards() throws Exception {
        // given
        int page = 1;
        String category = "Test";
        String path = "/api/v1/boards/category";

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("c", category)
                .param("p", String.valueOf(page)));
        //then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("검색 결과별 게시글 조회")
    void getSearchedBoards() throws Exception {
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
    @DisplayName("게시글 조회 V4")
    void getBoardDetail() throws Exception {
        // given
        String path = "/api/v4/boards/" + boardId;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(boardId));
    }

    @Test
    @DisplayName("게시글 상세 관리자용")
    void getBoardDetailForAdmin() throws Exception {
        // given
        String path = "/api/v1/management/boards/" + boardId;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 등록")
    void writeBoard() throws Exception {
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
    @DisplayName("삭제 예정 게시글 리스트")
    void getDeletedBoards() throws  Exception {
        // given
        // 게시글 삭제
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/boards/" + boardId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        String path = "/api/v1/deleted-boards";
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 삭제 취소")
    void cancelDeletedBoard()throws Exception {
        // given
        // 먼저 삭제 요청
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/boards" + boardId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        String path = "/api/v1/deleted-boards/" + boardId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.put(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 영구 삭제")
    void deleteBoardPermanently() throws Exception {
        // given
        String path = "/api/v1/deleted-boards/" + boardId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("게시글 좋아요 V2")
    void addBoardLike() throws Exception {
        // given
        String path = "/api/v2/likes/" + boardId;

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }


    @Test
    @DisplayName("게시글 좋아요 여부 체크")
    void checkBoardLike() throws Exception {
        // given
        String path = "/api/v2/likes/" + boardId;

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
    void cancelBoardLike() throws Exception {
        // given
        String path = "/api/v2/likes/" + boardId;

        // 먼저 게시글 좋아요 요청
        mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        // when
        // 좋아요 취소
        try {
            ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                    .header(HttpHeaders.AUTHORIZATION, accessToken));

            // then
            resultActions.andExpect(MockMvcResultMatchers.status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}


