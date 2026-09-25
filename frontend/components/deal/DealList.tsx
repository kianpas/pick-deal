import type { DealSummary } from "@/lib/api-types";
import { DealCard } from "./DealCard";

interface Props {
  deals: DealSummary[];
  showThumbnail?: boolean;
  listHref?: string;
}

export function DealList({ deals, showThumbnail = true, listHref }: Props) {
  return (
    <ul className="divide-y divide-border rounded-xl border border-border bg-surface/20">
      {deals.map((deal) => (
        <li key={deal.id}>
          <DealCard deal={deal} showThumbnail={showThumbnail} listHref={listHref} />
        </li>
      ))}
    </ul>
  );
}
