import type { Deal } from "@/lib/types";
import { DealCard } from "./DealCard";

interface Props {
  deals: Deal[];
}

export function DealList({ deals }: Props) {
  return (
    <ul className="space-y-3">
      {deals.map((deal) => (
        <li key={deal.id}>
          <DealCard deal={deal} />
        </li>
      ))}
    </ul>
  );
}
