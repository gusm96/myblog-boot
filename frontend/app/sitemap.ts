import { MetadataRoute } from "next";
import { getAllSlugs } from "@/lib/api";

// sitemap 자체를 1시간 단위로 재생성. 신규 글 즉시 반영은 on-demand (revalidatePath("/sitemap.xml")) 가 담당.
export const revalidate = 3600;

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://myblog.com";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  let postEntries: MetadataRoute.Sitemap = [];

  try {
    const posts = await getAllSlugs();
    postEntries = posts.map((post) => ({
      url: `${SITE_URL}/posts/${post.slug}`,
      lastModified: new Date(),
      changeFrequency: "weekly" as const,
      priority: 0.8,
    }));
  } catch {
    // 백엔드 미실행 시 게시글 항목 없이 기본 항목만 반환
  }

  return [
    {
      url: SITE_URL,
      lastModified: new Date(),
      changeFrequency: "daily",
      priority: 1.0,
    },
    ...postEntries,
  ];
}
