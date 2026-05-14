"use client";

import { usePostEventSource } from "@/hooks/usePostEventSource";

export function PostEventListener() {
  usePostEventSource();
  return null;
}
