import type { DealSummary } from "@/lib/api-types";
import { DealCard } from "./DealCard";

interface Props {
  deals: DealSummary[];
  showThumbnail?: boolean;
}

export function DealList({ deals, showThumbnail = true }: Props) {
  return (
    <ul className={showThumbnail ? "space-y-3" : "space-y-1"}>
      {deals.map((deal) => (
        <li key={deal.id}>
          <DealCard deal={deal} showThumbnail={showThumbnail} />
        </li>
      ))}
    </ul>
  );
}
