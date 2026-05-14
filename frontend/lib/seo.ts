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
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      {
        "@type": "ListItem",
        position: 1,
        name: "홈",
        item: siteUrl,
      },
      {
        "@type": "ListItem",
        position: 2,
        name: post.categoryName,
        item: `${siteUrl}/category/${encodeURIComponent(post.categoryName)}`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: post.title,
        item: `${siteUrl}/posts/${post.slug}`,
      },
    ],
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
