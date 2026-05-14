import { revalidateTag, revalidatePath } from "next/cache";
import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function POST(req: Request) {
  const secret = req.headers.get("x-revalidate-secret");

  if (!process.env.REVALIDATE_SECRET) {
    return NextResponse.json(
      { ok: false, error: "server misconfiguration" },
      { status: 500 }
    );
  }

  if (secret !== process.env.REVALIDATE_SECRET) {
    return NextResponse.json(
      { ok: false, error: "invalid secret" },
      { status: 401 }
    );
  }

  let body: { tags?: string[]; paths?: string[] };
  try {
    body = await req.json();
  } catch {
    return NextResponse.json(
      { ok: false, error: "invalid json body" },
      { status: 400 }
    );
  }

  const { tags = [], paths = [] } = body;

  if (tags.length === 0 && paths.length === 0) {
    return NextResponse.json(
      { ok: false, error: "tags or paths required" },
      { status: 400 }
    );
  }

  tags.forEach((tag) => revalidateTag(tag, "max"));
  paths.forEach((path) => revalidatePath(path));

  console.info("[revalidate] tags=%o paths=%o", tags, paths);

  return NextResponse.json({
    ok: true,
    tags,
    paths,
    revalidatedAt: Date.now(),
  });
}
