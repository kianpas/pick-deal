import type { DealSummary } from "@/lib/api-types";
import { isDealRead, type ReadDeals } from "@/lib/read-deals";
import { DealCard } from "./DealCard";

interface Props {
  deals: DealSummary[];
  showThumbnail?: boolean;
  listHref?: string;
  onOpenDetail?: (dealId: number) => void;
  readDeals?: ReadDeals;
}

export function DealList({ deals, showThumbnail = true, listHref, onOpenDetail, readDeals }: Props) {
  return (
    <ul className="divide-y divide-border rounded-xl border border-border bg-surface/20">
      {deals.map((deal) => (
        <li key={deal.id}>
          <DealCard deal={deal} showThumbnail={showThumbnail} listHref={listHref} onOpenDetail={onOpenDetail}
            read={readDeals ? isDealRead(readDeals, deal) : false} />
        </li>
      ))}
    </ul>
  );
}
