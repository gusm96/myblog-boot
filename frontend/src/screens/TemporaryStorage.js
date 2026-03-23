import React from "react";
import { getDeletedBoards } from "../services/boardApi";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router";
import { PageButton } from "../components/Boards/PageButton";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { queryKeys } from "../services/queryKeys";

export const TemporaryStorage = () => {
  const [page] = useSearchParams("p");

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: queryKeys.admin.trash(page.toString()),
    queryFn:  () => getDeletedBoards(page),
    staleTime: 3  * 60 * 1000,
    gcTime:    10 * 60 * 1000,
    placeholderData: keepPreviousData,
  });

  if (isPending) return <div>Loading...</div>;

  return (
    <div>
      <h3>휴지통</h3>
      <p style={{ fontSize: "0.75em", color: "#999" }}>
        삭제일로 부터 15일 이후 자동 영구 삭제됩니다.
      </p>
      <hr />
      <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
        <BoardList boards={data?.list ?? []} path="/management/boards" />
        <PageButton
          pageCount={data?.totalPage ?? 0}
          path="/management/temporary-storage?"
        />
      </div>
    </div>
  );
};
