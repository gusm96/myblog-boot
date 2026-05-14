import { MetadataRoute } from "next";
import { getAllSlugs, getCategoriesV2 } from "@/lib/api";

// sitemap 자체를 1시간 단위로 재생성. 신규 글 즉시 반영은 on-demand (revalidatePath("/sitemap.xml")) 가 담당.
export const revalidate = 3600;

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [posts, categories] = await Promise.all([
    getAllSlugs().catch(() => []),
    getCategoriesV2().catch(() => []),
  ]);

  const postEntries: MetadataRoute.Sitemap = posts.map((post) => ({
    url: `${SITE_URL}/posts/${post.slug}`,
    lastModified: new Date(post.updateDate),
    changeFrequency: "weekly" as const,
    priority: 0.8,
  }));

  const categoryEntries: MetadataRoute.Sitemap = categories.map((c) => ({
    url: `${SITE_URL}/category/${encodeURIComponent(c.name)}`,
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
    ...categoryEntries,
  ];
}
