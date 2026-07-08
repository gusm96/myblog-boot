import type { Post } from "@/types";

const AUTHOR_NAME =
  process.env.NEXT_PUBLIC_AUTHOR_NAME ?? "Dev-Moya";
const AUTHOR_URL = process.env.NEXT_PUBLIC_AUTHOR_URL ?? "";

function absoluteUrl(url: string, siteUrl: string): string {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return new URL(url, siteUrl).toString();
}

export function buildArticleSchema(
  post: Post,
  siteUrl: string
): Record<string, unknown> {
  const postUrl = `${siteUrl}/posts/${post.slug}`;
  const imageUrl = post.thumbnailUrl
    ? absoluteUrl(post.thumbnailUrl, siteUrl)
    : `${siteUrl}/og-default.png`;

  return {
    "@context": "https://schema.org",
    "@type": "BlogPosting",
    headline: post.title,
    description: post.metaDescription ?? "",
    url: postUrl,
    datePublished: post.createDate,
    dateModified: post.updateDate ?? post.createDate,
    image: { "@type": "ImageObject", url: imageUrl, width: 1200, height: 630 },
    author: {
      "@type": "Person",
      name: AUTHOR_NAME,
      ...(AUTHOR_URL ? { url: AUTHOR_URL } : {}),
    },
    publisher: {
      "@type": "Organization",
      name: "Dev-Moya",
      logo: { "@type": "ImageObject", url: `${siteUrl}/og-default.png` },
    },
    mainEntityOfPage: { "@type": "WebPage", "@id": postUrl },
  };
}

export function buildBreadcrumbSchema(
  post: Post,
  siteUrl: string
): Record<string, unknown> {
  const items = [
    {
      "@type": "ListItem",
      position: 1,
      name: "홈",
      item: siteUrl,
    },
  ];

  let nextPos = 2;

  if (post.tags && post.tags.length > 0) {
    const firstTag = post.tags[0];
    items.push({
      "@type": "ListItem",
      position: nextPos,
      name: firstTag.name,
      item: `${siteUrl}/tag/${encodeURIComponent(firstTag.slug)}`,
    });
    nextPos++;
  }

  items.push({
    "@type": "ListItem",
    position: nextPos,
    name: post.title,
    item: `${siteUrl}/posts/${post.slug}`,
  });

  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items,
  };
}

export function buildOrganizationSchema(
  siteUrl: string
): Record<string, unknown> {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: "Dev-Moya",
    url: siteUrl,
    logo: { "@type": "ImageObject", url: `${siteUrl}/og-default.png` },
  };
}
