package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class RssFeedController {

    private static final int RSS_ITEM_COUNT = 50;
    private static final DateTimeFormatter RFC822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0900", Locale.ENGLISH);

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.blog-title}")
    private String blogTitle;

    @Value("${app.blog-description}")
    private String blogDescription;

    private final PostRepository postRepository;

    @GetMapping(value = "/rss.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> rss() {
        Page<Post> posts = postRepository.findRecentForRss(
                PageRequest.of(0, RSS_ITEM_COUNT)
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=600")
                .contentType(MediaType.APPLICATION_XML)
                .body(buildRss(posts.getContent()));
    }

    private String buildRss(List<Post> posts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        sb.append("  <channel>\n");
        sb.append("    <title>").append(escapeXml(blogTitle)).append("</title>\n");
        sb.append("    <link>").append(baseUrl).append("</link>\n");
        sb.append("    <description>").append(escapeXml(blogDescription)).append("</description>\n");
        sb.append("    <language>ko</language>\n");
        sb.append("    <atom:link href=\"").append(baseUrl).append("/rss.xml\" ")
                .append("rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (Post post : posts) {
            String link = baseUrl + "/posts/" + post.getSlug();
            String description = post.getMetaDescription() != null
                    ? post.getMetaDescription()
                    : truncate(stripHtml(post.getContent()), 200);

            sb.append("    <item>\n");
            sb.append("      <title>").append(escapeXml(post.getTitle())).append("</title>\n");
            sb.append("      <link>").append(escapeXml(link)).append("</link>\n");
            sb.append("      <description>").append(escapeXml(description)).append("</description>\n");
            sb.append("      <pubDate>").append(post.getCreateDate().format(RFC822)).append("</pubDate>\n");
            sb.append("      <guid isPermaLink=\"true\">").append(escapeXml(link)).append("</guid>\n");
            if (post.getCategory() != null) {
                sb.append("      <category>").append(escapeXml(post.getCategory().getName())).append("</category>\n");
            }
            sb.append("    </item>\n");
        }

        sb.append("  </channel>\n");
        sb.append("</rss>");
        return sb.toString();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
