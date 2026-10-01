import type { DealSummary } from "@/lib/api-types";
import { DealCard } from "./DealCard";

interface Props {
  deals: DealSummary[];
  showThumbnail?: boolean;
  listHref?: string;
  onOpenDetail?: (dealId: number) => void;
}

export function DealList({ deals, showThumbnail = true, listHref, onOpenDetail }: Props) {
  return (
    <ul className="divide-y divide-border rounded-xl border border-border bg-surface/20">
      {deals.map((deal) => (
        <li key={deal.id}>
          <DealCard deal={deal} showThumbnail={showThumbnail} listHref={listHref} onOpenDetail={onOpenDetail} />
        </li>
      ))}
    </ul>
  );
}
