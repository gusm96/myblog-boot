import { MetadataRoute } from "next";
import { getAllSlugs, getPublicTags } from "@/lib/api";

// sitemap 자체를 1시간 단위로 재생성. 신규 글 즉시 반영은 on-demand (revalidatePath("/sitemap.xml")) 가 담당.
export const revalidate = 3600;

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [posts, tags] = await Promise.all([
    getAllSlugs().catch(() => []),
    getPublicTags().catch(() => []),
  ]);

  const postEntries: MetadataRoute.Sitemap = posts.map((post) => ({
    url: `${SITE_URL}/posts/${post.slug}`,
    lastModified: new Date(post.updateDate),
    changeFrequency: "weekly" as const,
    priority: 0.8,
  }));

  const tagEntries: MetadataRoute.Sitemap = tags.map((t) => ({
    url: `${SITE_URL}/tag/${encodeURIComponent(t.slug)}`,
    lastModified: new Date(),
    changeFrequency: "weekly" as const,
    priority: 0.6,
  }));

  return [
    {
      url: SITE_URL,
      lastModified: new Date(),
      changeFrequency: "daily",
      priority: 1.0,
    },
    ...postEntries,
    ...tagEntries,
  ];
}
