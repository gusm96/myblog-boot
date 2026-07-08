import type { ApiError } from "@/types";

export function getApiErrorMessage(err: unknown, fallback: string): string {
  const data = (err as { response?: { data?: Partial<ApiError> } })?.response?.data;
  return data?.message ?? fallback;
}
